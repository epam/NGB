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
import com.epam.catgenome.entity.externaldb.ncbi.GeneInfo;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.manager.index.CaseInsensitiveWhitespaceAnalyzer;
import com.epam.catgenome.util.FileFormat;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByOptionsQuery;
import static com.epam.catgenome.util.IndexUtils.getByPrefixQuery;

@Service
public class NCBIGeneInfoManager extends AbstractIndexManager<GeneInfo> {

    private List<String> filterTaxIds;
    private static final int COLUMNS = 16;
    private static final Integer BATCH_SIZE = 100000;

    public NCBIGeneInfoManager(final @Value("${ncbi.index.directory}") String indexDirectory,
                               final @Value("${ncbi.gene.info.top.hits:1000}") int topHits,
                               final @Value("${ncbi.gene.info.tax.ids:}") String taxIds) {
        super(Paths.get(indexDirectory, "gene.info").toString(), topHits);
        if (StringUtils.hasText(taxIds)) {
            this.filterTaxIds = Arrays.asList(taxIds.split(","));
        }
    }

    public List<GeneInfo> searchBySymbol(final String prefix) throws ParseException, IOException {
        Query query = getByPrefixQuery(prefix.toLowerCase(), IndexFields.SYMBOL.name());
        if (CollectionUtils.isNotEmpty(filterTaxIds)) {
            final Query filter = getByOptionsQuery(filterTaxIds, IndexFields.TAX_ID.name());
            final BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(query, BooleanClause.Occur.SHOULD);
            builder.add(filter, BooleanClause.Occur.SHOULD);
        }
        return search(query, Sort.RELEVANCE);
    }

    @Override
    public void importData(final String path) throws IOException, ParseException {
        Set<GeneInfo> entries = new HashSet<>();
        String line;
        try (Reader reader = new FileReader(path);
             BufferedReader bufferedReader = new BufferedReader(reader);
             Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                GeneInfo geneInfo = GeneInfo.builder()
                        .taxId(Long.parseLong(cells[0].trim()))
                        .geneId(Long.parseLong(cells[1].trim()))
                        .symbol(cells[2].trim())
                        .description(cells[8].trim())
                        .build();
                entries.add(geneInfo);
                if (entries.size() >= BATCH_SIZE) {
                    for (GeneInfo entry: entries) {
                        addDoc(writer, entry);
                    }
                    entries = new HashSet<>();
                }
            }
            for (GeneInfo entry: entries) {
                addDoc(writer, entry);
            }
        }
    }

    public List<GeneInfo> readEntries(final String path) throws IOException {
        final Set<GeneInfo> entries = new HashSet<>();
        String line;
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            String[] cells = line.split(FileFormat.TSV.getSeparator());
            Assert.isTrue(cells.length == COLUMNS, MessagesConstants.ERROR_INCORRECT_FILE_FORMAT);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(FileFormat.TSV.getSeparator());
                GeneInfo geneInfo = GeneInfo.builder()
                        .taxId(Long.parseLong(cells[0].trim()))
                        .geneId(Long.parseLong(cells[1].trim()))
                        .symbol(cells[2].trim())
                        .description(cells[8].trim())
                        .build();
                entries.add(geneInfo);
            }
        }
        return new ArrayList<>(entries);
    }

    @Override
    public SortField getDefaultSortField() {
        return null;
    }

    @Override
    public List<GeneInfo> processEntries(List<GeneInfo> entries) throws IOException, ParseException {
        return entries;
    }

    public void addDoc(final IndexWriter writer, final GeneInfo entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(IndexFields.GENE_ID.name(), entry.getGeneId().toString(), Field.Store.YES));
        doc.add(new StringField(IndexFields.TAX_ID.name(), entry.getTaxId().toString(), Field.Store.YES));
        doc.add(new TextField(IndexFields.SYMBOL.name(), entry.getSymbol(), Field.Store.YES));
        doc.add(new StringField(IndexFields.DESCRIPTION.name(), entry.getDescription(), Field.Store.YES));
        writer.addDocument(doc);
    }

    public GeneInfo entryFromDoc(final Document doc) {
        return GeneInfo.builder()
                .geneId(Long.parseLong(doc.getField(IndexFields.GENE_ID.name()).stringValue()))
                .taxId(Long.parseLong(doc.getField(IndexFields.TAX_ID.name()).stringValue()))
                .symbol(doc.getField(IndexFields.SYMBOL.name()).stringValue())
                .description(doc.getField(IndexFields.DESCRIPTION.name()).stringValue())
                .build();
    }

    @Getter
    private enum IndexFields {
        GENE_ID,
        TAX_ID,
        SYMBOL,
        DESCRIPTION;
    }
}
