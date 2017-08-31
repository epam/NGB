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

package com.epam.catgenome.manager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.indexer.BigVcfFeatureIndexBuilder;
import com.epam.catgenome.dao.index.indexer.VcfFeatureIndexBuilder;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.index.*;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.reader.AbstractGeneReader;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.BookmarkManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfFileManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
    private BookmarkManager bookmarkManager;

    @Value("#{catgenome['search.features.max.results'] ?: 100}")
    private Integer maxFeatureSearchResultsCount;

    @Value("#{catgenome['files.vcf.max.entries.in.memory'] ?: 3000000}")
    private int maxVcfIndexEntriesInMemory;

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
        Project project = projectManager.loadProject(projectId);
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
     * @return a {@code Set} of gene IDs, that are affected by some variations in specified VCf files
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
        Project project = projectManager.loadProject(projectId);
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
        Project project = projectManager.loadProject(projectId);
        List<VcfFile> files = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());
        if (filterForm.getPage() != null && filterForm.getPageSize() != null) {
            IndexSearchResult<VcfIndexEntry> res = featureIndexDao.searchFileIndexesPaging(files,
                                                     filterForm.computeQuery(FeatureType.VARIATION),
                                                     filterForm.getInfoFields(), filterForm.getPage(),
                                                     filterForm.getPageSize(), filterForm.getOrderBy());
            res.setTotalPagesCount((int) Math.ceil(res.getTotalResultsCount() /
                    filterForm.getPageSize().doubleValue()));
            return res;
        } else {
            IndexSearchResult<VcfIndexEntry> res = featureIndexDao.searchFileIndexes(files, filterForm.computeQuery(
                FeatureType.VARIATION), filterForm.getInfoFields(), null, null);
            res.setExceedsLimit(false);
            return res;
        }
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

        Project project = projectManager.loadProject(projectId);
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
        Project project = projectManager.loadProject(projectId);
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
        if (filterForm.getPage() != null && filterForm.getPageSize() != null) {
            IndexSearchResult<VcfIndexEntry> res = featureIndexDao.searchFileIndexesPaging(files,
                                                                   filterForm.computeQuery(FeatureType.VARIATION),
                                                                   filterForm.getInfoFields(), filterForm.getPage(),
                                                                   filterForm.getPageSize(), filterForm.getOrderBy());
            res.setTotalPagesCount((int) Math.ceil(res.getTotalResultsCount()
                    / filterForm.getPageSize().doubleValue()));
            return res;
        } else {
            IndexSearchResult<VcfIndexEntry> res = featureIndexDao.searchFileIndexes(files,
                               filterForm.computeQuery(FeatureType.VARIATION), filterForm.getInfoFields(),
                                                                           null, null);
            res.setExceedsLimit(false);
            return res;
        }
    }

    /**
     * Searches genes by it's ID in project's gene files. Minimum featureId prefix length == 2
     *
     * @param featureId a feature ID prefix to search for
     * @return a {@code List} of {@code FeatureIndexEntry}
     * @throws IOException if something goes wrong with the file system
     */
    public IndexSearchResult searchFeaturesInProject(String featureId, long projectId) throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult(Collections.emptyList(), false, 0);
        }

        IndexSearchResult<FeatureIndexEntry> bookmarkSearchRes = bookmarkManager.searchBookmarks(featureId,
                                                                                         maxFeatureSearchResultsCount);

        Project project = projectManager.loadProject(projectId);
        Optional<Reference> opt = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE)
            .map(i -> (Reference) i.getBioDataItem()).findFirst();
        if (opt.isPresent() && opt.get().getGeneFile() != null) {
            IndexSearchResult<FeatureIndexEntry> res = featureIndexDao.searchFeatures(featureId,
                                                  geneFileManager.loadGeneFile(opt.get().getGeneFile().getId()),
                                                  maxFeatureSearchResultsCount);
            bookmarkSearchRes.mergeFrom(res);
            return bookmarkSearchRes;
        }

        return bookmarkSearchRes;
    }

    public IndexSearchResult searchFeaturesByReference(String featureId, long referenceId) throws IOException {
        if (featureId == null || featureId.length() < 2) {
            return new IndexSearchResult<>(Collections.emptyList(), false, 0);
        }

        IndexSearchResult<FeatureIndexEntry> bookmarkSearchRes = bookmarkManager.searchBookmarks(featureId,
                                                                                         maxFeatureSearchResultsCount);

        Reference reference = referenceGenomeManager.loadReferenceGenome(referenceId);
        if (reference.getGeneFile() != null) {
            IndexSearchResult res = featureIndexDao.searchFeatures(featureId,
                                                       geneFileManager.loadGeneFile(reference.getGeneFile().getId()),
                                                       maxFeatureSearchResultsCount);
            bookmarkSearchRes.mergeFrom(res);
            return bookmarkSearchRes;
        }

        return bookmarkSearchRes;
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
        Project project = projectManager.loadProject(projectId);
        List<Long> vcfIds = project.getItems().stream()
                .filter(item -> item.getBioDataItem() != null
                        && item.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
                .map(item -> ((VcfFile) item.getBioDataItem()).getId())
                .collect(Collectors.toList());

        return vcfManager.getFiltersInfo(vcfIds);
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
                    new BigVcfFeatureIndexBuilder(info, vcfHeader, featureIndexDao, maxVcfIndexEntriesInMemory);

            while (iterator.hasNext()) {
                variantContext = iterator.next();

                if (!variantContext.getContig().equals(currentKey)) {
                    putVariationsInIndex(indexer, currentKey, vcfFile, geneFiles, chromosomeMap);
                    currentKey = variantContext.getContig();
                }
                indexer.add(variantContext, chromosomeMap);
            }

            // Put the last one
            if (variantContext != null && currentKey != null && Utils
                    .chromosomeMapContains(chromosomeMap, currentKey)) {
                List<VcfIndexEntry> processedEntries = indexer.build(geneFiles,
                        Utils.getFromChromosomeMap(chromosomeMap, currentKey));

                featureIndexDao.writeLuceneIndexForFile(vcfFile, processedEntries, info);
                LOGGER.info(MessageHelper
                        .getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE,
                                currentKey));
                indexer.clear();
            }
        } catch (IOException | GeneReadingException e) {
            throw new FeatureIndexException(vcfFile, e);
        }
    }

    private void putVariationsInIndex(VcfFeatureIndexBuilder indexer, String currentKey,
            VcfFile vcfFile, List<GeneFile> geneFiles, Map<String, Chromosome> chromosomeMap)
            throws GeneReadingException, IOException {
        if (currentKey != null && Utils.chromosomeMapContains(chromosomeMap, currentKey)) {
            List<VcfIndexEntry> processedEntries =
                    indexer.build(geneFiles, Utils.getFromChromosomeMap(chromosomeMap, currentKey));

            featureIndexDao
                    .writeLuceneIndexForFile(vcfFile, processedEntries, indexer.getFilterInfo());
            LOGGER.info(MessageHelper
                    .getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentKey));
            indexer.clear();
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
            FeatureIndexEntry masterEntry = new FeatureIndexEntry();
            masterEntry.setFeatureId(feature.getFeatureId());
            masterEntry.setUuid(UUID.randomUUID());
            masterEntry.setChromosome(chromosomeMap.containsKey(feature.getContig()) ?
                    chromosomeMap.get(feature.getContig()) :
                    chromosomeMap.get(Utils.changeChromosomeName(feature.getContig())));
            masterEntry.setStartIndex(feature.getStart());
            masterEntry.setEndIndex(feature.getEnd());
            if (GeneUtils.isGene(feature)) {
                masterEntry.setFeatureType(FeatureType.GENE);
            }
            if (GeneUtils.isTranscript(feature)) {
                masterEntry.setFeatureType(FeatureType.MRNA);
            }
            if (GeneUtils.isExon(feature)) {
                masterEntry.setFeatureType(FeatureType.EXON);
            }

            allEntries.add(masterEntry);

            String featureName = feature.getFeatureName();
            masterEntry.setFeatureName(featureName);
        }
    }
}
