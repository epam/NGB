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
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
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
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.testng.internal.collections.Pair;

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
public class PharmGKBDrugAssociationManager extends AbstractAssociationManager<PharmGKBDrug> {

    private final PharmGKBGeneManager pharmGKBGeneManager;
    private final PharmGKBDrugManager pharmGKBDrugManager;

    public PharmGKBDrugAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                          final @Value("${targets.top.hits:10000}") int targetsTopHits,
                                          final PharmGKBGeneManager pharmGKBGeneManager,
                                          final PharmGKBDrugManager pharmGKBDrugManager) {
        super(Paths.get(indexDirectory, "pharmgkb.drug.association").toString(), targetsTopHits);
        this.pharmGKBGeneManager = pharmGKBGeneManager;
        this.pharmGKBDrugManager = pharmGKBDrugManager;
    }

    public Pair<Long, Long> totalCount(final List<String> ids) throws ParseException, IOException {
        final List<PharmGKBDrug> result = searchByGeneIds(ids);
        return Pair.of(Long.valueOf(result.size()), result.stream().map(PharmGKBDrug::getId).distinct().count());
    }

    public PharmGKBDrugFieldValues getFieldValues(final List<String> geneIds)
            throws IOException, ParseException {
        final Query query = getByGeneIdsQuery(geneIds);
        final List<PharmGKBDrug> result = search(query, null);
        final List<String> sources = result.stream()
                .map(PharmGKBDrug::getSource)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return PharmGKBDrugFieldValues.builder()
                .sources(sources)
                .build();
    }

    @Override
    public List<PharmGKBDrug> readEntries(final String path) throws IOException {
        final List<PharmGKBDrug> entries = new ArrayList<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            String pharmGKBId;
            String[] drugIds;
            Assert.isTrue(cells.length == 4, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                pharmGKBId = cells[0].trim();
                drugIds = cells[2].trim().split(";");
                for (String drugId : drugIds) {
                    PharmGKBDrug entry = PharmGKBDrug.builder()
                            .pharmGKBGeneId(pharmGKBId)
                            .id(drugId)
                            .build();
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    @Override
    public String getDefaultSortField() {
        return PharmGKBDrugField.DRUG_NAME.name();
    }

    @Override
    public List<PharmGKBDrug> processEntries(final List<PharmGKBDrug> entries) throws IOException, ParseException {
        final Set<String> pharmGeneIds = entries.stream()
                .map(PharmGKBDrug::getPharmGKBGeneId)
                .collect(Collectors.toSet());
        final List<PharmGKBGene> pharmGKBGenes = pharmGKBGeneManager.search(new ArrayList<>(pharmGeneIds));
        final Map<String, String> genesMap = pharmGKBGenes.stream()
                .collect(Collectors.toMap(PharmGKBGene::getPharmGKBId, PharmGKBGene::getGeneId));

        final Set<String> drugIds = entries.stream()
                .map(PharmGKBDrug::getId)
                .collect(Collectors.toSet());
        final List<PharmGKBDrug> drugs = pharmGKBDrugManager.search(new ArrayList<>(drugIds));
        final Map<String, PharmGKBDrug> drugsMap = drugs.stream()
                .collect(Collectors.toMap(PharmGKBDrug::getId, Function.identity()));
        for (PharmGKBDrug entry: entries) {
            if (drugsMap.containsKey(entry.getId())) {
                PharmGKBDrug drug = drugsMap.get(entry.getId());
                entry.setName(drug.getName());
                entry.setGeneId(genesMap.get(entry.getPharmGKBGeneId()));
                entry.setSource(drug.getSource());
            }
        }
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final PharmGKBDrug entry) throws IOException {
        final String geneId = entry.getGeneId();
        if (geneId != null) {
            final Document doc = new Document();
            doc.add(new TextField(PharmGKBDrugField.GENE_ID.name(), geneId, Field.Store.YES));
            doc.add(new SortedDocValuesField(PharmGKBDrugField.GENE_ID.name(), new BytesRef(geneId)));

            doc.add(new TextField(PharmGKBDrugField.DRUG_ID.name(), entry.getId(), Field.Store.YES));

            doc.add(new TextField(PharmGKBDrugField.DRUG_NAME.name(), entry.getName(), Field.Store.YES));
            doc.add(new SortedDocValuesField(PharmGKBDrugField.DRUG_NAME.name(), new BytesRef(entry.getName())));

            doc.add(new TextField(PharmGKBDrugField.SOURCE.name(), entry.getSource(), Field.Store.YES));
            doc.add(new SortedDocValuesField(PharmGKBDrugField.SOURCE.name(), new BytesRef(entry.getSource())));
            writer.addDocument(doc);
        }
    }

    @Override
    public PharmGKBDrug entryFromDoc(final Document doc) {
        final String id = doc.getField(PharmGKBDrugField.DRUG_ID.name()).stringValue();
        return PharmGKBDrug.builder()
                .id(id)
                .name(doc.getField(PharmGKBDrugField.DRUG_NAME.name()).stringValue())
                .url(String.format(PharmGKBDrug.URL_PATTERN, id))
                .geneId(doc.getField(PharmGKBDrugField.GENE_ID.name()).stringValue())
                .source(doc.getField(PharmGKBDrugField.SOURCE.name()).stringValue())
                .build();
    }

    @SneakyThrows
    @Override
    public void addFieldQuery(BooleanQuery.Builder builder, Filter filter) {
        final Query query = PharmGKBDrugField.valueOf(filter.getField()).getType().equals(FilterType.PHRASE) ?
                getByPhraseQuery(filter.getTerms().get(0), filter.getField()) :
                getByTermsQuery(filter.getTerms(), filter.getField());
        builder.add(query, BooleanClause.Occur.MUST);
    }

    @Override
    public List<AssociationExportField<PharmGKBDrug>> getExportFields() {
        return Arrays.asList(PharmGKBDrugField.values());
    }
}
