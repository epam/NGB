/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.dao.index;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.index.field.GeneIndexSortField;
import com.epam.catgenome.dao.index.field.VcfIndexSortField;
import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.dao.index.indexer.GeneDocumentBuilder;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.gene.GeneFilterInfo;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.index.BookmarkIndexEntry;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.Pointer;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.gene.GeneActivityService;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import htsjdk.variant.vcf.VCFHeaderLineType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedSetSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Source:      FeatureIndexDao
 * Created:     02.09.16, 11:15
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A DAO class to work with Lucene index, that stores features: VCF variation, gene features
 * </p>
 */
@Repository
@DependsOn(value = "vcfManager")
public class FeatureIndexDao {
    @Autowired
    private FileManager fileManager;

    @Autowired
    private BookmarkManager bookmarkManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    @Autowired
    private GeneActivityService geneActivityService;

    @Value("#{catgenome['lucene.index.max.size.grouping'] ?: 2L * 1024 * 1024 * 1024}")
    private long luceneIndexMaxSizeForGrouping;

    @Value("${lucene.request.max.values:20}")
    private int luceneRequestMaxValues;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureIndexDao.class);

    private static final int FACET_LIMIT = 1000;
    private static final int GENE_LIMIT = 100;

    public enum FeatureIndexFields {
        UID("uid"),
        FEATURE_ID("featureId"),
        CHROMOSOME_ID("chromosomeId"),
        CHROMOSOME_NAME("chromosomeName"),
        START_INDEX("startIndex"),
        END_INDEX("endIndex"),
        FEATURE_TYPE("featureType"),
        FILE_ID("fileId"),
        FEATURE_NAME("featureName"),

        VARIATION_TYPE("variationType"),
        FAILED_FILTER("failedFilter"),
        GENE_ID("geneId"),
        GENE_IDS("geneIds"), // gene Ids, concatenated by comma
        GENE_NAME("geneName"), // gene name
        GENE_NAMES("geneNames"), // gene names, concatenated by coma
        QUALITY("quality"),
        IS_EXON("is_exon"),

        STRAND("strand"),
        SCORE("score"),
        FRAME("frame"),
        SOURCE("source"),

        // Facet fields
        CHR_ID("chrId"),
        FACET_CHR_ID("facet_chrId"),
        F_UID("f_uid"),
        FACET_UID("facet_uid");

        private String fieldName;

        FeatureIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getGroupName() {
            return "G_" + fieldName;
        }

        public static String getGroupName(String fieldName) {
            return "G_" + fieldName;
        }

        public String getFacetName() {
            return "F_" + fieldName;
        }

        public static String getFacetName(String fieldName) {
            return "F_" + fieldName;
        }
    }

    /**
     * Stores features from a specified feature file to it's Lucene index
     * Sample query: featureId:rs44022* AND (variationType:del OR variationType:ins)
     *
     * @param featureFile a FeatureFile, for which features to save
     * @param entries a list of FeatureIndexEntry to write to index
     * @throws IOException
     */
    public void writeLuceneIndexForFile(final FeatureFile featureFile,
                                        final List<? extends FeatureIndexEntry> entries,
                                        VcfFilterInfo vcfFilterInfo) throws IOException {
        try (
            StandardAnalyzer analyzer = new StandardAnalyzer();
            Directory index = fileManager.createIndexForFile(featureFile);
            IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(analyzer).setOpenMode(
                IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            AbstractDocumentBuilder creator = AbstractDocumentBuilder.createDocumentCreator(
                    entries.isEmpty() ? new FeatureIndexEntry() : entries.get(0));

            FacetsConfig facetsConfig = creator.createFacetsConfig(vcfFilterInfo);

            for (FeatureIndexEntry entry : entries) {
                Document document = creator.buildDocument(entry, featureFile.getId());
                writer.addDocument(facetsConfig.build(document));
            }
        }
    }

    public IndexSearchResult<FeatureIndexEntry> searchFeatures(String featureId,
            FeatureFile featureFile,
            Integer maxResultsCount)
            throws IOException {
        return searchFeatures(featureId, Collections.singletonList(featureFile), maxResultsCount);
    }

    /**
     * Searches genes by it's ID in project's gene files. Minimum featureId prefix length == 2
     *
     * @param featureId a feature ID prefix to search for
     * @param featureFiles a gene file ,from which to search
     * @return a {@code List} of {@code FeatureIndexEntry}
     * @throws IOException
     */
    public IndexSearchResult<FeatureIndexEntry> searchFeatures(final String featureId,
                                                               final List<? extends FeatureFile> featureFiles,
                                                               final Integer maxResultsCount) throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return IndexSearchResult.empty();
        }
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();

        final BooleanQuery.Builder prefixQueryBuilder = new BooleanQuery.Builder()
                .add(new PrefixQuery(new Term(FeatureIndexFields.FEATURE_ID.getFieldName(),
                featureId.toLowerCase())), BooleanClause.Occur.SHOULD)
                .add(new PrefixQuery(new Term(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                featureId.toLowerCase())), BooleanClause.Occur.SHOULD);

        mainBuilder.add(prefixQueryBuilder.build(), BooleanClause.Occur.MUST);

        final BooleanQuery.Builder featureTypeBuilder = new BooleanQuery.Builder()
                .add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.GENE.getFileValue())), BooleanClause.Occur.SHOULD)
                .add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.MRNA.getFileValue())), BooleanClause.Occur.SHOULD)
                .add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.BOOKMARK.getFileValue())), BooleanClause.Occur.SHOULD)
                .add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.BED_FEATURE.getFileValue())), BooleanClause.Occur.SHOULD);
        mainBuilder.add(featureTypeBuilder.build(), BooleanClause.Occur.MUST);

        return searchFileIndexes(featureFiles, mainBuilder.build(), null,
                                 maxResultsCount, new Sort(new SortField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                                                                         SortField.Type.STRING)));
    }

    public IndexSearchResult<FeatureIndexEntry> searchFeaturesInInterval(List<? extends FeatureFile> files, int start,
            int end, Chromosome chromosome)
            throws IOException {

        List<? extends FeatureFile> indexedFiles =
                files.stream().filter(f -> fileManager.indexForFeatureFileExists(f))
                        .collect(Collectors.toList());
        if (indexedFiles.isEmpty()) {
            return IndexSearchResult.empty();
        }
        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return IndexSearchResult.empty();
            }

            final Query query = IndexQueryUtils.intervalQuery(chromosome.getId().toString(), start, end, Arrays.asList(
                    FeatureType.GENE.getFileValue(), FeatureType.EXON.getFileValue()));

            return searchFileIndexes(files, query, null, reader.numDocs(), null);
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
    }

    /**
     * Queries a feature index of a project, specified by ID
     *
     * @param projectId ID of a project, which index to work with
     * @param query a query string
     * @param vcfInfoFields list of info fields to retrieve
     * @return  a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @deprecated
     * @throws FeatureIndexException if something goes wrong
     */
    @Deprecated
    public IndexSearchResult searchLuceneIndexForProject(final long projectId, String query,
                                                     List<String> vcfInfoFields) throws FeatureIndexException {
        try (Analyzer analyzer = new StandardAnalyzer()) {
            QueryParser queryParser = new QueryParser(FeatureIndexFields.FEATURE_ID.getFieldName(), analyzer);
            return searchLuceneIndexForProject(projectId, queryParser.parse(query), vcfInfoFields);
        } catch (IOException | ParseException e) {
            throw new FeatureIndexException("Failed to perform index search for project " + projectId, e);
        }
    }

    /**
     * Queries a feature index of a project, specified by ID
     *
     * @param projectId ID of a project, which index to work with
     * @param query a query to search in index
     * @param vcfInfoFields list of info fields to retrieve
     * @return a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @deprecated
     * @throws IOException
     */
    @Deprecated
    public IndexSearchResult searchLuceneIndexForProject(final long projectId, Query query,
                                                                     List<String> vcfInfoFields) throws IOException {
        return searchLuceneIndexForProject(projectId, query, vcfInfoFields, null, null);
    }

    /**
     * Queries a feature index of a project, specified by ID
     *
     * @param projectId ID of a project, which index to work with
     * @param query a query to search in index
     * @return a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @deprecated
     * @throws IOException
     */
    @Deprecated
    private IndexSearchResult searchLuceneIndexForProject(final long projectId, Query query,
                                             List<String> vcfInfoFields, Integer maxResultsCount, Sort sort) throws
            IOException {
        Map<Integer, FeatureIndexEntry> entryMap = new LinkedHashMap<>();

        int totalHits = 0;
        try (
            Directory index = fileManager.getIndexForProject(projectId);
            IndexReader reader = DirectoryReader.open(index)
        ) {
            if (reader.numDocs() == 0) {
                return IndexSearchResult.empty();
            }

            IndexSearcher searcher = new IndexSearcher(reader);
            final TopDocs docs;
            int resultsCount = maxResultsCount == null ? reader.numDocs() : maxResultsCount;
            if (sort == null) {
                docs = searcher.search(query, resultsCount);
            } else {
                docs = searcher.search(query, resultsCount, sort);
            }

            totalHits = docs.totalHits;
            final ScoreDoc[] hits = docs.scoreDocs;

            Map<Long, BookmarkIndexEntry> foundBookmarkEntries = new HashMap<>(); // for batch bookmarks loading
            AbstractDocumentBuilder documentCreator = AbstractDocumentBuilder.createDocumentCreator(
                    BiologicalDataItemFormat.VCF, vcfInfoFields);
            createIndexEntries(hits, entryMap, searcher, documentCreator);
            setBookmarks(foundBookmarkEntries);
        } catch (IOException e) {
            LOGGER.error(getMessage(MessagesConstants.ERROR_FEATURE_INDEX_SEARCH_FAILED), e);
            return IndexSearchResult.empty();
        }

        return new IndexSearchResult(new ArrayList<>(entryMap.values()), maxResultsCount != null &&
                totalHits > maxResultsCount, totalHits);
    }

    /**
     * Queries a feature index of a list of files. A default index order sorting is being used.
     *
     * @param files a {@link List} of {@link FeatureFile}, which indexes to search
     * @param query a query string to search in index
     * @param vcfInfoFields list of info fields to retrieve
     * @return a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @throws FeatureIndexException if something is wrong in the filesystem or query syntax is wrong
     */
    public IndexSearchResult searchFileIndexes(List<? extends FeatureFile> files, String query,
                                                         List<String> vcfInfoFields) throws FeatureIndexException {
        try (Analyzer analyzer = new StandardAnalyzer()) {
            QueryParser queryParser = new QueryParser(FeatureIndexFields.FEATURE_ID.getFieldName(), analyzer);
            return searchFileIndexes(files, queryParser.parse(query), vcfInfoFields, null, null);
        } catch (IOException | ParseException e) {
            throw new FeatureIndexException("Failed to perform index search for files " +
                                        files.stream().map(BaseEntity::getName).collect(Collectors.joining(", ")), e);
        }
    }

    public GeneFilterInfo getAvailableFieldsToSearch(final List<? extends FeatureFile> files) {
        final Set<String> availableFields = new HashSet<>();
        final Set<String> mainFields = Arrays.stream(FeatureIndexFields.values())
                .map(FeatureIndexFields::getFieldName).collect(Collectors.toSet());
        try {
            for (SimpleFSDirectory file : fileManager.getIndexesForFiles(files)) {
                DirectoryReader reader = DirectoryReader.open(file);
                for (LeafReaderContext subReader : reader.leaves()) {
                    Fields fields = subReader.reader().fields();
                    for (String field : fields) {
                        if (!mainFields.contains(field)) {
                            availableFields.add(field);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to perform index search for files " +
                    files.stream().map(BaseEntity::getName).collect(Collectors.joining(", ")), e);
        }
        return GeneFilterInfo.builder().availableFilters(availableFields).build();
    }

    public Set<String> getAvailableFieldValues(final List<? extends FeatureFile> files, final String fieldName) {
        final Set<String> termValues = new HashSet<>();
        int i = 0;
        try {
            for (SimpleFSDirectory file : fileManager.getIndexesForFiles(files)) {
                DirectoryReader reader = DirectoryReader.open(file);
                for (LeafReaderContext subReader : reader.leaves()) {
                    Terms terms = subReader.reader().terms(fieldName);
                    TermsEnum termsEnum = terms.iterator();
                    BytesRef byteRef = termsEnum.next();
                    while (byteRef != null && i < luceneRequestMaxValues) {
                        termValues.add(byteRef.utf8ToString().toLowerCase(Locale.ROOT));
                        byteRef = termsEnum.next();
                        i++;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to perform index search for files " +
                    files.stream().map(BaseEntity::getName).collect(Collectors.joining(", ")), e);
        }
        return termValues;
    }

    /**
     * Queries a feature index of a list of files
     *
     * @param files a {@link List} of {@link FeatureFile}, which indexes to search
     * @param query a query to search in index
     * @param vcfInfoFields list of info fields to retrieve
     * @param maxResultsCount specifies a maximum number of search results to get
     * @param sort specifies sorting
     * @return a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @throws IOException if something is wrong in the filesystem
     */
    public <T extends FeatureIndexEntry> IndexSearchResult<T> searchFileIndexes(List<? extends FeatureFile> files,
                                                                                Query query, List<String> vcfInfoFields,
                                                                                Integer maxResultsCount, Sort sort)
        throws IOException {
        if (CollectionUtils.isEmpty(files)) {
            return IndexSearchResult.empty();
        }

        Map<Integer, FeatureIndexEntry> entryMap = new LinkedHashMap<>();

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return IndexSearchResult.empty();
            }

            IndexSearcher searcher = new IndexSearcher(reader, taskExecutorService.getSearchExecutor());
            final TopDocs docs = performSearch(searcher, query, reader, maxResultsCount, sort);

            int totalHits = docs.totalHits;
            final ScoreDoc[] hits = docs.scoreDocs;

            Map<Long, BookmarkIndexEntry> foundBookmarkEntries = new HashMap<>(); // for batch bookmarks loading
            AbstractDocumentBuilder documentCreator = AbstractDocumentBuilder.createDocumentCreator(
                    files.get(0).getFormat(), vcfInfoFields);
            createIndexEntries(hits, entryMap, searcher, documentCreator);
            setBookmarks(foundBookmarkEntries);

            return new IndexSearchResult<>(new ArrayList<T>((Collection<? extends T>) entryMap.values()),
                                           maxResultsCount != null &&
                                           totalHits > maxResultsCount, totalHits);
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
    }

    /**
     * Queries gene index entry by 'uid' Lucene document field
     *
     * @param featureFile the {@link GeneFile} which indexes to search
     * @param uid Lucene document field
     * @return {@link GeneIndexEntry} if such document exists
     * @throws IOException if something is wrong in the filesystem
     */
    public GeneIndexEntry searchGeneFeatureByUid(final GeneFile featureFile, final String uid)
            throws IOException {
        final Term uidTerm = new Term(FeatureIndexFields.UID.getFieldName(), uid);
        final SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(Collections.singletonList(featureFile));
        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return null;
            }

            final IndexSearcher searcher = new IndexSearcher(reader, taskExecutorService.getSearchExecutor());
            final GeneDocumentBuilder documentCreator = new GeneDocumentBuilder();
            final Integer docId = documentCreator.findDocumentIdByUid(searcher, uidTerm);
            final Document document = searcher.doc(docId);

            return buildGeneIndexEntry(documentCreator, document);
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
    }

    /**
     * Updates gene feature by 'uid' Lucene document field
     *
     * @param featureFile the {@link GeneFile} which indexes to search
     * @param uid Lucene document field
     * @param geneContent a new gene content
     * @return {@link GeneIndexEntry} if such document exists
     * @throws IOException if something is wrong in the filesystem
     */
    public GeneHighLevel updateGeneFeatureByUid(final GeneFile featureFile, final String uid,
                                                final GeneHighLevel geneContent, final Gene.Origin geneFileType)
            throws IOException {
        final Term uidTerm = new Term(FeatureIndexFields.UID.getFieldName(), uid);
        final GeneHighLevel newGeneContent = prepareGeneContentForDocument(geneContent);
        final SimpleFSDirectory index = fileManager.createIndexForFile(featureFile);
        final GeneIndexEntry oldEntry;
        try (StandardAnalyzer analyzer = new StandardAnalyzer();
             MultiReader reader = openMultiReader(new SimpleFSDirectory[] {index});
             IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(analyzer)
                     .setOpenMode(IndexWriterConfig.OpenMode.APPEND))) {
            if (reader.numDocs() == 0) {
                throw new IllegalStateException("Failed to find any documents");
            }

            final IndexSearcher searcher = new IndexSearcher(reader, taskExecutorService.getSearchExecutor());
            final GeneDocumentBuilder documentCreator = new GeneDocumentBuilder();
            final FacetsConfig facetsConfig = documentCreator.createFacetsConfig(null);
            final Integer docId = documentCreator.findDocumentIdByUid(searcher, uidTerm);
            final Document oldDocument = searcher.doc(docId);
            if (Objects.isNull(oldDocument)) {
                throw new IllegalStateException(String.format("Failed to find document by ID '%d'", docId));
            }
            oldEntry = buildGeneIndexEntry(documentCreator, oldDocument);

            final String featureId = GeneUtils.getFeatureId(geneContent.getFeature(),
                    MapUtils.emptyIfNull(geneContent.getAttributes()));
            final String featureName = getFeatureName(geneFileType, geneContent.getFeature(),
                    MapUtils.emptyIfNull(geneContent.getAttributes()));
            final Document newDocument = documentCreator.copyGeneDocument(newGeneContent, oldDocument, uid,
                    featureId, featureName);

            writer.updateDocument(uidTerm, facetsConfig.build(newDocument));
        } finally {
            IOUtils.closeQuietly(index);
        }

        geneActivityService.saveGeneActivities(newGeneContent, oldEntry);

        return geneContent;
    }

    public int getTotalVariationsCountFacet(List<? extends FeatureFile> files, Query query) throws IOException {
        if (CollectionUtils.isEmpty(files)) {
            return 0;
        }

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);
        long totalIndexSize = getTotalIndexSize(indexes);
        if (totalIndexSize > luceneIndexMaxSizeForGrouping) {
            return 0;
        }

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return 0;
            }

            FacetsCollector facetsCollector = new FacetsCollector();
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.search(query, facetsCollector);

            Facets facets = new SortedSetDocValuesFacetCounts(new DefaultSortedSetDocValuesReaderState(reader,
                                                           FeatureIndexFields.FACET_UID.fieldName), facetsCollector);
            FacetResult res = facets.getTopChildren(reader.numDocs(), FeatureIndexFields.F_UID.getFieldName());
            if (res == null) {
                return 0;
            }

            return res.childCount;
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
    }

    public Sort createVcfSorting(final List<VcfFilterForm.OrderBy> orderBy,
                                 final List<? extends FeatureFile> files) throws IOException {
        if (CollectionUtils.isNotEmpty(orderBy)) {
            ArrayList<SortField> sortFields = new ArrayList<>();
            for (VcfFilterForm.OrderBy o : orderBy) {
                VcfIndexSortField sortField = VcfIndexSortField.getByName(o.getField());
                if (sortField == null) {
                    VcfFilterInfo info = vcfManager.getFiltersInfo(
                            files.stream().map(BaseEntity::getId).collect(Collectors.toList()));

                    InfoItem infoItem = info.getInfoItemMap().get(o.getField());
                    Assert.notNull(infoItem, "Unknown sort field: " + o.getField());

                    SortField.Type type = determineSortType(infoItem);
                    SortField sf =
                            new SortedSetSortField(infoItem.getName().toLowerCase(), o.isDesc());

                    setMissingValuesOrder(sf, type, o.isDesc());

                    sortFields.add(sf);
                } else {
                    SortField sf;
                    if (sortField.getType() == SortField.Type.STRING) {
                        sf = new SortedSetSortField(sortField.getField().getFieldName(), o.isDesc());
                    } else {
                        sf = new SortField(sortField.getField().getFieldName(), sortField.getType(),
                                o.isDesc());
                    }
                    setMissingValuesOrder(sf, sortField.getType(), o.isDesc());

                    sortFields.add(sf);
                }
            }

            return new Sort(sortFields.toArray(new SortField[sortFields.size()]));
        }

        return null;
    }

    public Sort createGeneSorting(final List<GeneFilterForm.OrderBy> orderBy,
                                  final List<? extends FeatureFile> featureFiles) {
        if (CollectionUtils.isNotEmpty(orderBy)) {
            final ArrayList<SortField> sortFields = new ArrayList<>();
            for (GeneFilterForm.OrderBy o : orderBy) {
                final GeneIndexSortField sortField = GeneIndexSortField.getByName(o.getField());
                if (sortField != null) {
                    final SortField sf;
                    if (sortField.getType() == SortField.Type.STRING) {
                        sf = new SortedSetSortField(sortField.getField().getFieldName(), o.isDesc());
                    } else {
                        sf = new SortField(sortField.getField().getFieldName(), sortField.getType(),
                                o.isDesc());
                    }
                    setMissingValuesOrder(sf, sortField.getType(), o.isDesc());

                    sortFields.add(sf);
                } else {
                    if(getAvailableFieldsToSearch(featureFiles).getAvailableFilters().contains(o.getField())) {
                        SortField.Type type = SortField.Type.STRING;
                        SortField sf = new SortedSetSortField(o.getField(), o.isDesc());
                        setMissingValuesOrder(sf, type, o.isDesc());
                        sortFields.add(sf);
                    }
                }
            }

            return sortFields.isEmpty() ? new Sort() : new Sort(sortFields.toArray(new SortField[sortFields.size()]));
        }

        return null;
    }

    private void setMissingValuesOrder(final SortField sf, final SortField.Type type, final boolean desc) {
        if (sf instanceof SortedSetSortField) {
            sf.setMissingValue(desc ? SortField.STRING_FIRST : SortField.STRING_LAST);
        } else {
            switch (type) {
                case STRING:
                    sf.setMissingValue(desc ? SortField.STRING_FIRST : SortField.STRING_LAST);
                    break;
                case FLOAT:
                    sf.setMissingValue(Float.MIN_VALUE);
                    break;
                case INT:
                    sf.setMissingValue(Integer.MIN_VALUE);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected sort type: " + type);
            }
        }
    }

    private SortField.Type determineSortType(final InfoItem item) {
        switch (item.getType()) {
            case Integer:
                return SortField.Type.INT;
            case Float:
                return SortField.Type.FLOAT;
            default:
                return SortField.Type.STRING;
        }
    }

    /**
     * Groups variations from specified {@link List} of {@link VcfFile}s by specified field
     * @param files a {@link List} of {@link FeatureFile}, which indexes to search
     * @param query a query to search in index
     * @param groupBy a field to perform grouping
     * @return a {@link List} of {@link Group}s, mapping field value to number of variations, having this value
     * @throws IOException if something goes wrong with the file system
     */
    public List<Group> groupVariations(List<VcfFile> files, Query query, String groupBy) throws IOException {
        List<Group> res = new ArrayList<>();

        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptyList();
        }

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);
        long totalIndexSize = getTotalIndexSize(indexes);
        if (totalIndexSize > luceneIndexMaxSizeForGrouping) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_FEATURE_INEDX_TOO_LARGE));
        }

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return Collections.emptyList();
            }

            IndexSearcher searcher = new IndexSearcher(reader);

            String groupByField = getGroupByField(files, groupBy);
            SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(reader,
                    FeatureIndexFields.getFacetName(groupByField));
            FacetsCollector collector = new FacetsCollector();
            FacetsCollector.search(searcher, query, 10, collector);

            Facets facets = new SortedSetDocValuesFacetCounts(state, collector);
            FacetResult result = facets.getTopChildren(reader.numDocs(), groupByField);
            for (int i = 0; i < result.childCount; i++) {
                LabelAndValue lv = result.labelValues[i];
                res.add(new Group(lv.label, lv.value.intValue()));
            }
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }

        return res;
    }

    public long getTotalIndexSize(SimpleFSDirectory[] indexes) throws IOException {
        long totalIndexSize = 0;
        for (SimpleFSDirectory index : indexes) {
            totalIndexSize += getTotalIndexSize(index);
        }
        return totalIndexSize;
    }

    private long getTotalIndexSize(Directory index) throws IOException {
        long totalFileSize = 0L;
        String[] files = index.listAll();
        if (files == null) {
            return 0;
        }
        for (int i = 0; i < files.length; i++) {
            totalFileSize += index.fileLength(files[i]);
        }
        return totalFileSize;
    }

    private String getGroupByField(List<VcfFile> files, String groupBy) throws IOException {
        VcfIndexSortField sortField = VcfIndexSortField.getByName(groupBy);
        if (sortField == null) {
            VcfFilterInfo info = vcfManager.getFiltersInfo(
                    files.stream().map(BaseEntity::getId).collect(Collectors.toList()));

            InfoItem infoItem = info.getInfoItemMap().get(groupBy);
            Assert.notNull(infoItem, "Unknown sort field: " + groupBy);

            if (infoItem.getType() == VCFHeaderLineType.Integer
                    || infoItem.getType() == VCFHeaderLineType.Float) {
                return infoItem.getName().toLowerCase();
            } else {
                return infoItem.getName().toLowerCase();
            }
        } else {
            if (sortField.getType() == SortField.Type.INT
                    || sortField.getType() == SortField.Type.FLOAT) {
                return sortField.getField().fieldName;
            } else {
                return sortField.getField().fieldName;
            }
        }
    }

    public MultiReader openMultiReader(SimpleFSDirectory[] indexes) throws IOException {
        IndexReader[] readers = new IndexReader[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            readers[i] = DirectoryReader.open(indexes[i]);
        }

        return new MultiReader(readers, true);
    }


    public TopDocs performSearch(IndexSearcher searcher, Query query, IndexReader reader,
            Integer maxResultsCount, Sort sort) throws IOException {
        final TopDocs docs;
        int resultsCount = maxResultsCount == null ? reader.numDocs() : maxResultsCount;
        Query constantQuery = new ConstantScoreQuery(query);
        if (sort == null) {
            docs = searcher.search(constantQuery, resultsCount);
        } else {
            docs = searcher.search(constantQuery, resultsCount, sort, false, false);
        }
        return docs;
    }


    private void setBookmarks(Map<Long, BookmarkIndexEntry> foundBookmarkEntries) {
        if (!foundBookmarkEntries.isEmpty()) { // batch load all bookmarks
            List<Bookmark> bookmarks = bookmarkManager.loadBookmarksByIds(foundBookmarkEntries.keySet());
            bookmarks.forEach(b -> foundBookmarkEntries.get(b.getId()).setBookmark(b));
        }
    }


    /**
     * Returns a {@code List} of chromosome IDs from specified files, where variations exist and satisfy a
     * specified query
     *
     * @param files a list of {@link FeatureFile}s to search chromosomes
     * @param query     a query to filter variations
     * @return a {@code List} of chromosome IDs
     * @throws IOException
     */
    public List<Long> getChromosomeIdsWhereVariationsPresentFacet(List<? extends FeatureFile> files, String query)
        throws FeatureIndexException {
        try (Analyzer analyzer = new StandardAnalyzer()) {
            QueryParser queryParser = new QueryParser(FeatureIndexFields.FEATURE_ID.getFieldName(), analyzer);
            return getChromosomeIdsWhereVariationsPresentFacet(files, queryParser.parse(query));
        } catch (IOException | ParseException e) {
            throw new FeatureIndexException("Failed to perform facet index search for files " + files.stream()
                .map(BaseEntity::getName).collect(Collectors.joining(", ")), e);
        }
    }

    /**
     * Returns a {@code List} of chromosome IDs from specified files, where variations exist and satisfy a
     * specified query
     *
     * @param files a list of {@link FeatureFile}s to search chromosomes
     * @param query     a query to filter variations
     * @return a {@code List} of chromosome IDs
     * @throws IOException
     */
    public List<Long> getChromosomeIdsWhereVariationsPresentFacet(List<? extends FeatureFile> files, Query query)
        throws IOException {
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptyList();
        }

        List<Long> chromosomeIds = new ArrayList<>();

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return Collections.emptyList();
            }

            FacetsCollector facetsCollector = new FacetsCollector();
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.search(query, facetsCollector);

            Facets facets = new SortedSetDocValuesFacetCounts(new DefaultSortedSetDocValuesReaderState(reader,
                                                   FeatureIndexFields.FACET_CHR_ID.getFieldName()), facetsCollector);
            FacetResult res = facets.getTopChildren(FACET_LIMIT, FeatureIndexFields.CHR_ID.getFieldName());
            if (res == null) {
                return Collections.emptyList();
            }

            for (LabelAndValue labelAndValue : res.labelValues) {
                chromosomeIds.add(Long.parseLong(labelAndValue.label));
            }
        } finally {
            closeIndexes(indexes);
        }

        return chromosomeIds;
    }

    public long getLuceneIndexMaxSizeForGrouping() {
        return luceneIndexMaxSizeForGrouping;
    }

    private void closeIndexes(SimpleFSDirectory[] indexes) {
        for (SimpleFSDirectory index : indexes) {
            IOUtils.closeQuietly(index);
        }
    }

    public Set<String> searchGenesInVcfFiles(String gene, List<VcfFile> vcfFiles) throws IOException {
        if (CollectionUtils.isEmpty(vcfFiles)) {
            return Collections.emptySet();
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        PrefixQuery geneIdPrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE_ID.getFieldName(),
                                                                 gene.toLowerCase()));
        PrefixQuery geneNamePrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE_NAME.getFieldName(),
                                                                   gene.toLowerCase()));
        BooleanQuery.Builder geneIdOrNameQuery = new BooleanQuery.Builder();
        geneIdOrNameQuery.add(geneIdPrefixQuery, BooleanClause.Occur.SHOULD);
        geneIdOrNameQuery.add(geneNamePrefixQuery, BooleanClause.Occur.SHOULD);

        builder.add(geneIdOrNameQuery.build(), BooleanClause.Occur.MUST);
        BooleanQuery query =  builder.build();

        Set<String> geneIds = new HashSet<>();

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(vcfFiles);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return Collections.emptySet();
            }
            if (StringUtils.isEmpty(gene)) {
                Fields fields = MultiFields.getFields(reader);
                fetchTermValues(geneIds, fields, FeatureIndexFields.GENE_ID.getFieldName());
                fetchTermValues(geneIds, fields, FeatureIndexFields.GENE_NAME.getFieldName());
            } else {
                IndexSearcher searcher = new IndexSearcher(reader);
                final TopDocs docs = searcher.search(query, reader.numDocs());
                final ScoreDoc[] hits = docs.scoreDocs;
                geneIds = fetchGeneIds(hits, searcher);
            }

        } catch (IOException e) {
            LOGGER.error(getMessage(MessagesConstants.ERROR_FEATURE_INDEX_SEARCH_FAILED), e);
            return Collections.emptySet();
        }

        return geneIds;
    }

    private void fetchTermValues(Set<String> geneIds, Fields fields, String fieldName) throws IOException {
        Terms terms = fields.terms(fieldName);
        if (terms != null) {
            TermsEnum iterator = terms.iterator();
            BytesRef next = iterator.next();
            while (next != null) {
                geneIds.add(next.utf8ToString());
                next = iterator.next();
            }
        }
    }

    /**
     * Deletes features from specified feature files from project's index
     *
     * @param projectId a project to delete index entries
     * @param fileIds a list of Pair of feature types to file Ids, which entries to delete. To delete gene file
     *                entries, pass FeatureType.GENE
     */
    public void deleteFromIndexByFileId(final long projectId, List<Pair<FeatureType, Long>> fileIds) {
        if (fileIds == null || fileIds.isEmpty() || !fileManager.indexForProjectExists(projectId)) {
            return;
        }

        try (
            StandardAnalyzer analyzer = new StandardAnalyzer();
            Directory index = fileManager.getIndexForProject(projectId);
            IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(analyzer).setOpenMode(
                                                                        IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            if (fileManager.indexForProjectExists(projectId)) {
                for (Pair<FeatureType, Long> id : fileIds) {
                    deleteDocumentByTypeAndId(id.getKey(), id.getValue(), writer);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception while deleting from index:", e);
        }
    }

    /**
     * Returns gene features according to query with all indexed fields
     *
     * @param featureFile the {@link GeneFile} which indexes to search
     * @param chrId chromosome ID string
     * @param filterForm chromosome ID string
     * @param sort the sort for result
     * @return found genes
     */
    public IndexSearchResult<GeneIndexEntry> searchGeneFeaturesFully(final GeneFile featureFile, final String chrId,
                                                                     final GeneFilterForm filterForm, final Sort sort)
            throws IOException {
        final SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(Collections.singletonList(featureFile));
        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return IndexSearchResult.empty();
            }

            final IndexSearcher searcher = new IndexSearcher(reader, taskExecutorService.getSearchExecutor());
            final GeneDocumentBuilder documentCreator = new GeneDocumentBuilder();

            final TopDocs docs = performIntervalSearch(chrId, filterForm, sort, reader, searcher);
            if (Objects.isNull(docs) || ArrayUtils.isEmpty(docs.scoreDocs)) {
                return IndexSearchResult.empty();
            }

            final ScoreDoc[] hits = docs.scoreDocs;
            final int totalHits = docs.totalHits;
            final List<GeneIndexEntry> values = Arrays.stream(hits)
                    .map(hit -> searchDocument(searcher, hit))
                    .map(document -> buildGeneIndexEntry(documentCreator, document))
                    .collect(Collectors.toList());

            final ScoreDoc lastEntry = hits.length == 0 ? null : hits[hits.length-1];
            filterForm.setPointer(Pointer.fromScoreDoc(lastEntry));
            return new IndexSearchResult<>(values, false, totalHits, lastEntry);
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
    }

    private void deleteDocumentByTypeAndId(FeatureType type, Long id, IndexWriter writer) throws IOException {
        BooleanQuery.Builder deleteQueryBuilder = new BooleanQuery.Builder();
        TermQuery idQuery = new TermQuery(new Term(FeatureIndexFields.FILE_ID.getFieldName(),
                                                   id.toString()));
        deleteQueryBuilder.add(idQuery, BooleanClause.Occur.MUST);

        if (type != FeatureType.GENE) {
            TermQuery typeQuery = new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                                                         type.getFileValue()));
            deleteQueryBuilder.add(typeQuery, BooleanClause.Occur.MUST);
        } else {
            deleteQueryBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                                                  FeatureType.BOOKMARK.getFileValue())), BooleanClause.Occur.MUST_NOT);
            deleteQueryBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                                                FeatureType.VARIATION.getFileValue())), BooleanClause.Occur.MUST_NOT);
        }

        writer.deleteDocuments(deleteQueryBuilder.build());
    }

    private void createIndexEntries(final ScoreDoc[] hits, Map<Integer, FeatureIndexEntry> entryMap,
            IndexSearcher searcher, AbstractDocumentBuilder documentCreator) throws IOException {
        createIndexEntries(hits, entryMap, searcher, documentCreator, null, null);
    }

    private ScoreDoc createIndexEntries(final ScoreDoc[] hits, Map<Integer, FeatureIndexEntry> entryMap,
            IndexSearcher searcher, AbstractDocumentBuilder documentCreator, Integer page,
            Integer pageSize) throws IOException {
        int from = page != null ? (page - 1) * pageSize : 0;
        int to = page != null ? Math.min(from + pageSize, hits.length) : hits.length;
        if (from > hits.length) {
            return null;
        }

        for (int i = from; i < to; i++) {
            FeatureIndexEntry entry = documentCreator.buildEntry(searcher, hits[i].doc);

            entryMap.put(Objects.hash(entry.getFeatureFileId(),
                    entry.getChromosome() != null ? entry.getChromosome().getId() : null,
                    entry.getStartIndex(), entry.getEndIndex(), entry.getFeatureId()), entry);
        }
        int min = Math.max(to, hits.length);
        if (hits.length == 0) {
            return null;
        } else {
            return hits[min-1];
        }

    }


    private Set<String> fetchGeneIds(final ScoreDoc[] hits, IndexSearcher searcher) throws IOException {
        Set<String> geneIds = new HashSet<>();
        Set<String> requiredFields = new HashSet<>();
        requiredFields.add(FeatureIndexFields.GENE_ID.getFieldName());
        requiredFields.add(FeatureIndexFields.GENE_NAME.getFieldName());
        for (ScoreDoc hit : hits) {
            if (geneIds.size() > GENE_LIMIT * 2) {
                break;
            }
            int docId = hit.doc;
            Document d = searcher.doc(docId, requiredFields);
            String geneId = d.get(FeatureIndexFields.GENE_ID.getFieldName());
            String geneName = d.get(FeatureIndexFields.GENE_NAME.getFieldName());
            if (geneId != null) {
                geneIds.add(geneId);
            }
            if (geneName != null) {
                geneIds.add(geneName);
            }
        }

        return geneIds;
    }

    private GeneHighLevel prepareGeneContentForDocument(final GeneHighLevel geneContent) {
        final Integer frame = geneContent.getFrame();
        geneContent.setFrame(Objects.isNull(frame) ? -1 : frame);

        final Float score = geneContent.getScore();
        geneContent.setScore(Objects.isNull(score) ? -1F : score);

        final StrandSerializable strand = geneContent.getStrand();
        geneContent.setStrand(Objects.isNull(strand) ? StrandSerializable.NONE : strand);

        final String featureType = geneContent.getFeature();
        geneContent.setFeature(Objects.isNull(featureType) ? StringUtils.EMPTY : featureType);

        return geneContent;
    }

    private String getFeatureName(final Gene.Origin geneFileType, final String feature,
                                  final Map<String, String> attributes) {
        if (Objects.isNull(feature)) {
            return StringUtils.EMPTY;
        }
        final String featureName = Gene.Origin.GTF.equals(geneFileType)
                ? GeneUtils.getFeatureName(feature, attributes)
                : attributes.get("Name");
        return Objects.isNull(featureName) ? StringUtils.EMPTY : featureName;
    }

    private GeneIndexEntry buildGeneIndexEntry(final GeneDocumentBuilder documentCreator, final Document document) {
        final GeneIndexEntry entry = documentCreator.buildEntry(document);
        final Set<String> featureIndexFieldNames = Arrays.stream(FeatureIndexFields.values())
                .map(FeatureIndexFields::getFieldName)
                .collect(Collectors.toSet());
        final Map<String, String> attributes = document.getFields().stream()
                .map(IndexableField::name)
                .distinct()
                .filter(fieldName -> !featureIndexFieldNames.contains(fieldName))
                .collect(Collectors.toMap(Function.identity(), document::get));
        entry.setAttributes(attributes);
        return entry;
    }

    private Document searchDocument(final IndexSearcher searcher, final ScoreDoc hit) {
        try {
            return searcher.doc(hit.doc);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private TopDocs performIntervalSearch(final String chrId, final GeneFilterForm filterForm,
                                          final Sort sort, final MultiReader reader, final IndexSearcher searcher)
            throws IOException {
        final Query queryWithFeatureTypeFilter = IndexQueryUtils.intervalQuery(chrId, filterForm.getStartIndex(),
                filterForm.getEndIndex(), filterForm.getFeatureTypes());
        final Pointer pointer = filterForm.getPointer();
        final Integer numDocs = filterForm.getPageSize();
        final TopDocs docs = Objects.isNull(pointer)
                ? performSearch(searcher, queryWithFeatureTypeFilter, reader, numDocs, sort)
                : performSearchAfter(searcher, queryWithFeatureTypeFilter, pointer.toScoreDoc(), numDocs, sort);

        if (Objects.isNull(docs)) {
            return null;
        }

        // if search by specific feature type is empty let's try to find any features
        if (docs.totalHits == 0 && CollectionUtils.isNotEmpty(filterForm.getFeatureTypes())) {
            final Query queryWithoutFeatureTypeFilter = IndexQueryUtils.intervalQuery(chrId, filterForm.getStartIndex(),
                    filterForm.getEndIndex(), null);
            return Objects.isNull(pointer)
                    ? performSearch(searcher, queryWithoutFeatureTypeFilter, reader, numDocs, sort)
                    : performSearchAfter(searcher, queryWithoutFeatureTypeFilter, pointer.toScoreDoc(), numDocs, sort);
        }
        return docs;
    }

    private TopDocs performSearchAfter(final IndexSearcher searcher, final Query query, final ScoreDoc pointer,
                                       final Integer pageSize, final Sort sort) throws IOException {
        final Query constantQuery = new ConstantScoreQuery(query);
        return Objects.isNull(sort)
                ? searcher.searchAfter(pointer, constantQuery, pageSize)
                : searcher.searchAfter(pointer, constantQuery, pageSize, sort, false, false);
    }
}
