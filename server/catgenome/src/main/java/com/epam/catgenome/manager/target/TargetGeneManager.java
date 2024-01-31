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
import com.epam.catgenome.entity.index.SortType;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.TargetGeneField;
import com.epam.catgenome.entity.target.TargetGenePriority;
import com.epam.catgenome.exception.TargetGenesException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.epam.catgenome.manager.index.CaseInsensitiveWhitespaceAnalyzer;
import com.epam.catgenome.manager.index.FieldInfo;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.manager.index.OrderInfo;
import com.epam.catgenome.manager.index.SearchRequest;
import com.epam.catgenome.util.FileFormat;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.*;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;

@Service
public class TargetGeneManager extends AbstractIndexManager<TargetGene> {

    private static final List<String> GENE_IDS = Arrays.asList("gene id", "id", "gene", "gene_id");
    private static final List<String> GENE_NAMES = Arrays.asList("gene name", "name", "gene_name");
    private static final List<String> TAX_IDS = Arrays.asList("tax id", "tax_id", "organism_id");
    private static final List<String> SPECIES_NAMES = Arrays.asList("species", "species name",
            "species_name", "organism");
    private static final int FIELD_VALUES_TOP_HITS = 200;
    private static final String EXCEL_EXTENSION = "xlsx";
    private static final float OPTIONS_RATIO = 0.5F;
    private int keywordMaxLength;
    private final TargetGeneFieldManager targetGeneFieldManager;

    public TargetGeneManager(final @Value("${targets.index.directory}") String indexDirectory,
                             final @Value("${targets.top.hits:10000}") int targetsTopHits,
                             final @Value("${targets.keyword.max.length:100}") int keywordMaxLength,
                             final TargetGeneFieldManager targetGeneFieldManager) {
        super(Paths.get(indexDirectory, "genes").toString(), targetsTopHits);
        this.targetGeneFieldManager = targetGeneFieldManager;
        this.keywordMaxLength = keywordMaxLength;
    }

