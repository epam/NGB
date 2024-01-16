/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.ncbi;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.util.FileFormat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.SortField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@Slf4j
public class NCBIEnsemblIdsManager extends AbstractIndexManager<GeneId> {

    private static final int COLUMNS = 7;

    public NCBIEnsemblIdsManager(final @Value("${ncbi.index.directory}") String indexDirectory,
                                 final @Value("${ncbi.gene.ids.hits:10000}") int topHits) {
        super(Paths.get(indexDirectory, "gene.ids").toString(), topHits);
    }

    public List<GeneId> searchByEntrezIds(final List<String> ids) throws ParseException, IOException {
        return search(ids, IndexFields.ENTREZ_ID.name());
    }

    public List<GeneId> searchByEnsemblIds(final List<String> ids) throws ParseException, IOException {
        final List<String> filtered = ListUtils.emptyIfNull(ids).stream()
                .filter(StringUtils::isAlphanumeric)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filtered)) {
            return Collections.emptyList();
        }
        return search(filtered, IndexFields.ENSEMBL_ID.name());
    }

    public List<GeneId> readEntries(final String path) throws IOException {
        final Map<String, GeneId> entries = new HashMap<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                String ensemblId = cells[2].trim();
                GeneId geneId = GeneId.builder()
                        .entrezId(Long.parseLong(cells[1].trim()))
                        .ensemblId(ensemblId)
                        .build();
                if (entries.containsKey(ensemblId) && !entries.get(ensemblId).equals(geneId)) {
                    log.debug(getMessage(MessagesConstants.ERROR_NCBI_DUPLICATE_ENSEMBL_ID, ensemblId));
                }
                entries.put(ensemblId, geneId);
            }
        }
        return new ArrayList<>(entries.values());
    }

    @Override
    public SortField getDefaultSortField() {
        return null;
    }

    @Override
    public List<GeneId> processEntries(List<GeneId> entries) throws IOException, ParseException {
        return entries;
    }

    public void addDoc(final IndexWriter writer, final GeneId entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(IndexFields.ENTREZ_ID.name(), entry.getEntrezId().toString(), Field.Store.YES));
        doc.add(new StringField(IndexFields.ENSEMBL_ID.name(), entry.getEnsemblId().toLowerCase(), Field.Store.YES));
        writer.addDocument(doc);
    }

    public GeneId entryFromDoc(final Document doc) {
        return GeneId.builder()
                .entrezId(Long.parseLong(doc.getField(IndexFields.ENTREZ_ID.name()).stringValue()))
                .ensemblId(doc.getField(IndexFields.ENSEMBL_ID.name()).stringValue())
                .build();
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return null;
    }

    @Getter
    private enum IndexFields {
        ENTREZ_ID,
        ENSEMBL_ID;
    }
}
