/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.epam.catgenome.manager.pathway;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.PathwayRegistrationRequest;
import com.epam.catgenome.dao.pathway.PathwayDao;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.pathway.Pathway;
import com.epam.catgenome.entity.pathway.PathwayQueryParams;
import com.epam.catgenome.exception.SbgnFileParsingException;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.SortInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.NotNull;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.util.NgbFileUtils.getBioDataItemName;
import static com.epam.catgenome.util.NgbFileUtils.getFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class PathwayManager {

    private static final String SBGN = "sbgn";

    private final PathwayDao pathwayDao;
    private final BiologicalDataItemManager biologicalDataItemManager;

    @Value("${pathway.index.directory}")
    private String pathwayIndexDirectory;

    @Value("${pathway.top.hits:10000}")
    private int pathwayTopHits;

    @Transactional(propagation = Propagation.REQUIRED)
    public Pathway createPathway(final PathwayRegistrationRequest request) throws IOException {
        final String path = request.getPath();
        final File pathwayFile = getFile(path);
        Assert.isTrue(Utils.getFileExtension(request.getPath()).equals("." + SBGN),
                getMessage(MessagesConstants.ERROR_UNSUPPORTED_FILE_EXTENSION, "pathway", SBGN));
        final Pathway pathway = getPathway(request);
        pathway.setPathwayId(pathwayDao.createPathwayId());
        try {
            writeLucenePathwayIndex(pathway, readPathway(pathwayFile));
        } catch (JAXBException e) {
            throw new SbgnFileParsingException(getMessage(MessagesConstants.ERROR_FILE_PARSING, SBGN), e);
        }
        try {
            biologicalDataItemManager.createBiologicalDataItem(pathway);
            pathway.setBioDataItemId(pathway.getId());
            pathwayDao.savePathway(pathway);
        } finally {
            if (pathway.getBioDataItemId() == null) {
                deleteDocument(pathway.getPathwayId());
            }
        }
        return pathway;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePathway(final long pathwayId) throws IOException {
        final Pathway pathway = getPathway(pathwayId);
        pathwayDao.deletePathway(pathwayId);
        biologicalDataItemManager.deleteBiologicalDataItem(pathway.getBioDataItemId());
        deleteDocument(pathwayId);
    }

    public Pathway loadPathway(final long pathwayId) {
        return pathwayDao.loadPathway(pathwayId);
    }

    public byte[] loadPathwayContent(final Long pathwayId) throws IOException {
        final Pathway pathway = getPathway(pathwayId);
        final String path = pathway.getPath();
        final File pathwayFile = getFile(path);
        return FileUtils.readFileToByteArray(pathwayFile);
    }

    public Page<Pathway> loadPathways(final PathwayQueryParams params) throws IOException, ParseException {
        final Page<Pathway> page = new Page<>();
        final List<Pathway> items = new ArrayList<>();
        int totalCount = 0;
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexReader reader = DirectoryReader.open(index)) {
            final IndexSearcher searcher = new IndexSearcher(reader);
            final Query query = TextUtils.isBlank(params.getTerm()) ? new MatchAllDocsQuery() :
                    buildPathwaySearchQuery(params.getTerm());
            TopDocs topDocs = searcher.search(query, pathwayTopHits);
            totalCount = topDocs.totalHits;

            if (totalCount > 0) {
                final Sort sort = getSortBySortInfo(params.getSortInfo());

                final int pageNum = params.getPagingInfo() == null ? 1 :
                        params.getPagingInfo().getPageNum() > 0 ? params.getPagingInfo().getPageNum() : 1;
                final int pageSize = params.getPagingInfo() == null ? totalCount :
                        params.getPagingInfo().getPageSize() > 0 ? params.getPagingInfo().getPageSize() : totalCount;
                final int numDocs = params.getPagingInfo() == null ? totalCount : pageNum * pageSize;

                topDocs = searcher.search(query, numDocs, sort);

                final int from = (pageNum - 1) * pageSize;
                final int to = Math.min(from + pageSize, totalCount);

                for (int i = from; i < to; i++) {
                    int docId = topDocs.scoreDocs[i].doc;
                    Document doc = searcher.doc(docId);
                    Pathway pathway = Pathway.builder().pathwayId(getId(doc)).build();
                    pathway.setName(getDocValue(doc, PathwayIndexFields.NAME));
                    pathway.setPrettyName(getDocValue(doc, PathwayIndexFields.PRETTY_NAME));
                    pathway.setPathwayDesc(getDocValue(doc, PathwayIndexFields.DESCRIPTION));
                    items.add(pathway);
                }
            }
        } catch (IndexNotFoundException e) {
            log.debug(getMessage(MessagesConstants.ERROR_INDEX_DIRECTORY_IS_EMPTY), e);
        }
        page.setTotalCount(totalCount);
        page.setItems(items);
        return page;
    }

    public List<Pathway> loadPathways() {
        return pathwayDao.loadAllPathways(null);
    }

    private Sort getSortBySortInfo(final SortInfo sortInfo) {
        return sortInfo == null ? new Sort(new SortField("name", SortField.Type.STRING, false)) :
                new Sort(new SortField(sortInfo.getField(), SortField.Type.STRING, !sortInfo.isAscending()));
    }

    private void writeLucenePathwayIndex(final Pathway pathway, final String content) throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            addDoc(writer, pathway, content);
        }
    }

    private void deleteDocument(final long pathwayId) throws IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            final Term term = new Term(PathwayIndexFields.PATHWAY_ID.getFieldName(), String.valueOf(pathwayId));
            writer.deleteDocuments(term);
        }
    }

    private String readPathway(final File pathwayFile) throws JAXBException {
        final Sbgn sbgn = SbgnUtil.readFromFile(pathwayFile);
        final Map map = sbgn.getMap().get(0);
        final List<String> entries = new ArrayList<>();
        for (Glyph glyph : map.getGlyph()) {
            if (glyph.getLabel() != null) {
                entries.add(glyph.getLabel().getText());
            }
            glyph.getGlyph().forEach(g -> {
                if (g.getLabel() != null) {
                    entries.add(g.getLabel().getText());
                }
            });
        }
        return StringUtils.join(entries, ",");
    }

    private static void addDoc(final IndexWriter writer, final Pathway pathway, String content) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(PathwayIndexFields.PATHWAY_ID.getFieldName(),
                String.valueOf(pathway.getPathwayId()), Field.Store.YES));

        doc.add(new SortedDocValuesField(PathwayIndexFields.NAME.getFieldName(), new BytesRef(pathway.getName())));
        doc.add(new StoredField(PathwayIndexFields.NAME.getFieldName(), pathway.getName()));
        doc.add(new TextField(PathwayIndexFields.NAME.getFieldName(),
                pathway.getName(), Field.Store.YES));

        if (!TextUtils.isBlank(pathway.getPrettyName())) {
            doc.add(new TextField(PathwayIndexFields.PRETTY_NAME.getFieldName(),
                    pathway.getPrettyName(), Field.Store.YES));
        }

        if (!TextUtils.isBlank(pathway.getPathwayDesc())) {
            doc.add(new TextField(PathwayIndexFields.DESCRIPTION.getFieldName(),
                    pathway.getPathwayDesc(), Field.Store.YES));
        }

        doc.add(new TextField(PathwayIndexFields.CONTENT.getFieldName(), content, Field.Store.YES));
        writer.addDocument(doc);
    }

    @Getter
    private enum PathwayIndexFields {
        PATHWAY_ID("pathwayId"),
        NAME("name"),
        PRETTY_NAME("prettyName"),
        DESCRIPTION("description"),
        CONTENT("content");

        private final String fieldName;

        PathwayIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private String getDocValue(final Document doc, final PathwayIndexFields field) {
        return doc.getField(field.getFieldName()) == null ? null : doc.getField(field.getFieldName()).stringValue();
    }

    private long getId(final Document doc) {
        return Long.parseLong(doc.getField(PathwayIndexFields.PATHWAY_ID.getFieldName()).stringValue());
    }

    private Query buildPathwaySearchQuery(final String term) throws ParseException {
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(buildQuery(PathwayIndexFields.NAME.getFieldName(), term, analyzer),
                BooleanClause.Occur.SHOULD);
        builder.add(buildQuery(PathwayIndexFields.PRETTY_NAME.getFieldName(), term, analyzer),
                BooleanClause.Occur.SHOULD);
        builder.add(buildQuery(PathwayIndexFields.DESCRIPTION.getFieldName(), term, analyzer),
                BooleanClause.Occur.SHOULD);
        builder.add(buildQuery(PathwayIndexFields.CONTENT.getFieldName(), term, analyzer),
                BooleanClause.Occur.SHOULD);
        return builder.build();
    }

    private Query buildQuery(final String fieldName, final String fieldValue, final StandardAnalyzer analyzer)
            throws ParseException {
        return new QueryParser(fieldName, analyzer).parse(fieldValue);
    }

    @NotNull
    private Pathway getPathway(final PathwayRegistrationRequest request) {
        final Pathway pathway = Pathway.builder()
                .pathwayDesc(request.getPathwayDesc())
                .build();
        pathway.setPath(request.getPath());
        pathway.setName(getBioDataItemName(request.getName(), request.getPath()));
        pathway.setPrettyName(request.getPrettyName());
        pathway.setType(BiologicalDataItemResourceType.FILE);
        pathway.setFormat(BiologicalDataItemFormat.PATHWAY);
        pathway.setCreatedDate(new Date());
        pathway.setSource(request.getPath());
        return pathway;
    }

    private Pathway getPathway(final long pathwayId) {
        final Pathway pathway = pathwayDao.loadPathway(pathwayId);
        Assert.notNull(pathway, getMessage(MessagesConstants.ERROR_PATHWAY_NOT_FOUND, pathwayId));
        return pathway;
    }
}