    public void importData(final long targetId, final String path, final MultipartFile file)
            throws IOException, ParseException, TargetGenesException {
        Assert.isTrue(path != null || file != null, "Genes file path or content should be defined");
        final InputStream inputStream = file != null ?  file.getInputStream() :
                Files.newInputStream(Paths.get(path));
        final String extension = FilenameUtils.getExtension(file != null ? file.getOriginalFilename() : path);
        final List<TargetGene> entries = readEntries(inputStream, extension);
        final Map<String, TargetGeneField> targetGeneFields = processMetadata(entries, targetId);
        setIds(targetId, entries);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGene entry: entries) {
                addDoc(writer, entry, targetGeneFields);
            }
        }
    }

    public List<TargetGene> load(final List<Long> targetGeneIds) throws ParseException, IOException {
        return search(targetGeneIds.stream().map(Object::toString).collect(Collectors.toList()),
                IndexField.TARGET_GENE_ID.getValue());
    }

    public List<TargetGeneField> getFields(final Long targetId) throws ParseException, IOException {
        final Set<IndexField> indexFields = new LinkedHashSet<>(IndexField.VALUES_MAP.values());
        indexFields.remove(IndexField.TARGET_GENE_ID);
        indexFields.remove(IndexField.TARGET_ID);
        final List<TargetGeneField> defaultColumns = indexFields.stream()
                .map(d -> TargetGeneField.builder()
                        .field(d.getValue())
                        .filterType(d.getType())
                        .sortType(d.getSortType())
                        .build())
                .collect(Collectors.toList());
        final List<TargetGeneField> targetGeneFields = targetGeneFieldManager.load(targetId);
        defaultColumns.addAll(targetGeneFields);
        return defaultColumns;
    }

    public List<FieldInfo> getFieldInfos(final Long targetId) throws ParseException, IOException {
        final List<TargetGeneField> targetGeneFields = getFields(targetId);
        final List<FieldInfo> fieldInfos = new ArrayList<>();
        targetGeneFields.forEach(f -> {
            FieldInfo fieldInfo = FieldInfo.builder()
                    .fieldName(f.getField())
                    .filterType(f.getFilterType())
                    .sort(f.getSortType() != SortType.NONE)
                    .build();
            fieldInfos.add(fieldInfo);
        });
        return fieldInfos;
    }

    public List<String> getOptions(final Long targetId, final String field) throws ParseException, IOException {
        final Map<String, TargetGeneField> targetGeneFields = getFieldsMap(targetId);
        TargetGeneField targetGeneField = targetGeneFields.get(field);
        if (targetGeneField.getFilterType() != FilterType.OPTIONS) {
            return Collections.emptyList();
        }
        final Set<String> values = new LinkedHashSet<>();
        final Query query = getByTermQuery(targetId.toString(), IndexField.TARGET_ID.getValue());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            final OrderInfo orderInfo = OrderInfo.builder()
                    .orderBy(field)
                    .reverse(false)
                    .build();
            final Sort sort = getSort(Collections.singletonList(orderInfo), targetGeneFields);
            TopDocs topDocs = sort != null ? searcher.search(query, topHits, sort) : searcher.search(query, topHits);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                IndexableField indexableField = doc.getField(field);
                if (indexableField != null) {
                    values.add(indexableField.stringValue());
                }
            }
        }
        final List<String> result = new ArrayList<>(values);
        return result.stream().limit(FIELD_VALUES_TOP_HITS).collect(Collectors.toList());
    }

    public void delete(final Long targetId) throws ParseException, IOException {
        final Query query = getByTermQuery(targetId.toString(), IndexField.TARGET_ID.getValue());
        delete(query);
        targetGeneFieldManager.delete(targetId);
    }

    public void delete(final List<Long> targetGeneIds) throws ParseException, IOException {
        final Query query = getByTermsQuery(targetGeneIds.stream().map(Object::toString).collect(Collectors.toList()),
                IndexField.TARGET_GENE_ID.getValue());
        delete(query);
    }

    public void create(final long targetId, final List<TargetGene> targetGenes)
            throws IOException, ParseException, TargetGenesException {
        final Map<String, TargetGeneField> targetGeneFields = processMetadata(targetGenes, targetId);
        setIds(targetId, targetGenes);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGene g : targetGenes) {
                addDoc(writer, g, targetGeneFields);
            }
        }
    }

    public void update(final List<TargetGene> targetGenes) throws IOException, ParseException, TargetGenesException {
        final Map<String, TargetGeneField> targetGeneFields = processMetadata(targetGenes,
                targetGenes.get(0).getTargetId());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new CaseInsensitiveWhitespaceAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            for (TargetGene g : targetGenes) {
                Term term = new Term(IndexField.TARGET_GENE_ID.getValue(), g.getTargetGeneId().toString());
                writer.updateDocument(term, docFromEntry(g, targetGeneFields));
            }
        }
    }

    public SearchResult<TargetGene> filter(final long targetId, final SearchRequest request)
            throws ParseException, IOException {
        final Map<String, TargetGeneField> fieldsMap = getFieldsMap(targetId);
        final Query query = buildQuery(targetId, request.getFilters(), fieldsMap);
        return search(request, query, getSort(request.getOrderInfos(), fieldsMap));
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

    public Query buildQuery(final long targetId,
                            final List<Filter> filters,
                            final Map<String, TargetGeneField> fieldsMap) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        final Query query = getByTermQuery(String.valueOf(targetId), IndexField.TARGET_ID.getValue());
        mainBuilder.add(query, BooleanClause.Occur.MUST);
        if (CollectionUtils.isNotEmpty(filters)) {
            for (Filter filter: filters) {
                addFieldQuery(mainBuilder, filter, fieldsMap.get(filter.getField()).getFilterType());
            }
        }
        return mainBuilder.build();
    }

    public void addFieldQuery(final BooleanQuery.Builder builder,
                              final Filter filter,
                              final FilterType filterType) throws ParseException {
        Query query;
        switch (filterType) {
            case PHRASE:
                query = getByPhraseQuery(filter.getTerms().get(0), filter.getField());
                break;
            case TERM:
                query = getByTermsQuery(filter.getTerms(), filter.getField());
                break;
            case OPTIONS:
                query = getByOptionsQuery(filter.getTerms(), filter.getField());
                break;
            case RANGE:
                query = getByRangeQuery(filter.getRange(), filter.getField());
                break;
            default:
                return;
        }
        builder.add(query, BooleanClause.Occur.MUST);
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
    public void addDoc(IndexWriter writer, TargetGene entry) throws IOException {
    }

    public void addDoc(final IndexWriter writer,
                       final TargetGene entry,
                       final Map<String, TargetGeneField> targetGeneFields) throws IOException, TargetGenesException {
        final Document doc = docFromEntry(entry, targetGeneFields);
        writer.addDocument(doc);
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
                case TARGET_GENE_ID:
                    targetGene.setTargetGeneId(Long.parseLong(field.stringValue()));
                    break;
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

    private Sort getSort(final List<OrderInfo> orderInfos, final Map<String, TargetGeneField> fieldsMap) {
        if (CollectionUtils.isEmpty(orderInfos)) {
            return null;
        }
        final List<SortField> sortFields = new ArrayList<>();
        for (OrderInfo orderInfo : orderInfos) {
            SortField sortField;
            switch (fieldsMap.get(orderInfo.getOrderBy()).getSortType()) {
                case LONG:
                    sortField = new SortField(orderInfo.getOrderBy(), SortField.Type.LONG, orderInfo.isReverse());
                    sortFields.add(sortField);
                    break;
                case FLOAT:
                    sortField = new SortField(orderInfo.getOrderBy(), SortField.Type.FLOAT, orderInfo.isReverse());
                    sortFields.add(sortField);
                    break;
                case STRING:
                    sortField = new SortField(orderInfo.getOrderBy(), SortField.Type.STRING, orderInfo.isReverse());
                    sortFields.add(sortField);
                    break;
                default:
                    break;
            }
        }
        return CollectionUtils.isNotEmpty(sortFields) ?
                new Sort(sortFields.toArray(new SortField[sortFields.size()])) :
                null;
    }

    private Map<String, TargetGeneField> getFieldsMap(final long targetId) throws ParseException, IOException {
        final List<TargetGeneField> targetGeneFields = getFields(targetId);
        return targetGeneFields.stream()
                .collect(Collectors.toMap(TargetGeneField::getField, Function.identity()));
    }

    private Map<String, TargetGeneField> processMetadata(final List<TargetGene> entries, final Long targetId)
            throws IOException, ParseException {
        final Map<String, List<String>> metadataMap = new HashMap<>();
        for (TargetGene g: entries) {
            Map<String, String> metadata = g.getMetadata();
            metadata.forEach((k, v) -> {
                if (metadataMap.containsKey(k)) {
                    metadataMap.get(k).add(v);
                } else {
                    metadataMap.put(k, new ArrayList<>(Collections.singletonList(v)));
                }
            });
        }
        final List<TargetGeneField> targetGeneFields = targetGeneFieldManager.load(targetId);
        final List<String> targetGeneFieldNames = targetGeneFields.stream()
                .map(TargetGeneField::getField)
                .collect(Collectors.toList());
        final List<TargetGeneField> newTargetGeneFields = new ArrayList<>();
        metadataMap.forEach((k, v) -> {
            if (!targetGeneFieldNames.contains(k)) {
                newTargetGeneFields.add(getTargetGeneField(targetId, k, v));
            }
        });
        if (CollectionUtils.isNotEmpty(newTargetGeneFields)) {
            targetGeneFieldManager.create(newTargetGeneFields);
            targetGeneFields.addAll(newTargetGeneFields);
        }
        return targetGeneFields.stream()
                .collect(Collectors.toMap(TargetGeneField::getField, Function.identity()));
    }

    private TargetGeneField getTargetGeneField(final Long targetId, final String field,
                                                      final List<String> filedValues) {
        final TargetGeneField targetGeneField = TargetGeneField.builder()
                .targetId(targetId)
                .field(field)
                .build();
        FilterType filterType;
        SortType sortType;
        try {
            filedValues.stream().map(Float::parseFloat).collect(Collectors.toList());
            filterType = FilterType.RANGE;
            sortType = SortType.FLOAT;
        } catch (NumberFormatException e) {
            for (String v : filedValues) {
                if (Arrays.stream(v.split(" ")).anyMatch(a -> a.length() > keywordMaxLength)) {
                    targetGeneField.setFilterType(FilterType.TERM);
                    targetGeneField.setSortType(SortType.NONE);
                    return targetGeneField;
                }
            }
            filterType = isOptions(filedValues) ? FilterType.OPTIONS : FilterType.TERM;
            sortType = SortType.STRING;
        }
        targetGeneField.setFilterType(filterType);
        targetGeneField.setSortType(sortType);
        return targetGeneField;
    }

    private static boolean isOptions(final List<String> filedValues) {
        return filedValues.stream().distinct().count() / filedValues.size() < OPTIONS_RATIO;
    }

    private static void setIds(final long targetId, final List<TargetGene> targetGenes) {
        targetGenes.forEach(g -> {
            g.setTargetGeneId(getPrimaryKey());
            g.setTargetId(targetId);
        });
    }

    private static Document docFromEntry(final TargetGene entry,
                                         final Map<String, TargetGeneField> targetGeneFields) throws TargetGenesException {
        final Document doc = new Document();
        doc.add(new StringField(IndexField.TARGET_GENE_ID.getValue(),
                entry.getTargetGeneId().toString(), Field.Store.YES));
        doc.add(new StringField(IndexField.TARGET_ID.getValue(), entry.getTargetId().toString(), Field.Store.YES));

        doc.add(new TextField(IndexField.GENE_ID.getValue(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.GENE_ID.getValue(), new BytesRef(entry.getGeneId())));

        doc.add(new TextField(IndexField.GENE_NAME.getValue(), entry.getGeneName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.GENE_NAME.getValue(), new BytesRef(entry.getGeneName())));

        doc.add(new TextField(IndexField.TAX_ID.getValue(), entry.getTaxId().toString(), Field.Store.NO));
        doc.add(new NumericDocValuesField(IndexField.TAX_ID.getValue(), entry.getTaxId()));
        doc.add(new StoredField(IndexField.TAX_ID.getValue(), entry.getTaxId()));

        doc.add(new TextField(IndexField.SPECIES_NAME.getValue(), entry.getSpeciesName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexField.SPECIES_NAME.getValue(), new BytesRef(entry.getSpeciesName())));

        if (entry.getPriority() != null) {
            doc.add(new TextField(IndexField.PRIORITY.getValue(), entry.getPriority().toString(), Field.Store.NO));
            doc.add(new NumericDocValuesField(IndexField.PRIORITY.getValue(), entry.getPriority().getValue()));
            doc.add(new StoredField(IndexField.PRIORITY.getValue(), entry.getPriority().getValue()));
        }

        for (Map.Entry<String, String> mapEntry : entry.getMetadata().entrySet()) {
            String k = mapEntry.getKey();
            String v = mapEntry.getValue();
            TargetGeneField targetGeneField = targetGeneFields.get(k);
            if (targetGeneField.getFilterType() == FilterType.RANGE) {
                try {
                    float value = Float.parseFloat(v);
                    doc.add(new FloatPoint(k, value));
                    doc.add(new FloatDocValuesField(k, value));
                    doc.add(new StoredField(k, value));
                } catch (NumberFormatException e) {
                    throw new TargetGenesException(String.format("Can't add string value to numerical field '%s'", k));
                }
            } else {
                doc.add(new TextField(k, v, Field.Store.YES));
                if (targetGeneField.getSortType() != SortType.NONE) {
                    doc.add(new SortedDocValuesField(k, new BytesRef(v)));
                }
            }
        }
        return doc;
    }

    @AllArgsConstructor
    @Getter
    private enum IndexField {
        TARGET_GENE_ID("ID", FilterType.TERM, SortType.NONE),
        TARGET_ID("Target ID", FilterType.TERM, SortType.LONG),
        GENE_ID("Gene ID", FilterType.TERM, SortType.STRING),
        GENE_NAME("Gene Name", FilterType.PHRASE, SortType.STRING),
        TAX_ID("Tax ID", FilterType.OPTIONS, SortType.LONG),
        SPECIES_NAME("Species Name", FilterType.PHRASE, SortType.STRING),
        PRIORITY("Priority", FilterType.OPTIONS, SortType.LONG),
        METADATA("Metadata", FilterType.NONE, SortType.NONE);

        private final String value;
        private final FilterType type;
        private final SortType sortType;
        private static final Map<String, IndexField> VALUES_MAP = new LinkedHashMap<>();
        static {
            VALUES_MAP.put("ID", TARGET_GENE_ID);
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

    private List<TargetGene> readEntries(final InputStream inputStream, final String extension)
            throws IOException, TargetGenesException {
        if (extension.equalsIgnoreCase(EXCEL_EXTENSION)) {
            return readExcel(inputStream);
        } else if (extension.equals(FileFormat.CSV.getExtension()) ||
                extension.equals(FileFormat.TSV.getExtension())) {
            return readCSV(inputStream, extension);
        } else {
            throw new TargetGenesException(String.format("Unsupported file extension '%s'", extension));
        }
    }

    private List<TargetGene> readCSV(final InputStream inputStream, final String fileExtension) throws IOException {
        final String separator = FileFormat.getSeparatorByExtension(fileExtension);
        String line;
        String[] cells;
        final List<TargetGene> entries = new ArrayList<>();
        try (Reader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            line = bufferedReader.readLine();
            final Header header = getHeader(line, separator);
            while ((line = bufferedReader.readLine()) != null) {
                cells = line.split(separator);

                String geneId = cells[header.getIdIndex()].trim();
                Assert.isTrue(!TextUtils.isBlank(geneId), "Gene ID should not be blank");

                String geneName = cells[header.getNameIndex()].trim();
                Assert.isTrue(!TextUtils.isBlank(geneName), "Gene name should not be blank");

                Long taxId = Long.parseLong(cells[header.getTaxIdIndex()].trim());

                String speciesName = cells[header.getSpeciesNameIndex()].trim();
                Assert.isTrue(!TextUtils.isBlank(speciesName), "Species name should not be blank");

                Map<String, String> metadata = new HashMap<>();
                for (Map.Entry<String, Integer> entry : header.getMetadataIndexes().entrySet()) {
                    String value = cells[entry.getValue()].trim();
                    if (StringUtils.isNotBlank(value)) {
                        metadata.put(entry.getKey(), value);
                    }
                }
                TargetGene gene = TargetGene.builder()
                        .geneId(geneId)
                        .geneName(geneName)
                        .taxId(taxId)
                        .speciesName(speciesName)
                        .metadata(metadata)
                        .build();
                entries.add(gene);
            }
        }
        return entries;
    }

    private List<TargetGene> readExcel(final InputStream inputStream) throws IOException {
        final List<TargetGene> entries = new ArrayList<>();
        final Workbook workbook = new XSSFWorkbook(inputStream);
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
                    .geneId(geneId)
                    .geneName(geneName)
                    .taxId((long) taxIdCell.getNumericCellValue())
                    .speciesName(speciesName)
                    .metadata(metadata)
                    .build();
            entries.add(gene);
        }
        return entries;
    }

    private static long getPrimaryKey() {
        return System.nanoTime();
    }

    private static String getCellValue(final Cell cell) {
        String cellValue = "";
        switch (cell.getCellTypeEnum()) {
            case STRING:
                cellValue = cell.getStringCellValue().trim();
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

    private Header getHeader(final String line, final String separator) {
        int index = 0;
        Integer idIndex = null;
        Integer nameIndex = null;
        Integer taxIdIndex = null;
        Integer speciesNameIndex = null;
        final Map<String, Integer> metadataIndexes = new HashMap<>();
        for (String value : line.split(separator)) {
            if (TextUtils.isBlank(value)) {
                break;
            }
            if (GENE_IDS.stream().anyMatch(value::equalsIgnoreCase)) {
                idIndex = index;
            } else if (GENE_NAMES.stream().anyMatch(value::equalsIgnoreCase)) {
                nameIndex = index;
            } else if (TAX_IDS.stream().anyMatch(value::equalsIgnoreCase)) {
                taxIdIndex = index;
            } else if (SPECIES_NAMES.stream().anyMatch(value::equalsIgnoreCase)) {
                speciesNameIndex = index;
            } else {
                metadataIndexes.put(value, index);
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
