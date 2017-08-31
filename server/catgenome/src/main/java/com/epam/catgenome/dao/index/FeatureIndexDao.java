/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.epam.catgenome.dao.index.indexer.AbstractDocumentBuilder;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.gene.GeneFile;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.grouping.AbstractGroupFacetCollector;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.search.grouping.term.TermGroupFacetCollector;
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

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.index.field.IndexSortField;
import com.epam.catgenome.dao.index.field.SortedIntPoint;
import com.epam.catgenome.dao.index.field.SortedFloatPoint;
import com.epam.catgenome.dao.index.field.SortedStringField;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.index.BookmarkIndexEntry;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import htsjdk.variant.vcf.VCFHeaderLineType;

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

    @Value("#{catgenome['lucene.index.max.size.grouping'] ?: 4L * 1024 * 1024 * 1024}")
    private long luceneIndexMaxSizeForGrouping;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureIndexDao.class);
    private static Pattern viewFieldPattern = Pattern.compile("_.*_v$");
    private static final int FACET_LIMIT = 1000;
    private static final int GENE_LIMIT = 100;
    private static final int GROUP_INITIAL_SIZE = 128;

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
     * Stores features from a specified feature file to the specified project's Lucene index
     * Sample query: featureId:rs44022* AND (variationType:del OR variationType:ins)
     *
     * @param featureFileId a FeatureFile, for which features to save
     * @param projectId a project, for which to write an index
     * @param entries a list of FeatureIndexEntry to write to index
     * @throws IOException
     */
    public void writeLuceneIndexForProject(final Long featureFileId, final long projectId,
                                           final List<? extends FeatureIndexEntry> entries) throws IOException {
        try (
            StandardAnalyzer analyzer = new StandardAnalyzer();
            Directory index = fileManager.createIndexForProject(projectId);
            IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(analyzer).setOpenMode(
                                                                    IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            FacetsConfig facetsConfig = new FacetsConfig();
            facetsConfig.setIndexFieldName(FeatureIndexFields.CHR_ID.getFieldName(),
                    FeatureIndexFields.FACET_CHR_ID.getFieldName());

            for (FeatureIndexEntry entry : entries) {
//                Document document = new Document();
//                addCommonDocumentFields(document, entry, featureFileId);
//
//                if (entry instanceof VcfIndexEntry) {
//                    addVcfDocumentFields(document, entry);
//                }
                AbstractDocumentBuilder creator = AbstractDocumentBuilder.createDocumentCreator(entry);
                Document document = creator.buildDocument(entry, featureFileId);
                writer.addDocument(facetsConfig.build(document));
            }
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

    /**
     * Searches genes by it's ID in project's gene files. Minimum featureId prefix length == 2
     *
     * @param featureId a feature ID prefix to search for
     * @param geneFile a gene file ,from which to search
     * @return a {@code List} of {@code FeatureIndexEntry}
     * @throws IOException
     */
    public IndexSearchResult<FeatureIndexEntry> searchFeatures(String featureId, GeneFile geneFile,
                                                               Integer maxResultsCount)
            throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();

        PrefixQuery prefixQuery = new PrefixQuery(new Term(FeatureIndexFields.FEATURE_ID.getFieldName(),
                featureId.toLowerCase()));
        BooleanQuery.Builder prefixQueryBuilder = new BooleanQuery.Builder();
        prefixQueryBuilder.add(prefixQuery, BooleanClause.Occur.SHOULD);
        prefixQueryBuilder.add(new PrefixQuery(new Term(FeatureIndexFields.FEATURE_NAME.getFieldName(), featureId
                .toLowerCase())), BooleanClause.Occur.SHOULD);

        mainBuilder.add(prefixQueryBuilder.build(), BooleanClause.Occur.MUST);

        BooleanQuery.Builder featureTypeBuilder = new BooleanQuery.Builder();
        featureTypeBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.GENE.getFileValue())), BooleanClause.Occur.SHOULD);
        featureTypeBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.MRNA.getFileValue())), BooleanClause.Occur.SHOULD);
        featureTypeBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                FeatureType.BOOKMARK.getFileValue())), BooleanClause.Occur.SHOULD);
        mainBuilder.add(featureTypeBuilder.build(), BooleanClause.Occur.MUST);

        return searchFileIndexes(Collections.singletonList(geneFile), mainBuilder.build(), null,
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
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }
        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return new IndexSearchResult<>(Collections.emptyList(), false, 0);
            }
            BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
            Query chrQuery = new TermQuery(new Term(FeatureIndexFields.CHROMOSOME_ID.getFieldName(),
                    new BytesRef(chromosome.getId().toString())));
            mainBuilder.add(chrQuery, BooleanClause.Occur.MUST);

            BooleanQuery.Builder featureTypeBuilder = new BooleanQuery.Builder();
            featureTypeBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                    FeatureType.GENE.getFileValue())), BooleanClause.Occur.SHOULD);
            featureTypeBuilder.add(new TermQuery(new Term(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                    FeatureType.EXON.getFileValue())), BooleanClause.Occur.SHOULD);

            mainBuilder.add(featureTypeBuilder.build(), BooleanClause.Occur.MUST);

            BooleanQuery.Builder rangeBuilder = new BooleanQuery.Builder();
            //start in interval
            Query startQuery =
                    IntPoint.newRangeQuery(FeatureIndexFields.START_INDEX.getFieldName(), start, end);
            rangeBuilder.add(startQuery, BooleanClause.Occur.SHOULD);
            //end in interval
            Query endQuery =
                    IntPoint.newRangeQuery(FeatureIndexFields.END_INDEX.getFieldName(), start, end);
            rangeBuilder.add(endQuery, BooleanClause.Occur.SHOULD);

            //feature lasts along all the interval (start < range and end > range)
            BooleanQuery.Builder spanQueryBuilder = new BooleanQuery.Builder();
            Query startExtQuery =
                    IntPoint.newRangeQuery(FeatureIndexFields.START_INDEX.getFieldName(),
                            0, start - 1);
            spanQueryBuilder.add(startExtQuery, BooleanClause.Occur.MUST);

            Query endExtQuery =
                    IntPoint.newRangeQuery(FeatureIndexFields.END_INDEX.getFieldName(),
                            end + 1, Integer.MAX_VALUE);
            spanQueryBuilder.add(endExtQuery, BooleanClause.Occur.MUST);
            rangeBuilder.add(spanQueryBuilder.build(), BooleanClause.Occur.SHOULD);

            mainBuilder.add(rangeBuilder.build(), BooleanClause.Occur.MUST);

            return searchFileIndexes(files, mainBuilder.build(), null,
                    reader.numDocs(), null);

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
                return new IndexSearchResult(Collections.emptyList(), false, 0);
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
            return new IndexSearchResult(Collections.emptyList(), false, 0);
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
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        Map<Integer, FeatureIndexEntry> entryMap = new LinkedHashMap<>();

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return new IndexSearchResult<>(Collections.emptyList(), false, 0);
            }

            IndexSearcher searcher = new IndexSearcher(reader);
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
     * Queries a feature index of a list of files, returning specified page of specified size.
     * If no paging parameters are passed, returns all results
     *
     * @param files a {@link List} of {@link FeatureFile}, which indexes to search
     * @param query a query to search in index
     * @param vcfInfoFields list of info fields to retrieve
     * @param page number of a page to display
     * @param pageSize number of entries per page
     * @param orderBy object, that specifies sorting
     * @return a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @throws IOException if something is wrong in the filesystem
     */
    public <T extends FeatureIndexEntry> IndexSearchResult<T> searchFileIndexesPaging(
            List<? extends FeatureFile> files, Query query, List<String> vcfInfoFields,
            Integer page, Integer pageSize, List<VcfFilterForm.OrderBy> orderBy)
            throws IOException {

        if (CollectionUtils.isEmpty(files)) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        Map<Integer, FeatureIndexEntry> entryMap = new LinkedHashMap<>();

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return new IndexSearchResult<>(Collections.emptyList(), false, 0);
            }

            IndexSearcher searcher = new IndexSearcher(reader);
            int numDocs = page == null ? reader.numDocs() : page * pageSize;
            final TopDocs docs =
                    performSearch(searcher, query, reader, numDocs, createSorting2(orderBy, files));
            int totalHits = docs.totalHits;
            final ScoreDoc[] hits = docs.scoreDocs;

            AbstractDocumentBuilder documentCreator = AbstractDocumentBuilder
                    .createDocumentCreator(files.get(0).getFormat(), vcfInfoFields);
            createIndexEntries(hits, entryMap, searcher, documentCreator, page, pageSize);

            return new IndexSearchResult<>(
                    new ArrayList<T>((Collection<? extends T>) entryMap.values()), false,
                    totalHits);
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
            }
        }
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

    private Sort createSorting2(List<VcfFilterForm.OrderBy> orderBy,
            List<? extends FeatureFile> files) throws IOException {
        if (CollectionUtils.isNotEmpty(orderBy)) {
            ArrayList<SortField> sortFields = new ArrayList<>();
            for (VcfFilterForm.OrderBy o : orderBy) {
                IndexSortField sortField = IndexSortField.getByName(o.getField());
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
                        sf = new SortedSetSortField(sortField.getField().fieldName, o.isDesc());
                    } else {
                        sf = new SortField(sortField.getField().fieldName, sortField.getType(),
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


    private void setMissingValuesOrder(SortField sf, SortField.Type type, boolean desc) {
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

    private SortField.Type determineSortType(InfoItem item) {
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

            String groupByField = getGroupByField2(files, groupBy);
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

    private long getTotalIndexSize(SimpleFSDirectory[] indexes) throws IOException {
        long totalIndexSize = 0;
        for (SimpleFSDirectory index : indexes) {
            totalIndexSize += getTotalIndexSize(index);
        }
        return totalIndexSize;
    }

    private String getGroupByField2(List<VcfFile> files, String groupBy) throws IOException {
        IndexSortField sortField = IndexSortField.getByName(groupBy);
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

    private MultiReader openMultiReader(SimpleFSDirectory[] indexes) throws IOException {
        IndexReader[] readers = new IndexReader[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            readers[i] = DirectoryReader.open(indexes[i]);
        }

        return new MultiReader(readers, true);
    }

    private TopDocs performSearch(IndexSearcher searcher, Query query, IndexReader reader, Integer maxResultsCount,
                                  Sort sort) throws IOException {
        final TopDocs docs;
        int resultsCount = maxResultsCount == null ? reader.numDocs() : maxResultsCount;
        if (sort == null) {
            docs = searcher.search(query, resultsCount);
        } else {
            docs = searcher.search(query, resultsCount, sort);
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

    private void closeIndexes(SimpleFSDirectory[] indexes) {
        for (SimpleFSDirectory index : indexes) {
            IOUtils.closeQuietly(index);
        }
    }

    /**
     * Searches gene IDs, affected by variations in specified VCF files in a specified project
     *
     * @param projectId an ID of a project to search genes
     * @param gene a prefix of a gene ID to search
     * @param vcfFileIds a {@code List} of IDs of VCF files in project to search for gene IDs
     * @return a {@code Set} of gene IDs, that are affected by some variations in specified VCf files
     * @throws IOException
     */
    public Set<String> searchGenesInVcfFilesInProject(long projectId, String gene, List<Long> vcfFileIds) throws
            IOException {
        if (vcfFileIds == null || vcfFileIds.isEmpty()) {
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

        List<Term> terms = vcfFileIds.stream()
                .map(vcfFileId -> new Term(FeatureIndexFields.FILE_ID.getFieldName(), vcfFileId.toString()))
                .collect(Collectors.toList());
        TermsQuery termsQuery = new TermsQuery(terms);
        builder.add(termsQuery, BooleanClause.Occur.MUST);
        BooleanQuery query =  builder.build();

        Set<String> geneIds;

        try (
            Directory index = fileManager.getIndexForProject(projectId);
            IndexReader reader = DirectoryReader.open(index)
        ) {
            if (reader.numDocs() == 0) {
                return Collections.emptySet();
            }

            IndexSearcher searcher = new IndexSearcher(reader);
            final TopDocs docs = searcher.search(query, reader.numDocs());
            final ScoreDoc[] hits = docs.scoreDocs;

            geneIds = fetchGeneIds(hits, searcher);
        } catch (IOException e) {
            LOGGER.error(getMessage(MessagesConstants.ERROR_FEATURE_INDEX_SEARCH_FAILED), e);
            return Collections.emptySet();
        }

        return geneIds;
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

    private void createIndexEntries(final ScoreDoc[] hits, Map<Integer, FeatureIndexEntry> entryMap,
            IndexSearcher searcher, AbstractDocumentBuilder documentCreator, Integer page,
            Integer pageSize) throws IOException {
        int from = page != null ? (page - 1) * pageSize : 0;
        int to = page != null ? Math.min(from + pageSize, hits.length) : hits.length;
        if (from > hits.length) {
            return;
        }
        for (int i = from; i < to; i++) {
            FeatureIndexEntry entry = documentCreator.buildEntry(searcher.doc(hits[i].doc));

            entryMap.put(Objects.hash(entry.getFeatureFileId(),
                    entry.getChromosome() != null ? entry.getChromosome().getId() : null,
                    entry.getStartIndex(), entry.getEndIndex(), entry.getFeatureId()), entry);
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
}
