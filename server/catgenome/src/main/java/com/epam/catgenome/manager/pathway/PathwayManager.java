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
import com.epam.catgenome.entity.pathway.SbgnElement;
import com.epam.catgenome.entity.pathway.SbgnElementType;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.util.db.Page;
import com.epam.catgenome.util.db.QueryParameters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jetbrains.annotations.NotNull;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.Arc;
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

    private final PathwayDao pathwayDao;
    private final BiologicalDataItemManager biologicalDataItemManager;

    @Value("${pathway.index.directory}")
    private String pathwayIndexDirectory;

    @Value("${pathway.top.hits:100}")
    private int pathwayTopHits;

    @Transactional(propagation = Propagation.REQUIRED)
    public Pathway createPathway(final PathwayRegistrationRequest request)
            throws IOException, ParseException, JAXBException {
        final String path = request.getPath();
        final File pathwayFile = getFile(path);
        final Pathway pathway = getPathway(request);
        biologicalDataItemManager.createBiologicalDataItem(pathway);
        pathway.setBioDataItemId(pathway.getId());
        pathwayDao.savePathway(pathway);
        writeLucenePathwayIndex(pathway.getPathwayId(), pathwayFile);
        return pathway;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePathway(final long pathwayId) {
        final Pathway pathway = getPathway(pathwayId);
        pathwayDao.deletePathway(pathwayId);
        biologicalDataItemManager.deleteBiologicalDataItem(pathway.getBioDataItemId());
    }

    public Pathway loadPathway(final long pathwayId) {
        return pathwayDao.loadPathway(pathwayId);
    }

    public Page<Pathway> loadPathways(final QueryParameters queryParameters) {
        final Page<Pathway> page = new Page<>();
        final long totalCount = pathwayDao.getTotalCount();
        final List<Pathway> items = pathwayDao.loadAllPathways(queryParameters);
        page.setTotalCount(totalCount);
        page.setItems(items);
        return page;
    }

    public void writeLucenePathwayIndex(final long pathwayId, final File pathwayFile)
            throws IOException, ParseException, JAXBException {
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (SbgnElement entry: readPathway(pathwayId, pathwayFile)) {
                addDoc(writer, entry);
            }
        }
    }

    public List<SbgnElement> readPathway(final long pathwayId, final File pathwayFile) throws JAXBException {
        final Sbgn sbgn = SbgnUtil.readFromFile(pathwayFile);
        final Map map = sbgn.getMap().get(0);
        Assert.isTrue(!map.getGlyph().isEmpty(), getMessage(MessagesConstants.ERROR_PATHWAY_NO_GLYPHS));
        Assert.isTrue(!map.getArc().isEmpty(), getMessage(MessagesConstants.ERROR_PATHWAY_NO_ARCS));
        final List<SbgnElement> entries = new ArrayList<>();
        for (Glyph glyph : map.getGlyph()) {
            SbgnElement entry = SbgnElement.builder()
                    .pathwayId(pathwayId)
                    .type(SbgnElementType.GLYPH)
                    .clazz(glyph.getClazz())
                    .entryId(glyph.getId())
                    .label(glyph.getLabel() != null ? glyph.getLabel().getText() : null)
                    .build();
            entries.add(entry);
        }
        for (Arc arc : map.getArc()) {
            SbgnElement entry = SbgnElement.builder()
                    .pathwayId(pathwayId)
                    .type(SbgnElementType.ARC)
                    .clazz(arc.getClazz())
                    .entryId(arc.getId())
                    .build();
            entries.add(entry);
        }
        return entries;
    }

    public List<SbgnElement> searchElements(final SbgnElement filter) throws IOException, ParseException {
        final List<SbgnElement> elements = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            final Query query = buildPathwaySearchQuery(filter);
            TopDocs topDocs = searcher.search(query, pathwayTopHits);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                elements.add(
                        SbgnElement.builder()
                                .pathwayId(getId(doc))
                                .type(SbgnElementType.getByName(getDocValue(doc, PathwayIndexFields.TYPE)))
                                .entryId(getDocValue(doc, PathwayIndexFields.ENTRY_ID))
                                .clazz(getDocValue(doc, PathwayIndexFields.CLAZZ))
                                .label(getDocValue(doc, PathwayIndexFields.LABEL))
                                .build()
                );
            }
        }
        return elements;
    }

    public byte[] loadPathwayContent(final Long pathwayId) throws IOException {
        final Pathway pathway = getPathway(pathwayId);
        final String path = pathway.getPath();
        final File pathwayFile = getFile(path);
        return FileUtils.readFileToByteArray(pathwayFile);
    }

    private static void addDoc(final IndexWriter writer, final SbgnElement entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(PathwayIndexFields.PATHWAY_ID.getFieldName(),
                String.valueOf(entry.getPathwayId()), Field.Store.YES));
        doc.add(new StringField(PathwayIndexFields.TYPE.getFieldName(), entry.getType().getName(), Field.Store.YES));
        doc.add(new StringField(PathwayIndexFields.CLAZZ.getFieldName(), entry.getClazz(), Field.Store.YES));
        if (entry.getEntryId() != null) {
            doc.add(new TextField(PathwayIndexFields.ENTRY_ID.getFieldName(), entry.getEntryId(), Field.Store.YES));
        }
        if (entry.getLabel() != null) {
            doc.add(new TextField(PathwayIndexFields.LABEL.getFieldName(), entry.getLabel(), Field.Store.YES));
        }
        writer.addDocument(doc);
    }

    @Getter
    private enum PathwayIndexFields {
        PATHWAY_ID("pathwayId"),
        TYPE("type"),
        CLAZZ("clazz"),
        ENTRY_ID("entryId"),
        LABEL("label");

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

    private Query buildPathwaySearchQuery(final SbgnElement entry) throws ParseException {
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (entry.getPathwayId() != null) {
            builder.add(buildQuery(PathwayIndexFields.PATHWAY_ID.getFieldName(),
                    String.valueOf(entry.getPathwayId()), analyzer), BooleanClause.Occur.MUST);
        }
        if (entry.getType() != null) {
            builder.add(buildQuery(PathwayIndexFields.TYPE.getFieldName(),
                    entry.getType().name(), analyzer), BooleanClause.Occur.MUST);
        }
        if (entry.getClazz() != null) {
            builder.add(buildQuery(PathwayIndexFields.CLAZZ.getFieldName(),
                    entry.getClazz(), analyzer), BooleanClause.Occur.MUST);
        }
        if (entry.getEntryId() != null) {
            builder.add(buildQuery(PathwayIndexFields.ENTRY_ID.getFieldName(),
                    entry.getEntryId(), analyzer), BooleanClause.Occur.MUST);
        }
        if (entry.getLabel() != null) {
            builder.add(buildPhraseQuery(PathwayIndexFields.LABEL.getFieldName(),
                    entry.getLabel(), analyzer), BooleanClause.Occur.MUST);
        }
        return builder.build();
    }

    private Query buildQuery(final String fieldName, final String fieldValue, final StandardAnalyzer analyzer)
            throws ParseException {
        return new QueryParser(fieldName, analyzer).parse(fieldValue);
    }

    private Query buildPhraseQuery(final String fieldName, final String fieldValue, final StandardAnalyzer analyzer) {
        final QueryParser parser = new QueryParser(fieldName, analyzer);
        return parser.createPhraseQuery(fieldName, fieldValue);
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
