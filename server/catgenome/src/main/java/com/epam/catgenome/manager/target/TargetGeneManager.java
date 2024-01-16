/*
 * MIT License
 *
 * Copyright (c) 2024 EPAM Systems
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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.TargetGenePriority;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.manager.index.CaseInsensitiveWhitespaceAnalyzer;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.manager.index.SearchRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByTermQuery;
import static com.epam.catgenome.util.IndexUtils.getByTermsQuery;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;

@Service
public class TargetGeneManager extends AbstractIndexManager<TargetGene> {

    private static final List<String> GENE_IDS = Arrays.asList("gene id", "id", "gene", "gene_id");
    private static final List<String> GENE_NAMES = Arrays.asList("gene name", "name", "gene_name");
    private static final List<String> TAX_IDS = Arrays.asList("tax id", "tax_id", "organism_id");
    private static final List<String> SPECIES_NAMES = Arrays.asList("species", "species name",
            "species_name", "organism");

    public TargetGeneManager(final @Value("${targets.index.directory}") String indexDirectory,
                             final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "genes").toString(), targetsTopHits);
    }

    public void importData(final String path, final long targetId) throws IOException, ParseException {
        final List<TargetGene> entries = readEntries(path, targetId);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGene entry: entries) {
                addDoc(writer, entry);
            }
        }
    }

    public Set<String> getFields(final Long targetId) throws ParseException, IOException {
        final Set<String> defaultColumns = new LinkedHashSet<>(IndexField.VALUES_MAP.keySet());
        Set<String> fields = new HashSet<>();
        Query query = getByTermQuery(targetId.toString(), IndexField.TARGET_ID.getValue());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, topHits);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                final List<IndexableField> docFields = doc.getFields();
                for (IndexableField field : docFields) {
                    if (field.fieldType().stored()) {
                        fields.add(field.name());
                    }
                }
            }
        }
        final List<String> result = new ArrayList<>(fields).stream().sorted().collect(Collectors.toList());
        defaultColumns.addAll(result);
        return defaultColumns;
    }

    public List<String> getFieldValues(final Long targetId, final String field) throws ParseException, IOException {
        final Set<String> values = new HashSet<>();
        final Query query = getByTermQuery(targetId.toString(), IndexField.TARGET_ID.getValue());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, topHits);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                IndexableField indexableField = doc.getField(field);
                if (indexableField != null) {
                    values.add(indexableField.stringValue());
                }
            }
        }
        final List<String> result = new ArrayList<>(values);
        return result.stream().sorted().collect(Collectors.toList());
    }

    public void delete(final Long targetId, final List<String> geneIds) throws ParseException, IOException {
        Query query;
        if (CollectionUtils.isEmpty(geneIds)) {
            query = getByTermQuery(targetId.toString(), IndexField.TARGET_ID.getValue());
        } else {
            final List<String> terms = geneIds.stream()
                    .map(g -> getPrimaryKey(targetId, g))
                    .collect(Collectors.toList());
            query = getByTermsQuery(terms, IndexField.TARGET_GENE_ID.getValue());
        }
        delete(query);
    }

    public void create(final long targetId, final List<TargetGene> targetGenes) throws IOException {
        targetGenes.forEach(g -> g.setTargetId(targetId));
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGene g : targetGenes) {
                addDoc(writer, g);
            }
        }
    }

    public void update(final List<TargetGene> targetGenes) throws IOException, ParseException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGene g : targetGenes) {
                Term term = new Term(IndexField.TARGET_GENE_ID.getValue(),
                        getPrimaryKey(g.getTargetId(), g.getGeneId()));
                writer.updateDocument(term, docFromEntry(g));
            }
        }
    }

    public SearchResult<TargetGene> filter(final long targetId, final SearchRequest request)
            throws ParseException, IOException {
        final Query query = buildQuery(targetId, request.getFilters());
        return search(request, query);
    }

    public List<TargetGene> search(final String geneName, final Long taxId) throws ParseException, IOException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        final Query geneNameQuery = getByTermQuery(geneName, IndexField.GENE_NAME.getValue());
        mainBuilder.add(geneNameQuery, BooleanClause.Occur.MUST);
        if (taxId != null) {
            final Query taxIdQuery = getByTermQuery(String.valueOf(taxId), IndexField.TAX_ID.getValue());
            mainBuilder.add(taxIdQuery, BooleanClause.Occur.MUST);
        }
        return search(mainBuilder.build(), null);
    }

    public Query buildQuery(final long targetId, final List<Filter> filters) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        final Query query = getByTermQuery(String.valueOf(targetId), IndexField.TARGET_ID.getValue());
        mainBuilder.add(query, BooleanClause.Occur.MUST);
        if (filters != null) {
            for (Filter filter: filters) {
                addFieldQuery(mainBuilder, filter);
            }
        }
        return mainBuilder.build();
    }

    @Override
    public List<TargetGene> readEntries(String path) throws IOException {
        return null;
    }

    @Override
    public SortField getDefaultSortField() {
        return new SortField(IndexField.GENE_ID.name(), SortField.Type.STRING, false);
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return IndexField.getByValue(fieldName).getType();
    }

    @Override
    public List<TargetGene> processEntries(List<TargetGene> entries) {
        return entries;
    }

    @Override
    public void addDoc(final IndexWriter writer, final TargetGene entry) throws IOException {
        final Document doc = docFromEntry(entry);
        writer.addDocument(doc);
    }

    private static Document docFromEntry(TargetGene entry) {
        final Document doc = new Document();
        doc.add(new StringField(IndexField.TARGET_GENE_ID.getValue(),
                getPrimaryKey(entry.getTargetId(), entry.getGeneId()), Field.Store.NO));

        doc.add(new StringField(IndexField.TARGET_ID.getValue(), entry.getTargetId().toString(), Field.Store.YES));

        doc.add(new TextField(IndexField.GENE_ID.getValue(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.GENE_ID.getValue(), new BytesRef(entry.getGeneId())));

        doc.add(new TextField(IndexField.GENE_NAME.getValue(), entry.getGeneName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.GENE_NAME.getValue(), new BytesRef(entry.getGeneName())));

        doc.add(new NumericDocValuesField(IndexField.TAX_ID.getValue(), entry.getTaxId()));
        doc.add(new StoredField(IndexField.TAX_ID.getValue(), entry.getTaxId()));

        doc.add(new TextField(IndexField.SPECIES_NAME.getValue(), entry.getSpeciesName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.SPECIES_NAME.getValue(), new BytesRef(entry.getSpeciesName())));

        if (entry.getPriority() != null) {
            doc.add(new NumericDocValuesField(IndexField.PRIORITY.getValue(), entry.getPriority().getValue()));
            doc.add(new StoredField(IndexField.PRIORITY.getValue(), entry.getPriority().getValue()));
        }

        entry.getMetadata().forEach((k, v) -> {
            doc.add(new TextField(k, v, Field.Store.YES));
            doc.add(new SortedDocValuesField(k, new BytesRef(v)));
        });
        return doc;
    }

    @Override
    public TargetGene entryFromDoc(final Document doc) {
        final List<IndexableField> fields = doc.getFields();
        final Map<String, String> metadata = new HashMap<>();
        final TargetGene targetGene = TargetGene.builder()
                .metadata(metadata)
                .build();
        for (IndexableField field : fields) {
            IndexField indexField = IndexField.getByValue(field.name());
            switch (indexField) {
                case TARGET_ID:
                    targetGene.setTargetId(Long.parseLong(field.stringValue()));
                    break;
                case GENE_ID:
                    targetGene.setGeneId(field.stringValue());
                    break;
                case GENE_NAME:
                    targetGene.setGeneName(field.stringValue());
                    break;
                case TAX_ID:
                    targetGene.setTaxId((Long) field.numericValue());
                    break;
                case SPECIES_NAME:
                    targetGene.setSpeciesName(field.stringValue());
                    break;
                case PRIORITY:
                    targetGene.setPriority(TargetGenePriority.getByValue((int) field.numericValue()));
                    break;
                case METADATA:
                    metadata.put(field.name(), field.stringValue());
                    break;
                default:
                    break;
            }
        }
        return targetGene;
    }

    private static String getPrimaryKey(final long targetId, final String geneId) {
        return targetId + geneId.toLowerCase();
    }

    @AllArgsConstructor
    @Getter
    private enum IndexField {
        TARGET_GENE_ID("ID", FilterType.NONE),
        TARGET_ID("Target ID", FilterType.TERM),
        GENE_ID("Gene ID", FilterType.TERM),
        GENE_NAME("Gene Name", FilterType.PHRASE),
        TAX_ID("Tax ID", FilterType.TERM),
        SPECIES_NAME("Species Name", FilterType.PHRASE),
        PRIORITY("Priority", FilterType.OPTIONS),
        METADATA("Metadata", FilterType.PHRASE);

        private final String value;
        private final FilterType type;
        private static final Map<String, IndexField> VALUES_MAP = new LinkedHashMap<>();
        static {
            VALUES_MAP.put("Target ID", TARGET_ID);
            VALUES_MAP.put("Gene ID", GENE_ID);
            VALUES_MAP.put("Gene Name", GENE_NAME);
            VALUES_MAP.put("Tax ID", TAX_ID);
            VALUES_MAP.put("Species Name", SPECIES_NAME);
            VALUES_MAP.put("Priority", PRIORITY);
        }

        public static IndexField getByValue(String value) {
            return VALUES_MAP.getOrDefault(value, METADATA);
        }

    }
    private List<TargetGene> readEntries(final String path, final long targetId) throws IOException {
        final List<TargetGene> entries = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(path)) {
            final Workbook workbook = new XSSFWorkbook(file);
            final Sheet sheet = workbook.getSheetAt(0);
            final int totalRows = sheet.getPhysicalNumberOfRows();
            final Header header = getHeader(sheet.getRow(0));
            for (int i = 1; i < totalRows; i++) {
                Row row = sheet.getRow(i);

                Cell geneIdCell = row.getCell(header.getIdIndex());
                Assert.notNull(geneIdCell, "Gene ID column not found");
                String geneId = getCellValue(geneIdCell);
                Assert.isTrue(!TextUtils.isBlank(geneId), "Gene ID should not be blank");

                Cell geneNameCell = row.getCell(header.getNameIndex());
                Assert.notNull(geneNameCell, "Gene Name column not found");
                String geneName = getCellValue(geneNameCell);
                Assert.isTrue(!TextUtils.isBlank(geneName), "Gene name should not be blank");

                Cell taxIdCell = row.getCell(header.getTaxIdIndex());
                Assert.notNull(taxIdCell, "Tax ID column not found");
                Assert.isTrue(taxIdCell.getCellTypeEnum() == NUMERIC, "Tax ID should be numeric");

                Cell speciesNameCell = row.getCell(header.getSpeciesNameIndex());
                Assert.notNull(speciesNameCell, "Species name column not found");
                String speciesName = getCellValue(speciesNameCell);
                Assert.isTrue(!TextUtils.isBlank(speciesName), "Species name should not be blank");

                Map<String, String> metadata = new HashMap<>();
                header.getMetadataIndexes().forEach((k, v) -> {
                    Cell cell = row.getCell(v);
                    if (cell != null) {
                        String cellValue = getCellValue(cell);
                        if (StringUtils.isNotBlank(cellValue)) {
                            metadata.put(k, cellValue);
                        }
                    }
                });

                TargetGene gene = TargetGene.builder()
                        .targetId(targetId)
                        .geneId(geneId)
                        .geneName(geneName)
                        .taxId((long) taxIdCell.getNumericCellValue())
                        .speciesName(speciesName)
                        .metadata(metadata)
                        .build();
                entries.add(gene);
            }
        }
        return entries;
    }

    private static String getCellValue(final Cell cell) {
        String cellValue = "";
        switch (cell.getCellTypeEnum()) {
            case STRING:
                cellValue = cell.getStringCellValue();
                break;
            case NUMERIC:
                cellValue = String.valueOf(cell.getNumericCellValue());
                break;
            case BOOLEAN:
                cellValue = String.valueOf(cell.getBooleanCellValue());
                break;
            default:
                break;
        }
        return cellValue;
    }

    private Header getHeader(final Row headerRow) {
        int index = 0;
        Integer idIndex = null;
        Integer nameIndex = null;
        Integer taxIdIndex = null;
        Integer speciesNameIndex = null;
        final Map<String, Integer> metadataIndexes = new HashMap<>();
        for (Cell cell : headerRow) {
            String cellValue = cell.getStringCellValue();
            if (TextUtils.isBlank(cellValue)) {
                break;
            }
            if (GENE_IDS.stream().anyMatch(cellValue::equalsIgnoreCase)) {
                idIndex = index;
            } else if (GENE_NAMES.stream().anyMatch(cellValue::equalsIgnoreCase)) {
                nameIndex = index;
            } else if (TAX_IDS.stream().anyMatch(cellValue::equalsIgnoreCase)) {
                taxIdIndex = index;
            } else if (SPECIES_NAMES.stream().anyMatch(cellValue::equalsIgnoreCase)) {
                speciesNameIndex = index;
            } else {
                metadataIndexes.put(cellValue, index);
            }
            index++;
        }
        Assert.notNull(idIndex, "Gene ID column not found");
        Assert.notNull(nameIndex, "Gene Name column not found");
        Assert.notNull(taxIdIndex, "Tax ID column not found");
        Assert.notNull(speciesNameIndex, "Species name column not found");
        return Header.builder()
                .idIndex(idIndex)
                .nameIndex(nameIndex)
                .taxIdIndex(taxIdIndex)
                .speciesNameIndex(speciesNameIndex)
                .metadataIndexes(metadataIndexes)
                .build();
    }

    @Getter
    @Setter
    @Builder
    private static class Header {
        private Integer idIndex;
        private Integer nameIndex;
        private Integer taxIdIndex;
        private Integer speciesNameIndex;
        private Map<String, Integer> metadataIndexes;
    }
}
