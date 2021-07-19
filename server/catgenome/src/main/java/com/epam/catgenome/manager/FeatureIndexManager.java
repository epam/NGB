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
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.reader.AbstractGeneReader;
import com.epam.catgenome.manager.parallel.TaskExecutorService;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfFileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.util.ExportFormat;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.catgenome.dao.index.searcher.AbstractIndexSearcher.getIndexSearcher;
import static com.epam.catgenome.util.Utils.NEW_LINE;

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
    private static final List<String> GENE_FIELDS = Arrays.asList("Source", "Score", "Strand", "Frame", "Feature");

    @Autowired
    private FileManager fileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GffManager gffManager;

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
    private BedManager bedManager;

    @Autowired
    private BookmarkManager bookmarkManager;

    @Autowired
    private TaskExecutorService taskExecutorService;

    @Value("#{catgenome['search.features.max.results'] ?: 100}")
    private Integer maxFeatureSearchResultsCount;

    @Value("#{catgenome['search.indexer.buffer.size'] ?: 256}")
    private int indexBufferSize;

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

    private IndexSearchResult<GeneIndexEntry> getGeneSearchResult(final GeneFilterForm filterForm,
                                                                     final List<? extends FeatureFile> featureFiles)
            throws IOException {
        final LuceneIndexSearcher<GeneIndexEntry> indexSearcher =
                getIndexSearcher(filterForm, featureIndexDao, fileManager, taskExecutorService.getSearchExecutor());
        final Sort sort = Optional.ofNullable(
                featureIndexDao.createGeneSorting(filterForm.getOrderBy(), featureFiles))
                .orElseGet(filterForm::defaultSort);
        final IndexSearchResult<GeneIndexEntry> res =
                indexSearcher.getSearchResults(featureFiles, filterForm.computeQuery(), sort);
        res.setTotalPagesCount(filterForm.getPageSize() != null ? (int) Math.ceil(res.getTotalResultsCount()
                    / filterForm.getPageSize().doubleValue()) : 1);
        return res;
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
        return getGeneSearchResult(filterForm, getGeneFilesForReference(referenceId, filterForm.getFileIds()));
    }

    public byte[] exportGenesByReference(final GeneFilterForm filterForm, final long referenceId,
                                         final ExportFormat format, final boolean includeHeader) throws IOException {
        IndexSearchResult<GeneIndexEntry> indexSearchResult = getGeneSearchResult(filterForm,
                getGeneFilesForReference(referenceId, filterForm.getFileIds()));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final List<String> additionalFields = filterForm.getAdditionalFields();
        if (includeHeader) {
            String header = String.join(format.getSeparator(), GENE_FIELDS) +
                    (CollectionUtils.isEmpty(additionalFields) ? "" :
                            format.getSeparator() + String.join(format.getSeparator(), additionalFields)) +
                    NEW_LINE;
            outputStream.write(header.getBytes());
        }

        for (GeneIndexEntry geneIndexEntry: indexSearchResult.getEntries()) {
            List<String> additionalValues = new ArrayList<>();
            if (additionalFields != null) {
                Map<String, String> attributes = geneIndexEntry.getAttributes();
                for (String field: additionalFields) {
                    if (attributes != null) {
                        additionalValues.add(attributes.getOrDefault(field, null));
                    }
                }
            }
            String line = geneIndexEntry.getSource() + format.getSeparator() +
                    geneIndexEntry.getScore() + format.getSeparator() +
                    geneIndexEntry.getStrand() + format.getSeparator() +
                    geneIndexEntry.getFrame() + format.getSeparator() +
                    geneIndexEntry.getFeature() +
                    (CollectionUtils.isEmpty(additionalFields) ? "" :
                            format.getSeparator() + String.join(format.getSeparator(), additionalValues)) + NEW_LINE;
            outputStream.write(line.getBytes());
        }
        return outputStream.toByteArray();
    }

    public GeneFilterInfo getAvailableGeneFieldsToSearch(final Long referenceId, ItemsByProject fileIdsByProjectId) {
        return featureIndexDao.getAvailableFieldsToSearch(
                getGeneFilesForReference(referenceId, Optional.ofNullable(fileIdsByProjectId)
                        .map(ItemsByProject::getFileIds).orElse(Collections.emptyList())));
    }

    public Set<String> getAvailableFieldValues(final Long referenceId, final ItemsByProject fileIdsByProjectId,
                                               final String fieldName) {
        return featureIndexDao.getAvailableFieldValues(
                getGeneFilesForReference(referenceId, Optional.ofNullable(fileIdsByProjectId)
                        .map(ItemsByProject::getFileIds).orElse(Collections.emptyList())), fieldName);
    }

    private List<? extends FeatureFile> getGeneFilesForReference(final long referenceId, final List<Long> fileIds) {
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

    public void buildIndexForFile(FeatureFile featureFile) throws FeatureIndexException, IOException {
        if (!fileManager.indexForFeatureFileExists(featureFile)) {
            switch (featureFile.getFormat()) {
                case BED:
                    bedManager.reindexBedFile(featureFile.getId());
                    break;
                case GENE:
                    // use false here, because it's default parameter for reindexing in GeneController
                    gffManager.reindexGeneFile(featureFile.getId(), false, false);
                    break;
                case VCF:
                    vcfManager.reindexVcfFile(featureFile.getId(), false);
                    break;
                default:
                    throw new IllegalArgumentException("Wrong FeatureType: " + featureFile.getFormat().name());
            }
        }
    }

    /**
     * Fetch gene IDs of genes, affected by variation. The variation is specified by it's start and end indexes
     *
     * @param start a start index of the variation
     * @param end an end index of the variation
     * @param geneFiles a {@code List} of {@code GeneFile} to look for genes
     * @param chromosome a {@code Chromosome}
     * @return a {@code Set} of IDs of genes, affected by the variation
     * @throws GeneReadingException
     */
    public Set<String> fetchGeneIds(int start, int end, List<GeneFile> geneFiles, Chromosome chromosome)
        throws GeneReadingException {
        Set<String> geneIds = new HashSet<>();

        for (GeneFile geneFile : geneFiles) {
            List<Gene> genes = new ArrayList<>();
            Track<Gene> track = new Track<>();
            track.setStartIndex(start);
            track.setEndIndex(end);
            track.setId(geneFile.getId());
            track.setChromosome(chromosome);
            track.setScaleFactor(AbstractGeneReader.LARGE_SCALE_FACTOR_LIMIT);

            if (end > start) {
                track.setStartIndex(start);
                track.setEndIndex(start);
                track = gffManager.loadGenes(track, geneFile, chromosome, false);
                genes.addAll(track.getBlocks());

                track.setStartIndex(end);
                track.setEndIndex(end);
                track = gffManager.loadGenes(track, geneFile, chromosome, false);
                genes.addAll(track.getBlocks());

            } else {
                track.setStartIndex(start);
                track.setEndIndex(end);
                track = gffManager.loadGenes(track, geneFile, chromosome, false);
                genes = track.getBlocks();
            }

            geneIds.addAll(genes.stream()
                    .filter(GeneUtils::isGene)
                    .map(Gene::getGroupId)
                    .collect(Collectors.toList()));
        }

        return geneIds;
    }

    /**
     * Writes index entries for specified {@link FeatureFile} to file system
     * @param featureFile a {@link FeatureFile}, for which index is written
     * @param entries a list of index entries
     * @throws IOException if error occurred while writing to file system
     */
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
                    new BigVcfFeatureIndexBuilder(info, vcfHeader, featureIndexDao,
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
    public void processGeneFile(GeneFile geneFile, final Map<String, Chromosome> chromosomeMap,
            boolean full) throws IOException {
        List<FeatureIndexEntry> allEntries = new ArrayList<>();

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
                .map(biologicalDataItem -> (FeatureFile) biologicalDataItem)
                .collect(Collectors.toList());
        final GeneFile geneFile = reference.getGeneFile();
        if (geneFile != null) {
            final Long geneFileId = geneFile.getId();
            annotationFiles.add(geneFileManager.load(geneFileId));
        }
        return annotationFiles;
    }

    private FeatureFile getGeneFile(final long referenceId) {
        return referenceGenomeManager.load(referenceId).getGeneFile();
    }

    private void addFeaturesFromUsualGeneFileToIndex(GeneFile geneFile, Map<String, Chromosome> chromosomeMap,
                                                     List<FeatureIndexEntry> allEntries) throws IOException {
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

    private void addFeaturesFromIteratorToIndex(CloseableIterator<GeneFeature> iterator, Map<String, Chromosome>
            chromosomeMap, GeneFile geneFile, List<FeatureIndexEntry> allEntries,
                                                boolean transcriptIterator) throws IOException {
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

    private String checkNextChromosome(Feature feature, String currentChromosomeName,
                                       Map<String, Chromosome> chromosomeMap, List<FeatureIndexEntry> allEntries,
                                       GeneFile geneFile) throws IOException {
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


    public void addGeneFeatureToIndex(List<FeatureIndexEntry> allEntries, GeneFeature feature,
                                     Map<String, Chromosome> chromosomeMap) {
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
}
