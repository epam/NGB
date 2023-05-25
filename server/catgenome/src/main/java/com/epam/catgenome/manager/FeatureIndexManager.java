/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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

package com.epam.catgenome.manager;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.ItemsByProject;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.indexer.BigVcfFeatureIndexBuilder;
import com.epam.catgenome.dao.index.searcher.LuceneIndexSearcher;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.gene.GeneFilterInfo;
import com.epam.catgenome.entity.gene.GeneHighLevel;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.manager.gene.reader.AbstractGeneReader;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfFileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.util.NggbIntervalTreeMap;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.catgenome.dao.index.searcher.AbstractIndexSearcher.getIndexSearcher;

/**
 * Source:      VcfIndexManager
 * Created:     28.04.16, 18:01
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A service class, that contains logic, connected with feature indexes. They are used for fast feature
 * search (variations, genes) by various criteria.
 * </p>
 */
@Service
public class FeatureIndexManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureIndexManager.class);
    private static final String VCF_FILE_IDS_FIELD = "vcfFileIds";

    @Autowired
    private FileManager fileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfFileManager vcfFileManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private FeatureIndexDao featureIndexDao;

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private BookmarkManager bookmarkManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    @Value("#{catgenome['search.features.max.results'] ?: 100}")
    private Integer maxFeatureSearchResultsCount;

    @Value("#{catgenome['search.indexer.buffer.size'] ?: 256}")
    private int indexBufferSize;

    @Value("#{catgenome['search.features.internal.max.results'] ?: 1000}")
    private int maxFeatureInternalSearchResultsCount;

    /**
     * Deletes features from specified feature files from project's index
     *
     * @param projectId a project to delete index entries
     * @param fileIds files, which entries to delete
     */
    public void deleteFromIndexByFileId(final long projectId, List<Pair<FeatureType, Long>> fileIds) {
        featureIndexDao.deleteFromIndexByFileId(projectId, fileIds);
    }

    /**
     * Searches gene IDs, affected by variations in specified VCF files in a specified project
     *
     * @param gene a prefix of a gene ID to search
     * @param vcfFileIds a {@code List} of IDs of VCF files in project to search for gene IDs
     * @return a {@code Set} of gene IDs, that are affected by some variations in specified VCf files
     * @throws IOException
     */
    public Set<String> searchGenesInVcfFilesInProject(long projectId, String gene, List<Long> vcfFileIds)
            throws IOException {
        Project project = projectManager.load(projectId);
        List<VcfFile> vcfFiles = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());

        List<VcfFile> selectedVcfFiles;
        if (CollectionUtils.isEmpty(vcfFileIds)) {
            selectedVcfFiles = vcfFiles;
        } else {
            selectedVcfFiles = vcfFiles.stream().filter(f -> vcfFileIds.contains(f.getId()))
                .collect(Collectors.toList());
        }

        return featureIndexDao.searchGenesInVcfFiles(gene, selectedVcfFiles);
    }

    /**
     * Searches gene IDs, affected by variations in specified VCF files in a specified project
     *
     * @param gene a prefix of a gene ID to search
     * @param vcfFileIds a {@code List} of IDs of VCF files in project to search for gene IDs
     * @return a {@code Set} of gene IDs, that are affected by some variations in specified VCf fileFs
     * @throws IOException if something goes wrong with file system
     */
    public Set<String> searchGenesInVcfFiles(String gene, List<Long> vcfFileIds) throws IOException {
        List<VcfFile> vcfFiles = vcfFileManager.loadVcfFiles(vcfFileIds);
        return featureIndexDao.searchGenesInVcfFiles(gene, vcfFiles);
    }

    /**
     * Filer chromosomes that contain variations, specified by filter
     *
     * @param filterForm a {@code VcfFilterForm} to filter out chromosomes
     * @param projectId a {@code Project}s ID to filter
     * @return a {@code List} of {@code Chromosome} that corresponds to specified filter
     * @throws IOException
     */
    public List<Chromosome> filterChromosomes(VcfFilterForm filterForm, long projectId) throws IOException {
        Assert.isTrue(filterForm.getVcfFileIds() != null && !filterForm.getVcfFileIds().isEmpty(), MessageHelper
                .getMessage(MessagesConstants.ERROR_NULL_PARAM, VCF_FILE_IDS_FIELD));
        Project project = projectManager.load(projectId);
        List<VcfFile> vcfFiles = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());

        List<Chromosome> chromosomes = referenceGenomeManager.loadChromosomes(vcfFiles.get(0).getReferenceId());
        Map<Long, Chromosome> chromosomeMap = chromosomes.parallelStream()
                .collect(Collectors.toMap(BaseEntity::getId, chromosome -> chromosome));

        List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(vcfFiles, filterForm
                .computeQuery(FeatureType.VARIATION));

        return chromosomeIds.stream().map(chromosomeMap::get).collect(Collectors.toList());
    }

    /**
     * Filer chromosomes that contain variations, specified by filter
     *
     * @param filterForm a {@code VcfFilterForm} to filter out chromosomes
     * @return a {@code List} of {@code Chromosome} that corresponds to specified filter
     * @throws IOException
     */
    public List<Chromosome> filterChromosomes(VcfFilterForm filterForm) throws IOException {
        Assert.isTrue(filterForm.getVcfFileIds() != null && !filterForm.getVcfFileIds().isEmpty(), MessageHelper
            .getMessage(MessagesConstants.ERROR_NULL_PARAM, VCF_FILE_IDS_FIELD));
        List<VcfFile> vcfFiles = vcfFileManager.loadVcfFiles(filterForm.getVcfFileIds());

        List<Chromosome> chromosomes = referenceGenomeManager.loadChromosomes(vcfFiles.get(0).getReferenceId());
        Map<Long, Chromosome> chromosomeMap = chromosomes.parallelStream()
            .collect(Collectors.toMap(BaseEntity::getId, chromosome -> chromosome));

        List<Long> chromosomeIds = featureIndexDao.getChromosomeIdsWhereVariationsPresentFacet(vcfFiles, filterForm
                                                                                .computeQuery(FeatureType.VARIATION));

        return chromosomeIds.stream().map(chromosomeMap::get).collect(Collectors.toList());
    }

    /**
     * Filter variations in a feature index for a project, specified by ID
     *
     * @param filterForm {@code VcfFilterForm}, setting filter options
     * @param projectId  an ID of a project, which index to work with
     * @return a {@code List} of {@code VcfIndexEntry}, representing variations that satisfy the filter
     * @throws IOException
     */
    public IndexSearchResult<VcfIndexEntry> filterVariations(VcfFilterForm filterForm, long projectId)
        throws IOException {
        Project project = projectManager.load(projectId);
        List<VcfFile> files = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());
        return getVcfSearchResult(filterForm, files);
    }

    public int getTotalPagesCount(VcfFilterForm filterForm) throws IOException {
        if (filterForm.getPageSize() == null) {
            throw new IllegalArgumentException("No page size is specified");
        }

        List<VcfFile> files = vcfFileManager.loadVcfFiles(filterForm.getVcfFileIds());
        int totalCount = featureIndexDao.getTotalVariationsCountFacet(files, filterForm.computeQuery(
            FeatureType.VARIATION));
        return (int) Math.ceil(totalCount / filterForm.getPageSize().doubleValue());
    }

    public int getTotalPagesCount(VcfFilterForm filterForm, long projectId) throws IOException {
        if (filterForm.getPageSize() == null) {
            throw new IllegalArgumentException("No page size is specified");
        }

        Project project = projectManager.load(projectId);
        List<VcfFile> files = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());
        int totalCount = featureIndexDao.getTotalVariationsCountFacet(files, filterForm.computeQuery(
            FeatureType.VARIATION));
        return (int) Math.ceil(totalCount / filterForm.getPageSize().doubleValue());
    }

    /**
     * Groups variations from specified {@link List} of {@link VcfFile}s by specified field
     * @param filterForm {@code VcfFilterForm}, setting filter options
     * @param projectId a {@code Project}s ID to filter
     * @param groupByField a field to perform grouping
     * @return a {@link List} of {@link Pair}s, mapping field value to number of variations, having this value
     * @throws IOException if something goes wrong with the file system
     */
    public List<Group> groupVariations(VcfFilterForm filterForm, long projectId, String groupByField)
        throws IOException {
        Project project = projectManager.load(projectId);
        List<VcfFile> files = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());
        return featureIndexDao.groupVariations(files, filterForm.computeQuery(FeatureType.VARIATION), groupByField);
    }

    /**
     * Groups variations from specified {@link List} of {@link VcfFile}s by specified field
     * @param filterForm {@code VcfFilterForm}, setting filter options
     * @param groupByField a field to perform grouping
     * @return a {@link List} of {@link Pair}s, mapping field value to number of variations, having this value
     * @throws IOException if something goes wrong with the file system
     */
    public List<Group> groupVariations(VcfFilterForm filterForm, String groupByField)
        throws IOException {
        List<VcfFile> files = vcfFileManager.loadVcfFiles(filterForm.getVcfFileIds());
        return featureIndexDao.groupVariations(files, filterForm.computeQuery(FeatureType.VARIATION), groupByField);
    }

    /**
     * Filter variations in a feature index for a project, specified by ID
     *
     * @param filterForm {@code VcfFilterForm}, setting filter options
     * @return a {@code List} of {@code VcfIndexEntry}, representing variations that satisfy the filter
     * @throws IOException
     */
    public IndexSearchResult<VcfIndexEntry> filterVariations(VcfFilterForm filterForm) throws IOException {
        List<VcfFile> files = vcfFileManager.loadVcfFiles(filterForm.getVcfFileIds());
        return getVcfSearchResult(filterForm, files);
    }

    /**
     * Loads Gene feature content by 'uid' Lucene document field
     *
     * @param fileId {@link GeneFile} id
     * @param uid Lucene document field
     * @return gene content
     */
    public GeneHighLevel loadGeneFeatureByUid(final Long fileId, final String uid) {
        try {
            final GeneIndexEntry entry = featureIndexDao.searchGeneFeatureByUid(
                    geneFileManager.load(fileId), uid);
            if (Objects.isNull(entry)) {
                return null;
            }
            final GeneHighLevel geneHighLevel = new GeneHighLevel();
            geneHighLevel.setStartIndex(entry.getStartIndex());
            geneHighLevel.setEndIndex(entry.getEndIndex());
            geneHighLevel.setFrame(entry.getFrame());
            geneHighLevel.setSource(entry.getSource());
            geneHighLevel.setScore(entry.getScore());
            geneHighLevel.setStrand(StrandSerializable.forValue(entry.getStrand()));
            geneHighLevel.setFeature(entry.getFeature());
            geneHighLevel.setAttributes(entry.getAttributes());
            return geneHighLevel;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Updates Gene feature content by 'uid' Lucene document field
     *
     * @param fileId {@link GeneFile} id
     * @param uid Lucene document field
     * @param geneContent a new gene content
     * @return gene content
     */
    public GeneHighLevel updateGeneFeatureByUid(final Long fileId, final String uid, final GeneHighLevel geneContent) {
        try {
            final GeneFile geneFile = geneFileManager.load(fileId);
            final Gene.Origin geneFileType = AbstractGeneReader.getOrigin(geneFile);
            return featureIndexDao.updateGeneFeatureByUid(geneFile, uid, geneContent, geneFileType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private IndexSearchResult<VcfIndexEntry> getVcfSearchResult(final VcfFilterForm filterForm,
            final List<VcfFile> vcfFiles) throws IOException {
        if (filterForm.getPage() != null && filterForm.getPageSize() != null) {
            final LuceneIndexSearcher<VcfIndexEntry> indexSearcher =
                    getIndexSearcher(filterForm, featureIndexDao, fileManager, taskExecutorService.getSearchExecutor());
            final Sort sort = featureIndexDao.createVcfSorting(filterForm.getOrderBy(), vcfFiles);
            final IndexSearchResult<VcfIndexEntry> res =
                    indexSearcher.getSearchResults(vcfFiles, filterForm.computeQuery(FeatureType.VARIATION), sort);
            res.setTotalPagesCount((int) Math.ceil(res.getTotalResultsCount()
                    / filterForm.getPageSize().doubleValue()));
            return res;
        } else {
            final IndexSearchResult<VcfIndexEntry> res = featureIndexDao.searchFileIndexes(vcfFiles,
                               filterForm.computeQuery(FeatureType.VARIATION), filterForm.getAdditionalFields(),
                                                                           null, null);
            res.setExceedsLimit(false);
            return res;
        }
    }

    public IndexSearchResult<GeneIndexEntry> getGeneSearchResult(final GeneFilterForm filterForm,
                                                                     final List<? extends FeatureFile> featureFiles)
            throws IOException {
        final LuceneIndexSearcher<GeneIndexEntry> indexSearcher =
                getIndexSearcher(filterForm, featureIndexDao, fileManager, taskExecutorService.getSearchExecutor());
        final Sort sort = Optional.ofNullable(
                featureIndexDao.createGeneSorting(filterForm.getOrderBy(), featureFiles))
                .orElseGet(filterForm::defaultSort);
        final IndexSearchResult<GeneIndexEntry> res =
                indexSearcher.getSearchResults(featureFiles, filterForm.computeQuery(), sort);
        res.setTotalPagesCount((int) Math.ceil(res.getTotalResultsCount()
                / filterForm.getPageSize().doubleValue()));
        return res;
    }

    public IndexSearchResult<GeneIndexEntry> getFullGeneSearchResult(final GeneFilterForm filterForm,
                                                                     final GeneFile geneFile) {
        try {
            filterForm.setPageSize(maxFeatureInternalSearchResultsCount);
            final Sort sort = Optional.ofNullable(
                    featureIndexDao.createGeneSorting(filterForm.getOrderBy(), Collections.singletonList(geneFile)))
                    .orElseGet(filterForm::defaultSort);
            final Long chrId = ListUtils.emptyIfNull(filterForm.getChromosomeIds()).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Chromosome ID must be specified"));
            final IndexSearchResult<GeneIndexEntry> result = featureIndexDao
                    .searchGeneFeaturesFully(geneFile, chrId.toString(), filterForm, sort);
            result.setTotalPagesCount((int) Math.ceil(result.getTotalResultsCount()
                    / filterForm.getPageSize().doubleValue()));
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int countGenesInInterval(final GeneFilterForm filterForm, final GeneFile geneFile) {
        final Long chrId = ListUtils.emptyIfNull(filterForm.getChromosomeIds()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chromosome ID must be specified"));
        try {
            return featureIndexDao.countGenesInInterval(geneFile, chrId.toString(), filterForm);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Searches genes by it's ID in project's gene files. Minimum featureId prefix length == 2
     *
     * @param featureId a feature ID prefix to search for
     * @return a {@code List} of {@code FeatureIndexEntry}
     * @throws IOException if something goes wrong with the file system
     */
    public IndexSearchResult<FeatureIndexEntry> searchFeaturesInProject(final String featureId,
                                                                        final long projectId) throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        final IndexSearchResult<FeatureIndexEntry> bookmarkSearchRes = bookmarkManager.searchBookmarks(featureId,
                                                                                         maxFeatureSearchResultsCount);
        final Project project = projectManager.load(projectId);
        final Optional<Reference> opt = project.getItems().stream()
                .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE)
                .map(i -> (Reference) i.getBioDataItem())
                .findFirst();
        if (opt.isPresent() && opt.get().getGeneFile() != null) {
            final IndexSearchResult<FeatureIndexEntry> res = featureIndexDao.searchFeatures(featureId,
                    geneFileManager.load(opt.get().getGeneFile().getId()),
                    maxFeatureSearchResultsCount);
            bookmarkSearchRes.mergeFrom(res);
            return bookmarkSearchRes;
        }
        return bookmarkSearchRes;
    }

    public IndexSearchResult<GeneIndexEntry> searchGenesByReference(final GeneFilterForm filterForm,
                                                                    final long referenceId) throws IOException {
        final List<? extends FeatureFile> files = getGeneFilesForReference(referenceId, filterForm.getFileIds());
        if (CollectionUtils.isEmpty(files)) {
            return new IndexSearchResult<>();
        }
        return getGeneSearchResult(filterForm, files);
    }

    public GeneFilterInfo getAvailableGeneFieldsToSearch(final Long referenceId,
                                                         final ItemsByProject fileIdsByProjectId) throws IOException {
        final List<? extends FeatureFile> files =
                getGeneFilesForReference(referenceId, Optional.ofNullable(fileIdsByProjectId)
                .map(ItemsByProject::getFileIds).orElse(Collections.emptyList()));
        if (CollectionUtils.isEmpty(files)) {
            return GeneFilterInfo.builder().build();
        }
        return featureIndexDao.getAvailableFieldsToSearch(files);
    }

    public Set<String> getAvailableFieldValues(final Long referenceId,
                                               final ItemsByProject fileIdsByProjectId,
                                               final String fieldName) throws IOException {
        final List<? extends FeatureFile> files = getGeneFilesForReference(referenceId,
                Optional.ofNullable(fileIdsByProjectId)
                        .map(ItemsByProject::getFileIds).orElse(Collections.emptyList()));
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptySet();
        }
        return featureIndexDao.getAvailableFieldValues(files, fieldName);
    }

    public List<? extends FeatureFile> getGeneFilesForReference(final long referenceId, final List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Stream.concat(
                    Optional.ofNullable(getGeneFile(referenceId))
                            .map(Collections::singletonList).orElse(Collections.emptyList()).stream(),
                    getFeatureFiles(referenceId).stream()
            ).filter(featureFile -> featureFile.getFormat() == BiologicalDataItemFormat.GENE)
                    .distinct().collect(Collectors.toList());
        } else {
            return geneFileManager.loadFiles(fileIds);
        }
    }

    public IndexSearchResult<FeatureIndexEntry> searchFeaturesByReference(final String featureId,
                                                                          final long referenceId) throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        final IndexSearchResult<FeatureIndexEntry> res = featureIndexDao.searchFeatures(
                featureId, getFeatureFiles(referenceId), maxFeatureSearchResultsCount
        );

        return mergeWithBookmarkSearch(res, featureId);
    }

    public IndexSearchResult<FeatureIndexEntry> searchFeatures(final String featureId) throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }
        final IndexSearchResult<FeatureIndexEntry> res = featureIndexDao.searchFeatures(
                featureId, getFeatureFiles(), maxFeatureSearchResultsCount
        );
        return mergeWithBookmarkSearch(res, featureId);
    }

    /**
     * Loads {@code VcfFilterInfo} object for a specified project. {@code VcfFilterInfo} contains information about
     * available fields to perform filtering and display results
     *
     * @param projectId a {@code Project}'s ID to load filter info
     * @return a {@code VcfFilterInfo} object
     * @throws IOException
     */
    public VcfFilterInfo loadVcfFilterInfoForProject(long projectId) throws IOException {
        Project project = projectManager.load(projectId);
        List<Long> vcfIds = project.getItems().stream()
                .filter(item -> item.getBioDataItem() != null
                        && item.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
                .map(item -> (item.getBioDataItem()).getId())
                .collect(Collectors.toList());

        return vcfManager.getFiltersInfo(vcfIds);
    }

    /**
     * Writes index entries for specified {@link FeatureFile} to file system
     * @param featureFile a {@link FeatureFile}, for which index is written
     * @param entries a list of index entries
     * @throws IOException if error occurred while writing to file system
     */
    public void writeLuceneIndexForFile(final FeatureFile featureFile,
                                        final List<? extends FeatureIndexEntry> entries,
                                        VcfFilterInfo vcfFilterInfo, IndexWriter writer)
            throws IOException {
        featureIndexDao.writeLuceneIndexForFile(featureFile, entries, vcfFilterInfo, writer);
    }

    public void writeLuceneIndexForFile(final FeatureFile featureFile,
                                        final List<? extends FeatureIndexEntry> entries,
                                        VcfFilterInfo vcfFilterInfo)
            throws IOException {
        featureIndexDao.writeLuceneIndexForFile(featureFile, entries, vcfFilterInfo);
    }

    public void makeIndexForBedReader(BedFile bedFile, AbstractFeatureReader<NggbBedFeature, LineIterator> reader,
                                      Map<String, Chromosome> chromosomeMap) throws IOException {
        CloseableIterator<NggbBedFeature> iterator = reader.iterator();
        List<FeatureIndexEntry> allEntries = new ArrayList<>();
        while (iterator.hasNext()) {
            NggbBedFeature next = iterator.next();
            FeatureIndexEntry entry = new FeatureIndexEntry();
            entry.setFeatureFileId(bedFile.getId());
            entry.setChromosome(Utils.getFromChromosomeMap(chromosomeMap, next.getContig()));
            entry.setStartIndex(next.getStart());
            entry.setEndIndex(next.getEnd());
            entry.setFeatureId(next.getName());
            entry.setFeatureName(next.getName());
            entry.setUuid(UUID.randomUUID());
            entry.setFeatureType(FeatureType.BED_FEATURE);
            allEntries.add(entry);
        }
        featureIndexDao.writeLuceneIndexForFile(bedFile, allEntries, null);
    }

    /**
     * Creates a VCF file feature index
     * @param vcfFile a VCF file to create index
     * @param reader a reader to file
     * @param geneFiles a {@code List} of {@code GeneFile} to look for genes
     * @param chromosomeMap a Map of {@link Chromosome}s to chromosome names
     * @param info VCF file's info data
     * @throws FeatureIndexException if an error occured while building an index
     */
    public void makeIndexForVcfReader(VcfFile vcfFile, FeatureReader<VariantContext> reader, List<GeneFile> geneFiles,
                                       Map<String, Chromosome> chromosomeMap, VcfFilterInfo info)
        throws FeatureIndexException {

        VCFHeader vcfHeader = (VCFHeader) reader.getHeader();

        try {
            CloseableIterator<VariantContext> iterator = reader.iterator();
            String currentKey = null;
            VariantContext variantContext = null;
            BigVcfFeatureIndexBuilder indexer =
                    new BigVcfFeatureIndexBuilder(info, vcfHeader, this,
                            vcfFile, fileManager, geneFiles, indexBufferSize);

            while (iterator.hasNext()) {
                variantContext = iterator.next();

                if (!variantContext.getContig().equals(currentKey)) {
                    indexer.clear();
                    currentKey = variantContext.getContig();
                    LOGGER.info(MessageHelper
                            .getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE,
                                    currentKey));
                }
                indexer.add(variantContext, chromosomeMap);
            }
            // Put the last one
            if (variantContext != null && currentKey != null && Utils
                    .chromosomeMapContains(chromosomeMap, currentKey)) {
                LOGGER.info(MessageHelper
                        .getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE,
                                currentKey));
                indexer.clear();
            }
            indexer.close();
        } catch (IOException e) {
            throw new FeatureIndexException(vcfFile, e);
        }
    }

    /**
     * Creates Gene file's feature index
     * @param geneFile a {@link GeneFile} to create index
     * @param chromosomeMap a Map of {@link Chromosome}s to chromosome names
     * @param full
     * @throws IOException if an error occurred while writing index
     */
    public void processGeneFile(final GeneFile geneFile, final Map<String, Chromosome> chromosomeMap,
                                final boolean full) throws IOException {
        final List<FeatureIndexEntry> allEntries = new ArrayList<>();

        LOGGER.info("Writing feature index for file {}:{}", geneFile.getId(), geneFile.getName());

        if (!full && fileManager.checkGeneFileExists(geneFile, GeneFileType.TRANSCRIPT)) {
            try (AbstractFeatureReader<GeneFeature, LineIterator> largeScaleReader = fileManager.makeGeneReader(
                geneFile, GeneFileType.LARGE_SCALE);
                 AbstractFeatureReader<GeneFeature, LineIterator> transcriptReader = fileManager.makeGeneReader(
                     geneFile, GeneFileType.TRANSCRIPT)) {
                CloseableIterator<GeneFeature> largeScaleIterator = largeScaleReader.iterator();
                CloseableIterator<GeneFeature> transcriptIterator = transcriptReader.iterator();

                addFeaturesFromIteratorToIndex(largeScaleIterator, chromosomeMap, geneFile, allEntries, false);
                addFeaturesFromIteratorToIndex(transcriptIterator, chromosomeMap, geneFile, allEntries, true);
            }
        } else {
            addFeaturesFromUsualGeneFileToIndex(geneFile, chromosomeMap, allEntries);
        }
    }

    public NggbIntervalTreeMap<List<Gene>> loadGenesIntervalMap(final List<GeneFile> geneFiles, final int start,
                                                                final int end, final Chromosome chromosome) {
        final NggbIntervalTreeMap<List<Gene>> genesRangeMap = new NggbIntervalTreeMap<>();
        try {
            IndexSearchResult<FeatureIndexEntry> searchResult = featureIndexDao.searchFeaturesInInterval(geneFiles,
                    start, end, chromosome);
            searchResult.getEntries().stream()
                    .filter(f -> f.getFeatureType() == FeatureType.EXON || f.getFeatureType() == FeatureType.GENE)
                    .map(f -> {
                        Gene gene = new Gene();
                        gene.setFeature(f.getFeatureType().name());
                        gene.setStartIndex(f.getStartIndex());
                        gene.setEndIndex(f.getEndIndex());
                        gene.setGroupId(f.getFeatureId());
                        gene.setFeatureName(f.getFeatureName().toUpperCase());
                        return gene;
                    })
                    .forEach(g -> {
                        Interval interval = new Interval(chromosome.getName(), g.getStartIndex(), g.getEndIndex());
                        genesRangeMap.putIfAbsent(interval, new ArrayList<>());
                        genesRangeMap.get(interval).add(g);
                    });
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        genesRangeMap.setMaxEndIndex(start);
        genesRangeMap.setMinStartIndex(end);
        return genesRangeMap;
    }

    public void addGeneFeatureToIndex(final List<FeatureIndexEntry> allEntries, final GeneFeature feature,
                                      final Map<String, Chromosome> chromosomeMap) {
        if (chromosomeMap.containsKey(feature.getContig())
                || chromosomeMap.containsKey(Utils.changeChromosomeName(feature.getContig()))) {
            GeneIndexEntry masterEntry = new GeneIndexEntry();
            masterEntry.setFeatureId(feature.getFeatureId());
            masterEntry.setUuid(UUID.randomUUID());
            masterEntry.setChromosome(chromosomeMap.containsKey(feature.getContig()) ?
                    chromosomeMap.get(feature.getContig()) :
                    chromosomeMap.get(Utils.changeChromosomeName(feature.getContig())));
            masterEntry.setStartIndex(feature.getStart());
            masterEntry.setEndIndex(feature.getEnd());
            masterEntry.setFeatureType(GeneUtils.fetchType(feature));
            masterEntry.setFeature(feature.getFeature());

            masterEntry.setSource(feature.getSource());
            masterEntry.setScore(feature.getScore());
            masterEntry.setFrame(feature.getFrame());
            Optional.ofNullable(feature.getStrand()).ifPresent(strand -> masterEntry.setStrand(strand.toValue()));
            masterEntry.setAttributes(feature.getAttributes());

            allEntries.add(masterEntry);

            String featureName = feature.getFeatureName();
            masterEntry.setFeatureName(featureName);
        }
    }

    private IndexSearchResult<FeatureIndexEntry> mergeWithBookmarkSearch(final IndexSearchResult<FeatureIndexEntry> res,
                                                                         final String featureId) {
        final IndexSearchResult<FeatureIndexEntry> bookmarkSearchRes = bookmarkManager.searchBookmarks(featureId,
                maxFeatureSearchResultsCount);
        bookmarkSearchRes.mergeFrom(res);
        return bookmarkSearchRes;
    }

    private List<FeatureFile> getFeatureFiles(final long referenceId) {
        final Reference reference = referenceGenomeManager.load(referenceId);

        final List<FeatureFile> annotationFiles = referenceGenomeManager.getReferenceAnnotationFiles(referenceId)
                .stream()
                .filter(biologicalDataItem -> biologicalDataItem instanceof FeatureFile)
                .map(biologicalDataItem -> (FeatureFile) biologicalDataItem)
                .collect(Collectors.toList());
        final GeneFile geneFile = reference.getGeneFile();
        if (geneFile != null) {
            final Long geneFileId = geneFile.getId();
            annotationFiles.add(geneFileManager.load(geneFileId));
        }
        return annotationFiles;
    }

    private List<FeatureFile> getFeatureFiles() {
        final List<FeatureFile> annotationFiles = referenceGenomeManager.getReferenceAnnotationFiles()
                .stream()
                .filter(biologicalDataItem -> biologicalDataItem instanceof FeatureFile)
                .map(biologicalDataItem -> (FeatureFile) biologicalDataItem)
                .collect(Collectors.toList());
        annotationFiles.addAll(geneFileManager.loadFiles());
        return annotationFiles;
    }

    private FeatureFile getGeneFile(final long referenceId) {
        return referenceGenomeManager.load(referenceId).getGeneFile();
    }

    private void addFeaturesFromUsualGeneFileToIndex(final GeneFile geneFile,
                                                     final Map<String, Chromosome> chromosomeMap,
                                                     final List<FeatureIndexEntry> allEntries) throws IOException {
        try (AbstractFeatureReader<GeneFeature, LineIterator> usualReader = fileManager.makeGeneReader(
                geneFile, GeneFileType.ORIGINAL)) {
            CloseableIterator<GeneFeature> iterator = usualReader.iterator();
            GeneFeature feature = null;
            String currentKey = null;

            while (iterator.hasNext()) {
                feature = iterator.next();
                currentKey = checkNextChromosome(feature, currentKey, chromosomeMap, allEntries, geneFile);

                if (GeneUtils.isGene(feature) || GeneUtils.isTranscript(feature) || GeneUtils.isExon(feature)) {
                    addGeneFeatureToIndex(allEntries, feature, chromosomeMap);
                }
            }

            // Put the last one
            if (feature != null && currentKey != null && (chromosomeMap.containsKey(currentKey)
                    || chromosomeMap.containsKey(Utils.changeChromosomeName(currentKey)))) {
                featureIndexDao.writeLuceneIndexForFile(geneFile, allEntries, null);
                allEntries.clear();
            }
        }
    }

    private void addFeaturesFromIteratorToIndex(final CloseableIterator<GeneFeature> iterator,
                                                final Map<String, Chromosome> chromosomeMap,
                                                final GeneFile geneFile,
                                                final List<FeatureIndexEntry> allEntries,
                                                final boolean transcriptIterator) throws IOException {
        GeneFeature feature = null;
        String currentKey = null;

        while (iterator.hasNext()) {
            feature = iterator.next();
            currentKey = checkNextChromosome(feature, currentKey, chromosomeMap, allEntries, geneFile);

            if (transcriptIterator || GeneUtils.isGene(feature) || GeneUtils.isExon(feature)) {
                addGeneFeatureToIndex(allEntries, feature, chromosomeMap);
            }
        }

        // Put the last one
        if (feature != null && currentKey != null && (chromosomeMap.containsKey(currentKey)
                || chromosomeMap.containsKey(Utils.changeChromosomeName(currentKey)))) {
            featureIndexDao.writeLuceneIndexForFile(geneFile, allEntries, null);
            allEntries.clear();
        }
    }

    private String checkNextChromosome(final Feature feature, final String currentChromosomeName,
                                       final Map<String, Chromosome> chromosomeMap,
                                       final List<FeatureIndexEntry> allEntries,
                                       final GeneFile geneFile) throws IOException {
        if (!feature.getContig().equals(currentChromosomeName)) {
            if (currentChromosomeName != null && (chromosomeMap.containsKey(currentChromosomeName) ||
                                      chromosomeMap.containsKey(Utils.changeChromosomeName(currentChromosomeName)))) {
                featureIndexDao.writeLuceneIndexForFile(geneFile, allEntries, null);
                LOGGER.info(MessageHelper.getMessage(
                    MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentChromosomeName));
                allEntries.clear();
            }
            return feature.getContig();
        }
        return currentChromosomeName;
    }
}
