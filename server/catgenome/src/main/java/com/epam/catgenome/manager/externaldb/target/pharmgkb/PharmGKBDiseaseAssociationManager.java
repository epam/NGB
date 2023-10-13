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
package com.epam.catgenome.manager.externaldb.target.pharmgkb;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBGene;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AbstractAssociationManager;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.util.FileFormat;
import lombok.SneakyThrows;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByPhraseQuery;
import static com.epam.catgenome.util.IndexUtils.getByTermsQuery;

@Service
public class PharmGKBDiseaseAssociationManager extends AbstractAssociationManager<PharmGKBDisease> {

    private static final int COLUMNS = 11;
    private static final String GENE = "Gene";
    private static final String DISEASE = "Disease";
    private final PharmGKBGeneManager pharmGKBGeneManager;

    public PharmGKBDiseaseAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                             final @Value("${targets.top.hits:10000}") int targetsTopHits,
                                             final PharmGKBGeneManager pharmGKBGeneManager) {
        super(Paths.get(indexDirectory, "pharmgkb.disease").toString(), targetsTopHits);
        this.pharmGKBGeneManager = pharmGKBGeneManager;
    }

    public long totalCount(final List<String> ids) throws ParseException, IOException {
        final List<PharmGKBDisease> result = searchByGeneIds(ids);
        return result.stream().map(PharmGKBDisease::getId).distinct().count();
    }

    @Override
    public List<PharmGKBDisease> readEntries(final String path) throws IOException {
        final List<PharmGKBDisease> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                if (cells[2].trim().equals(GENE) && cells[5].trim().equals(DISEASE)) {
                    PharmGKBDisease entry = PharmGKBDisease.builder()
                            .id(cells[3].trim())
                            .name(cells[4].trim())
                            .geneId(cells[0].trim())
                            .build();
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    @Override
    public SortField getDefaultSortField() {
        return new SortField(PharmGKBDiseaseField.DISEASE_NAME.name(), SortField.Type.STRING, false);
    }

    @Override
    public List<PharmGKBDisease> processEntries(final List<PharmGKBDisease> entries)
            throws IOException, ParseException {
        final Set<String> pharmGeneIds = entries.stream()
                .map(PharmGKBDisease::getGeneId)
                .collect(Collectors.toSet());
        final List<PharmGKBGene> geneIds = pharmGKBGeneManager.search(new ArrayList<>(pharmGeneIds));
        final Map<String, PharmGKBGene> geneIdsMap = geneIds.stream()
                .collect(Collectors.toMap(PharmGKBGene::getPharmGKBId, Function.identity()));
        for (PharmGKBDisease entry: entries) {
            PharmGKBGene gene = geneIdsMap.getOrDefault(entry.getGeneId(), null);
            if (gene != null) {
                entry.setGeneId(gene.getGeneId());
            }
        }
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final PharmGKBDisease entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(PharmGKBDiseaseField.GENE_ID.name(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(PharmGKBDiseaseField.GENE_ID.name(), new BytesRef(entry.getGeneId())));

        doc.add(new TextField(PharmGKBDiseaseField.DISEASE_ID.name(), entry.getId(), Field.Store.YES));

        doc.add(new TextField(PharmGKBDiseaseField.DISEASE_NAME.name(), entry.getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(PharmGKBDiseaseField.DISEASE_NAME.name(), new BytesRef(entry.getName())));
        writer.addDocument(doc);
    }

    @Override
    public PharmGKBDisease entryFromDoc(final Document doc) {
        final String id = doc.getField(PharmGKBDiseaseField.DISEASE_ID.name()).stringValue();
        return PharmGKBDisease.builder()
                .geneId(doc.getField(PharmGKBDiseaseField.GENE_ID.name()).stringValue())
                .id(id)
                .name(doc.getField(PharmGKBDiseaseField.DISEASE_NAME.name()).stringValue())
                .url(String.format(PharmGKBDisease.URL_PATTERN, id))
                .build();
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return PharmGKBDiseaseField.valueOf(fieldName).getType();
    }
}
