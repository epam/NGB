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
import com.epam.catgenome.entity.pathway.PathwayEntry;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
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

    @Transactional(propagation = Propagation.REQUIRED)
    public Pathway createPathway(final PathwayRegistrationRequest request) throws IOException, ParseException {
        final String path = request.getPath();
        getFile(path);
        final Pathway pathway = getPathway(request);
        biologicalDataItemManager.createBiologicalDataItem(pathway);
        pathway.setBioDataItemId(pathway.getId());
        pathwayDao.savePathway(pathway);
        writeLucenePathwayIndex(path);
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

    public List<Pathway> loadPathways() {
        return pathwayDao.loadAllPathways();
    }

    public void writeLucenePathwayIndex(final String pathwayFilePath) throws IOException, ParseException {
        try (Directory index = new SimpleFSDirectory(Paths.get(pathwayIndexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (PathwayEntry entry: readPathway(pathwayFilePath)) {
                addDoc(writer, entry);
            }
        }
    }

    public List<PathwayEntry> readPathway(final String path) {
        //TODO
        return Collections.emptyList();
    }

    private static void addDoc(final IndexWriter writer, final PathwayEntry entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(PathwayIndexFields.PATHWAY_ID.getFieldName(),
                String.valueOf(entry.getPathwayId()), Field.Store.YES));
        //TODO
        writer.addDocument(doc);
    }

    @Getter
    private enum PathwayIndexFields {
        PATHWAY_ID("pathwayId");
        //TODO

        private final String fieldName;

        PathwayIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
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
