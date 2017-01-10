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

import java.io.IOException;
import java.util.ArrayList;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatPoint;
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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.BookmarkIndexEntry;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Bookmark;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.reference.BookmarkManager;

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
public class FeatureIndexDao {
    @Autowired
    private FileManager fileManager;

    @Autowired
    private BookmarkManager bookmarkManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureIndexDao.class);
    private static Pattern viewFieldPattern = Pattern.compile("_.*_v$");
    private static final int FACET_LIMIT = 1000;

    public enum FeatureIndexFields {
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
        GENE("gene"),
        GENE_NAMES("geneNames"), // gene Ids, concatenated by comma
        GENE_REAL_NAME("geneRealName"), // gene name
        GENE_REAL_NAMES("geneRealNames"), // gene names, concatenated by coma
        QUALITY("quality"),
        IS_EXON("is_exon"),

        // Facet fields
        CHR_ID("chrId"),
        FACET_CHR_ID("facet_chrId");

        private String fieldName;

        FeatureIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    private static final FieldType STRING_FIELD_STORED_SORTED = new FieldType(StringField.TYPE_STORED);
    static {
        STRING_FIELD_STORED_SORTED.setDocValuesType(DocValuesType.SORTED);
        STRING_FIELD_STORED_SORTED.freeze();
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
                Document document = new Document();
                addCommonDocumentFields(document, entry, featureFileId);

                if (entry instanceof VcfIndexEntry) {
                    addVcfDocumentFields(document, entry);
                }

                writer.addDocument(facetsConfig.build(document));
            }
        }
    }

    public void writeLuceneIndexForFile(final FeatureFile featureFile,
                                        final List<? extends FeatureIndexEntry> entries) throws IOException {
        try (
            StandardAnalyzer analyzer = new StandardAnalyzer();
            Directory index = fileManager.createIndexForFile(featureFile);
            IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(analyzer).setOpenMode(
                IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
        ) {
            FacetsConfig facetsConfig = new FacetsConfig();
            facetsConfig.setIndexFieldName(FeatureIndexFields.CHR_ID.getFieldName(),
                                           FeatureIndexFields.FACET_CHR_ID.getFieldName());

            for (FeatureIndexEntry entry : entries) {
                Document document = new Document();
                addCommonDocumentFields(document, entry, featureFile.getId());

                if (entry instanceof VcfIndexEntry) {
                    addVcfDocumentFields(document, entry);
                }

                writer.addDocument(facetsConfig.build(document));
            }
        }
    }

    /**
     * Searches genes by it's ID in project's gene files. Minimum featureId prefix length == 2
     *
     * @param featureId a feature ID prefix to search for
     * @param geneFile a gene file ,from which to search
     * @return a {@code List} of {@code FeatureIndexEntry}
     * @throws IOException
     */
    public IndexSearchResult searchFeatures(String featureId, GeneFile geneFile, Integer maxResultsCount)
            throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult(Collections.emptyList(), false, 0);
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

    /**
     * Queries a feature index of a project, specified by ID
     *
     * @param projectId ID of a project, which index to work with
     * @param query a query string
     * @param vcfInfoFields list of info fields to retrieve
     * @return  a {List} of {@code FeatureIndexEntry} objects that satisfy index query
     * @throws FeatureIndexException if something goes wrong
     */
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
     * @throws IOException
     */
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
     * @throws IOException
     */
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
            createIndexEntries(hits, entryMap, foundBookmarkEntries, searcher, vcfInfoFields);
            setBookmarks(foundBookmarkEntries);
        } catch (IOException e) {
            LOGGER.error(MessageHelper.getMessage(MessagesConstants.ERROR_FEATURE_INDEX_SEARCH_FAILED), e);
            return new IndexSearchResult(Collections.emptyList(), false, 0);
        }

        return new IndexSearchResult(new ArrayList<>(entryMap.values()), maxResultsCount != null &&
                                                                         totalHits > maxResultsCount, totalHits);
    }

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

    public IndexSearchResult searchFileIndexes(List<? extends FeatureFile> files, Query query,
                                               List<String> vcfInfoFields, Integer maxResultsCount, Sort sort)
        throws IOException {
        if (CollectionUtils.isEmpty(files)) {
            return new IndexSearchResult(Collections.emptyList(), false, 0);
        }

        Map<Integer, FeatureIndexEntry> entryMap = new LinkedHashMap<>();

        int totalHits = 0;
        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(files);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return new IndexSearchResult(Collections.emptyList(), false, 0);
            }

            IndexSearcher searcher = new IndexSearcher(reader);
            final TopDocs docs = performSearch(searcher, query, reader, maxResultsCount, sort);

            totalHits = docs.totalHits;
            final ScoreDoc[] hits = docs.scoreDocs;

            Map<Long, BookmarkIndexEntry> foundBookmarkEntries = new HashMap<>(); // for batch bookmarks loading
            createIndexEntries(hits, entryMap, foundBookmarkEntries, searcher, vcfInfoFields);
            setBookmarks(foundBookmarkEntries);

            return new IndexSearchResult(new ArrayList<>(entryMap.values()), maxResultsCount != null &&
                                                                             totalHits > maxResultsCount, totalHits);
        } finally {
            for (SimpleFSDirectory index : indexes) {
                IOUtils.closeQuietly(index);
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
     * Returns a {@code List} of chromosome IDs for a project, specified by ID, where variations exist and satisfy a
     * specified query
     *
     * @param projectId an ID of a project, which index to query
     * @param query     a query string to filter variations
     * @return a {@code List} of chromosome IDs
     * @throws FeatureIndexException if something goes wrong
     */
    public List<Long> getChromosomeIdsWhereVariationsPresentFacet(long projectId, String query)
        throws FeatureIndexException {
        try (Analyzer analyzer = new StandardAnalyzer()) {
            QueryParser queryParser = new QueryParser(FeatureIndexFields.FEATURE_ID.getFieldName(), analyzer);
            return getChromosomeIdsWhereVariationsPresentFacet(projectId, queryParser.parse(query));
        } catch (IOException | ParseException e) {
            throw new FeatureIndexException("Failed to perform facet index search for project " + projectId, e);
        }
    }

    /**
     * Returns a {@code List} of chromosome IDs for a project, specified by ID, where variations exist and satisfy a
     * specified query
     *
     * @param projectId an ID of a project, which index to query
     * @param query     a query to filter variations
     * @return a {@code List} of chromosome IDs
     * @throws IOException
     */
    public List<Long> getChromosomeIdsWhereVariationsPresentFacet(long projectId, Query query) throws IOException {
        List<Long> chromosomeIds = new ArrayList<>();

        try (
            Directory index = fileManager.getIndexForProject(projectId);
            IndexReader reader = DirectoryReader.open(index)
        ) {
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
        }

        return chromosomeIds;
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

        PrefixQuery geneIdPrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE.getFieldName(),
                gene.toLowerCase()));
        PrefixQuery geneNamePrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE_REAL_NAME.getFieldName(),
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
            LOGGER.error(MessageHelper.getMessage(MessagesConstants.ERROR_FEATURE_INDEX_SEARCH_FAILED), e);
            return Collections.emptySet();
        }

        return geneIds;
    }

    public Set<String> searchGenesInVcfFiles(String gene, List<VcfFile> vcfFiles) throws IOException {
        if (CollectionUtils.isEmpty(vcfFiles)) {
            return Collections.emptySet();
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        PrefixQuery geneIdPrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE.getFieldName(),
                                                                 gene.toLowerCase()));
        PrefixQuery geneNamePrefixQuery = new PrefixQuery(new Term(FeatureIndexFields.GENE_REAL_NAME.getFieldName(),
                                                                   gene.toLowerCase()));
        BooleanQuery.Builder geneIdOrNameQuery = new BooleanQuery.Builder();
        geneIdOrNameQuery.add(geneIdPrefixQuery, BooleanClause.Occur.SHOULD);
        geneIdOrNameQuery.add(geneNamePrefixQuery, BooleanClause.Occur.SHOULD);

        builder.add(geneIdOrNameQuery.build(), BooleanClause.Occur.MUST);
        BooleanQuery query =  builder.build();

        Set<String> geneIds;

        SimpleFSDirectory[] indexes = fileManager.getIndexesForFiles(vcfFiles);

        try (MultiReader reader = openMultiReader(indexes)) {
            if (reader.numDocs() == 0) {
                return Collections.emptySet();
            }

            IndexSearcher searcher = new IndexSearcher(reader);
            final TopDocs docs = searcher.search(query, reader.numDocs());
            final ScoreDoc[] hits = docs.scoreDocs;

            geneIds = fetchGeneIds(hits, searcher);
        } catch (IOException e) {
            LOGGER.error(MessageHelper.getMessage(MessagesConstants.ERROR_FEATURE_INDEX_SEARCH_FAILED), e);
            return Collections.emptySet();
        }

        return geneIds;
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

    private VcfIndexEntry createVcfIndexEntry(Document d, List<String> vcfInfoFields) {
        VcfIndexEntry vcfIndexEntry = new VcfIndexEntry();
        vcfIndexEntry.setGene(d.get(FeatureIndexFields.GENE.getFieldName()));
        vcfIndexEntry.setGeneIds(d.get(FeatureIndexFields.GENE_NAMES.getFieldName()));
        vcfIndexEntry.setGeneName(d.get(FeatureIndexFields.GENE_REAL_NAME.getFieldName()));
        vcfIndexEntry.setGeneNames(d.get(FeatureIndexFields.GENE_REAL_NAMES.getFieldName()));

        String isExon = d.get(FeatureIndexFields.IS_EXON.getFieldName());
        vcfIndexEntry.setExon(isExon != null && Boolean.parseBoolean(isExon));

        vcfIndexEntry.setVariationType(VariationType.valueOf(
                d.get(FeatureIndexFields.VARIATION_TYPE.getFieldName()).toUpperCase()));
        vcfIndexEntry.setFailedFilter(d.get(FeatureIndexFields.FAILED_FILTER.getFieldName()));

        IndexableField qualityField = d.getField(FeatureIndexFields.QUALITY.getFieldName());
        if (qualityField != null) {
            vcfIndexEntry.setQuality(qualityField.numericValue().doubleValue());
        }

        if (vcfInfoFields != null) {
            vcfIndexEntry.setInfo(new HashMap<>());
            for (String infoField : vcfInfoFields) {
                vcfIndexEntry.getInfo().put(infoField, d.get(infoField.toLowerCase()));
            }
        }

        return vcfIndexEntry;
    }

    private void addCommonDocumentFields(Document document, FeatureIndexEntry entry, final Long featureFileId) {
        document.add(new StringField(FeatureIndexFields.FEATURE_ID.getFieldName(), entry.getFeatureId() != null ?
                                                           entry.getFeatureId().toLowerCase() : "", Field.Store.YES));

        FieldType fieldType = new FieldType();
        fieldType.setOmitNorms(true);
        fieldType.setIndexOptions(IndexOptions.DOCS);
        fieldType.setStored(true);
        fieldType.setTokenized(false);
        fieldType.setDocValuesType(DocValuesType.SORTED);
        fieldType.freeze();
        Field field = new Field(FeatureIndexFields.CHROMOSOME_ID.getFieldName(), entry.getChromosome() != null ?
                                 new BytesRef(entry.getChromosome().getId().toString()) : new BytesRef(""), fieldType);
        document.add(field);
        document.add(new StoredField(FeatureIndexFields.CHROMOSOME_NAME.getFieldName(),
                                     entry.getChromosome().getName()));

        document.add(new IntPoint(FeatureIndexFields.START_INDEX.getFieldName(), entry.getStartIndex()));
        document.add(new StoredField(FeatureIndexFields.START_INDEX.getFieldName(), entry.getStartIndex()));

        document.add(new IntPoint(FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));
        document.add(new StoredField(FeatureIndexFields.END_INDEX.getFieldName(), entry.getEndIndex()));

        document.add(new StringField(FeatureIndexFields.FEATURE_TYPE.getFieldName(),
                     entry.getFeatureType() != null ? entry.getFeatureType().getFileValue() : "", Field.Store.YES));
        document.add(new StringField(FeatureIndexFields.FILE_ID.getFieldName(), featureFileId.toString(),
                                     Field.Store.YES));

        document.add(new StringField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                         entry.getFeatureName() != null ? entry.getFeatureName().toLowerCase() : "", Field.Store.YES));
        document.add(new SortedDocValuesField(FeatureIndexFields.FEATURE_NAME.getFieldName(),
                                         new BytesRef(entry.getFeatureName() != null ? entry.getFeatureName() : "")));

        document.add(new SortedSetDocValuesFacetField(FeatureIndexFields.CHR_ID.getFieldName(),
                                                      entry.getChromosome().getId().toString()));
    }

    private void  addVcfDocumentFields(Document document, FeatureIndexEntry entry) {
        VcfIndexEntry vcfIndexEntry = (VcfIndexEntry) entry;
        document.add(new StringField(FeatureIndexFields.VARIATION_TYPE.getFieldName(),
                                     vcfIndexEntry.getVariationType().name().toLowerCase(), Field.Store.YES));
        if (StringUtils.isNotBlank(vcfIndexEntry.getFailedFilter())) {
            document.add(new StringField(FeatureIndexFields.FAILED_FILTER.getFieldName(),
                                         vcfIndexEntry.getFailedFilter().toLowerCase(), Field.Store.YES));
        }

        document.add(new FloatPoint(FeatureIndexFields.QUALITY.getFieldName(), vcfIndexEntry.getQuality()
            .floatValue()));
        document.add(new StoredField(FeatureIndexFields.QUALITY.getFieldName(), vcfIndexEntry.getQuality()
            .floatValue()));

        if (StringUtils.isNotBlank(vcfIndexEntry.getGene())) {
            document.add(new StringField(FeatureIndexFields.GENE.getFieldName(), vcfIndexEntry.getGene().toLowerCase(),
                                         Field.Store.YES));
            document.add(new StoredField(FeatureIndexFields.GENE_NAMES.getFieldName(), vcfIndexEntry.getGeneIds()));
        }

        if (StringUtils.isNotBlank(vcfIndexEntry.getGeneName())) {
            document.add(new StringField(FeatureIndexFields.GENE_REAL_NAME.getFieldName(),
                                         vcfIndexEntry.getGeneName().toLowerCase(), Field.Store.YES));
            document.add(new StoredField(FeatureIndexFields.GENE_REAL_NAMES.getFieldName(),
                                         vcfIndexEntry.getGeneNames()));
        }

        document.add(new StringField(FeatureIndexFields.IS_EXON.getFieldName(),
                                     vcfIndexEntry.getExon().toString(), Field.Store.YES));

        if (vcfIndexEntry.getInfo() != null) {
            addVcfFocumentInfoFields(document, vcfIndexEntry);
        }
    }

    private void addVcfFocumentInfoFields(Document document, VcfIndexEntry vcfIndexEntry) {
        for (Map.Entry<String, Object> info : vcfIndexEntry.getInfo().entrySet()) {
            if (viewFieldPattern.matcher(info.getKey()).matches()) { //view fields are for view purposes
                continue;
            }

            String viewKey = "_" + info.getKey() + "_v";
            if (info.getValue() instanceof Integer) {
                document.add(new IntPoint(info.getKey().toLowerCase(), (Integer) info.getValue()));
                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(), vcfIndexEntry.getInfo()
                        .get(viewKey).toString()));
                } else {
                    document.add(new StoredField(info.getKey().toLowerCase(),
                                                 (Integer) info.getValue()));
                }
            } else if (info.getValue() instanceof Float) {
                document.add(new FloatPoint(info.getKey().toLowerCase(), (Float) info.getValue()));

                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(), vcfIndexEntry.getInfo()
                        .get(viewKey).toString()));
                } else {
                    document.add(new StoredField(info.getKey().toLowerCase(), (Float) info.getValue()));
                }
            } else {
                if (vcfIndexEntry.getInfo().containsKey(viewKey)) {
                    document.add(new StoredField(info.getKey().toLowerCase(), vcfIndexEntry.getInfo()
                        .get(viewKey).toString()));
                } else {
                    document.add(new StringField(info.getKey().toLowerCase(), info.getValue().toString()
                        .trim().toLowerCase(), Field.Store.YES));

                }
            }
        }
    }

    private void createIndexEntries(final ScoreDoc[] hits, Map<Integer, FeatureIndexEntry> entryMap,
                                    Map<Long, BookmarkIndexEntry> foundBookmarkEntries, IndexSearcher searcher,
                                    List<String> vcfInfoFields) throws IOException {
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            FeatureType featureType = FeatureType.forValue(d.get(FeatureIndexFields.FEATURE_TYPE.getFieldName()));
            FeatureIndexEntry entry;
            switch (featureType) {
                case VARIATION:
                    entry = createVcfIndexEntry(d, vcfInfoFields);
                    break;
                case BOOKMARK:
                    BookmarkIndexEntry bookmarkEntry = new BookmarkIndexEntry();
                    foundBookmarkEntries.put(Long.parseLong(d.get(FeatureIndexFields.FILE_ID.getFieldName())),
                                             bookmarkEntry);
                    entry = bookmarkEntry;
                    break;
                default:
                    entry = new FeatureIndexEntry();
            }

            entry.setFeatureType(featureType);
            entry.setFeatureId(d.get(FeatureIndexFields.FEATURE_ID.getFieldName()));
            entry.setStartIndex(Integer.parseInt(d.get(FeatureIndexFields.START_INDEX.getFieldName())));
            entry.setEndIndex(Integer.parseInt(d.get(FeatureIndexFields.END_INDEX.getFieldName())));
            entry.setFeatureFileId(Long.parseLong(d.get(FeatureIndexFields.FILE_ID.getFieldName())));
            entry.setFeatureName(d.get(FeatureIndexFields.FEATURE_NAME.getFieldName()));

            String chromosomeId = d.getBinaryValue(FeatureIndexFields.CHROMOSOME_ID.getFieldName()).utf8ToString();
            if (!chromosomeId.isEmpty()) {
                entry.setChromosome(new Chromosome(Long.parseLong(chromosomeId)));
                entry.getChromosome().setName(d.get(FeatureIndexFields.CHROMOSOME_NAME.getFieldName()));
            }

            entryMap.put(Objects.hash(entry.getFeatureFileId(), entry.getChromosome() != null ?
                                                                entry.getChromosome().getId() : null,
                                      entry.getStartIndex(), entry.getEndIndex(), entry.getFeatureId()), entry);

        }
    }

    private Set<String> fetchGeneIds(final ScoreDoc[] hits, IndexSearcher searcher) throws IOException {
        Set<String> geneIds = new HashSet<>();

        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            String geneId = d.get(FeatureIndexFields.GENE.getFieldName());
            String geneName = d.get(FeatureIndexFields.GENE_REAL_NAME.getFieldName());
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
