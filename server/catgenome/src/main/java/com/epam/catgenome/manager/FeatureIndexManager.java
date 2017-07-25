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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.util.DiskBasedList;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.OrganismType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationType;
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
import com.epam.catgenome.manager.vcf.reader.VcfFileReader;
import com.epam.catgenome.util.NggbIntervalTreeMap;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.entity.bed.BedFile;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCompoundHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;

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
    private BedManager bedManager;

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
        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
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
        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
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
        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
        List<VcfFile> files = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
            .map(i -> (VcfFile) i.getBioDataItem())
            .collect(Collectors.toList());
        if (filterForm.getPage() != null && filterForm.getPageSize() != null) {
            IndexSearchResult<VcfIndexEntry> res = featureIndexDao.searchFileIndexesPaging(files,
                                                     filterForm.computeQuery(FeatureType.VARIATION),
                                                     filterForm.getInfoFields(), filterForm.getPage(),
                                                     filterForm.getPageSize(), filterForm.getOrderBy());
            res.setTotalPagesCount(getTotalPagesCount(filterForm, projectId));
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

        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
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
        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
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
            res.setTotalPagesCount(getTotalPagesCount(filterForm));
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

        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
        Optional<Reference> maybeReference = project.getItems().stream()
            .filter(i -> i.getBioDataItem().getFormat() == BiologicalDataItemFormat.REFERENCE)
            .map(i -> (Reference) i.getBioDataItem()).findFirst();

        if (maybeReference.isPresent()) {
            Reference reference = maybeReference.get();
            List<FeatureFile> annotationFiles = referenceGenomeManager.getReferenceAnnotationFiles(reference.getId())
                    .stream()
                    .map(biologicalDataItem -> (FeatureFile) biologicalDataItem)
                    .collect(Collectors.toList());
            GeneFile geneFile = reference.getGeneFile();
            if (geneFile != null) {
                Long geneFileId = geneFile.getId();
                annotationFiles.add(geneFileManager.loadGeneFile(geneFileId));
            }

            IndexSearchResult<FeatureIndexEntry> res = featureIndexDao.searchFeatures(
                    featureId, annotationFiles, maxFeatureSearchResultsCount
            );
            bookmarkSearchRes.mergeFrom(res);
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

        List<FeatureFile> annotationFiles = referenceGenomeManager.getReferenceAnnotationFiles(referenceId)
                .stream()
                .map(biologicalDataItem -> (FeatureFile) biologicalDataItem)
                .collect(Collectors.toList());
        GeneFile geneFile = reference.getGeneFile();
        if (geneFile != null) {
            Long geneFileId = geneFile.getId();
            annotationFiles.add(geneFileManager.loadGeneFile(geneFileId));
        }

        IndexSearchResult<FeatureIndexEntry> res = featureIndexDao.searchFeatures(
                featureId, annotationFiles, maxFeatureSearchResultsCount
        );
        bookmarkSearchRes.mergeFrom(res);
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
        Project project = projectManager.loadProjectAndUpdateLastOpenedDate(projectId);
        List<Long> vcfIds = project.getItems().stream()
                .filter(item -> item.getBioDataItem() != null
                        && item.getBioDataItem().getFormat() == BiologicalDataItemFormat.VCF)
                .map(item -> item.getBioDataItem().getId())
                .collect(Collectors.toList());

        return vcfManager.getFiltersInfo(vcfIds);
    }

    /**
     * Fetch gene IDs of genes, affected by variation. The variation is specified by it's start and end indexes
     *
     * @param intervalMap represents a batch loaded genes form gene file
     * @param start a start index of the variation
     * @param end an end index of the variation
     * @param chromosome a {@code Chromosome}
     * @return a {@code Set} of IDs of genes, affected by the variation
     */
    private Set<VariationGeneInfo> fetchGeneIdsFromBatch(NggbIntervalTreeMap<List<Gene>> intervalMap,
            int start, int end, Chromosome chromosome) {
        Set<VariationGeneInfo> geneIds = getGeneIds(intervalMap, chromosome, start, start);
        if (end > start) {
            geneIds.addAll(getGeneIds(intervalMap, chromosome, end, end));
        }

        return geneIds;
    }

    /**
     * Post processes fetched VCF index entries to add gene information and split them to resolve ambiguous fields
     * @param entries list of entries to process
     * @param geneFiles list of {@link GeneFile}s to fetch gene information from
     * @param chromosome a {@code Chromosome}, from which entries came
     * @param vcfHeader a header of VCF file
     * @param vcfReader a reader for VCF file
     * @return a list of post-processed index entries, ready to write into index
     * @throws GeneReadingException if an exception was thrown when reading genes information
     */
    public List<VcfIndexEntry> postProcessIndexEntries(List<VcfIndexEntry> entries, List<GeneFile> geneFiles,
                                                    Chromosome chromosome, VCFHeader vcfHeader, VcfFileReader vcfReader)
        throws GeneReadingException {
        List<VcfIndexEntry> processedEntries =
                new DiskBasedList<VcfIndexEntry>(maxVcfIndexEntriesInMemory / 2).adaptToList();
        int start = chromosome.getSize();
        int end = 0;
        for (FeatureIndexEntry entry : entries) {
            start = Math.min(start, entry.getStartIndex());
            end = Math.max(end, entry.getEndIndex());
        }

        Map<GeneFile, NggbIntervalTreeMap<List<Gene>>> intervalMapCache = new HashMap<>();

        for (VcfIndexEntry indexEntry : entries) {
            String geneIdsString = null;
            String geneNamesString = null;
            Set<VariationGeneInfo> geneIds = Collections.emptySet();

            for (GeneFile geneFile : geneFiles) {
                if (!intervalMapCache.containsKey(geneFile)) {
                    intervalMapCache.put(geneFile, loadGenesIntervalMap(geneFile, start, end, chromosome));
                }
                NggbIntervalTreeMap<List<Gene>> intervalMap = intervalMapCache.get(geneFile);

                geneIds = fetchGeneIdsFromBatch(intervalMap, indexEntry.getStartIndex(), indexEntry.getEndIndex(),
                                                chromosome);
                geneIdsString = geneIds.stream().map(i -> i.geneId).collect(Collectors.joining(", "));
                geneNamesString = geneIds.stream().map(i -> i.geneName).collect(Collectors.joining(", "));
                indexEntry.setExon(geneIds.stream().anyMatch(i -> i.isExon));
            }

            Set<VariationType> types = new HashSet<>();
            for (int i = 0; i < indexEntry.getVariantContext().getAlternateAlleles().size(); i++) {
                Variation variation = vcfReader.createVariation(indexEntry.getVariantContext(), vcfHeader, i);
                types.add(variation.getType());
            }

            List<String> ambiguousInfoFields = vcfHeader.getInfoHeaderLines().stream()
                    .filter(l -> l.getCount(indexEntry.getVariantContext()) > 1
                            && !isVariableLength(l.getCountType()))
                    .map(VCFCompoundHeaderLine::getID).collect(Collectors.toList());

            List<VcfIndexEntry> simplifiedEntries = simplifyVcfIndexEntries(indexEntry, indexEntry.getVariantContext(),
                                                                        geneIds, types, geneIdsString, geneNamesString);

            processedEntries.addAll(splitAmbiguousInfoFields(simplifiedEntries, ambiguousInfoFields));
        }

        return processedEntries;
    }

    public void buildIndexForFile(FeatureFile featureFile) throws FeatureIndexException, IOException {
        if (!fileManager.indexForFeatureFileExists(featureFile)) {
            switch (featureFile.getFormat()) {
                case BED:
                    bedManager.reindexBedFile(featureFile.getId());
                    break;
                case GENE:
                    // use false here, because it's default parameter for reindexing in GeneController
                    gffManager.reindexGeneFile(featureFile.getId(), false);
                    break;
                case VCF:
                    vcfManager.reindexVcfFile(featureFile.getId());
                    break;
                default:
                    throw new IllegalArgumentException("Wrong FeatureType: " + featureFile.getFormat().name());
            }
        }
    }

    private NggbIntervalTreeMap<List<Gene>> loadGenesIntervalMap(GeneFile geneFile, int start, int end,
            Chromosome chromosome) {
        final NggbIntervalTreeMap<List<Gene>> genesRangeMap = new NggbIntervalTreeMap<>();
        try {
            IndexSearchResult<FeatureIndexEntry> searchResult = featureIndexDao.searchFeaturesInInterval(
                Collections.singletonList(geneFile), start, end, chromosome);
            searchResult.getEntries().stream().filter(f -> f.getFeatureType() == FeatureType.EXON
                    || f.getFeatureType() == FeatureType.GENE).map(f -> {
                        Gene gene = new Gene();
                        gene.setFeature(f.getFeatureType().name());
                        gene.setStartIndex(f.getStartIndex());
                        gene.setEndIndex(f.getEndIndex());
                        gene.setGroupId(f.getFeatureId());
                        gene.setFeatureName(f.getFeatureName().toUpperCase());
                        return gene;
                    }).forEach(g -> {
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

    private Set<VariationGeneInfo> getGeneIds(NggbIntervalTreeMap<List<Gene>> intervalMap,
            Chromosome chromosome, int start, int end) {
        Collection<Gene> genes = intervalMap.getOverlapping(new Interval(chromosome.getName(), start, end))
                .stream()
                .flatMap(l -> l.stream())
                .collect(Collectors.toList());
        Set<VariationGeneInfo> geneIds = new HashSet<>();
        if (genes != null) {
            boolean isExon = genes.stream().anyMatch(GeneUtils::isExon);
            geneIds = genes.stream()
                .filter(GeneUtils::isGene)
                .map(g -> new VariationGeneInfo(g.getGroupId(), g.getFeatureName(), isExon))
                .collect(Collectors.toSet());
        }

        return geneIds;
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
    public void writeLuceneIndexForFile(final FeatureFile featureFile, final List<? extends FeatureIndexEntry> entries)
        throws IOException {
        featureIndexDao.writeLuceneIndexForFile(featureFile, entries);
    }

    public void makeIndexForBedReader(BedFile bedFile, AbstractFeatureReader<BEDFeature, LineIterator> reader,
                                      Map<String, Chromosome> chromosomeMap) throws IOException {
        CloseableIterator<BEDFeature> iterator = reader.iterator();
        List<FeatureIndexEntry> allEntries = new ArrayList<>();
        while (iterator.hasNext()) {
            BEDFeature next = iterator.next();
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
        featureIndexDao.writeLuceneIndexForFile(bedFile, allEntries);
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
            VcfFileReader vcfFileReader = new VcfFileReader(fileManager, referenceGenomeManager);
            List<VcfIndexEntry> allEntries = new ArrayList<>();

            while (iterator.hasNext()) {
                variantContext = iterator.next();

                if (!variantContext.getContig().equals(currentKey)) {
                    putVariationsInIndex(allEntries, currentKey, vcfFile, geneFiles, chromosomeMap, vcfFileReader,
                                         vcfHeader);

                    currentKey = variantContext.getContig();
                }

                addVariationToIndex(allEntries, variantContext, chromosomeMap, info, vcfHeader, vcfFileReader);
            }

            // Put the last one
            if (variantContext != null && currentKey != null &&
                Utils.chromosomeMapContains(chromosomeMap, currentKey)) {
                List<VcfIndexEntry> processedEntries = postProcessIndexEntries(allEntries, geneFiles,
                                       Utils.getFromChromosomeMap(chromosomeMap, currentKey), vcfHeader, vcfFileReader);
                featureIndexDao.writeLuceneIndexForFile(vcfFile, processedEntries);
                LOGGER.info(MessageHelper.getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE,
                                                     currentKey));
                allEntries.clear();
            }
        } catch (IOException | GeneReadingException e) {
            throw new FeatureIndexException(vcfFile, e);
        }
    }

    private void putVariationsInIndex(List<VcfIndexEntry> allEntries, String currentKey, VcfFile vcfFile,
                                      List<GeneFile> geneFiles, Map<String, Chromosome> chromosomeMap,
                                      VcfFileReader vcfFileReader, VCFHeader vcfHeader)
        throws GeneReadingException, IOException {
        if (currentKey != null && Utils.chromosomeMapContains(chromosomeMap, currentKey)) {
            List<VcfIndexEntry> processedEntries = postProcessIndexEntries(allEntries, geneFiles,
                                       Utils.getFromChromosomeMap(chromosomeMap, currentKey), vcfHeader, vcfFileReader);
            featureIndexDao.writeLuceneIndexForFile(vcfFile, processedEntries);
            LOGGER.info(MessageHelper.getMessage(MessagesConstants
                                                     .INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentKey));
            allEntries.clear();
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
                featureIndexDao.writeLuceneIndexForFile(geneFile, allEntries);
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
            featureIndexDao.writeLuceneIndexForFile(geneFile, allEntries);
            allEntries.clear();
        }
    }

    private String checkNextChromosome(Feature feature, String currentChromosomeName,
                                       Map<String, Chromosome> chromosomeMap, List<FeatureIndexEntry> allEntries,
                                       GeneFile geneFile) throws IOException {
        if (!feature.getContig().equals(currentChromosomeName)) {
            if (currentChromosomeName != null && (chromosomeMap.containsKey(currentChromosomeName) ||
                                      chromosomeMap.containsKey(Utils.changeChromosomeName(currentChromosomeName)))) {

                featureIndexDao.writeLuceneIndexForFile(geneFile, allEntries);
                LOGGER.info(MessageHelper.getMessage(
                    MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentChromosomeName));
                allEntries.clear();
            }

            return feature.getContig();
        }

        return currentChromosomeName;
    }

    public void addVariationToIndex(List<VcfIndexEntry> allEntries, VariantContext context,
                                              Map<String, Chromosome> chromosomeMap,
                                              VcfFilterInfo filterInfo, VCFHeader vcfHeader,
                                              VcfFileReader vcfReader) {
        if (chromosomeMap.containsKey(context.getContig())
                || chromosomeMap.containsKey(Utils.changeChromosomeName(context.getContig()))) {
            VcfIndexEntry masterEntry = new VcfIndexEntry();
            masterEntry.setUuid(UUID.randomUUID());
            masterEntry.setFeatureId(context.getID());
            masterEntry.setChromosome(Utils.getFromChromosomeMap(chromosomeMap, context.getContig()));
            masterEntry.setStartIndex(context.getStart());
            masterEntry.setEndIndex(context.getEnd());
            masterEntry.setFeatureType(FeatureType.VARIATION);
            masterEntry.setInfo(filterInfoByWhiteList(context, filterInfo, vcfHeader));
            masterEntry.setVariantContext(context);

            double qual = context.getPhredScaledQual();
            masterEntry.setQuality(MathUtils.equals(qual, VcfManager.HTSJDK_WRONG_QUALITY) ? 0D : qual);

            List<OrganismType> organismTypes = new ArrayList<>();
            for (int i = 0; i < context.getAlternateAlleles().size(); i++) {
                Variation variation = vcfReader.createVariation(context, vcfHeader, i);
                organismTypes.add(variation.getGenotypeData().getOrganismType());
            }

            if (!organismTypes.isEmpty() && organismTypes.stream()
                    .anyMatch(type -> type.equals(OrganismType.NO_VARIATION))) {
                return;
            }

            allEntries.add(masterEntry);
        }
    }

    private List<VcfIndexEntry> simplifyVcfIndexEntries(VcfIndexEntry masterEntry, VariantContext context,
                                                        Set<VariationGeneInfo> geneIds, Set<VariationType> types,
                                                        String geneIdsString, String geneNamesString) {
        List<VcfIndexEntry> simplifiedEntries = new ArrayList<>();
        for (VariationType type : types) {
            if (geneIds.isEmpty()) {
                if (context.getFilters().isEmpty()) {
                    VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                    entry.setVariationType(type);

                    simplifiedEntries.add(entry);
                } else {
                    simplifyFilters(masterEntry, context, simplifiedEntries, type);
                }
            } else {
                simplifyGeneIds(masterEntry, context, geneIds, geneIdsString, geneNamesString, simplifiedEntries, type);
            }
        }

        return simplifiedEntries;
    }

    private void simplifyFilters(VcfIndexEntry masterEntry, VariantContext context,
                                 List<VcfIndexEntry> simplifiedEntries, VariationType type) {
        for (String filter : context.getFilters()) {
            VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
            entry.setVariationType(type);
            entry.setFailedFilter(filter);

            simplifiedEntries.add(entry);
        }
    }

    private void simplifyGeneIds(VcfIndexEntry masterEntry, VariantContext context,
                                 Set<VariationGeneInfo> geneIds, String geneIdsString,
                                 String geneNamesString, List<VcfIndexEntry> simplifiedEntries,
                                 VariationType type) {
        for (VariationGeneInfo geneInfo : geneIds) {
            if (context.getFilters().isEmpty()) {
                VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                entry.setVariationType(type);
                entry.setGene(geneInfo.geneId);
                entry.setGeneName(geneInfo.geneName);
                entry.setGeneIds(geneIdsString);
                entry.setGeneNames(geneNamesString);

                simplifiedEntries.add(entry);
            } else {
                for (String filter : context.getFilters()) {
                    VcfIndexEntry entry = new VcfIndexEntry(masterEntry);
                    entry.setVariationType(type);
                    entry.setGene(geneInfo.geneId);
                    entry.setGeneName(geneInfo.geneName);
                    entry.setGeneIds(geneIdsString);
                    entry.setGeneNames(geneNamesString);
                    entry.setFailedFilter(filter);

                    simplifiedEntries.add(entry);
                }
            }
        }
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

    private Map<String, Object> filterInfoByWhiteList(VariantContext context,
                                                      VcfFilterInfo vcfFilterInfo, VCFHeader vcfHeader) {
        Map<String, Object> permittedInfo = new HashMap<>();
        Map<String, Object> info = context.getAttributes();

        vcfFilterInfo.getInfoItems().forEach(key -> {
            if (info.containsKey(key.getName()) && vcfHeader.getInfoHeaderLine(key.getName()) != null) {
                int count = vcfHeader.getInfoHeaderLine(key.getName()).getCount(context);
                VCFHeaderLineCount countType =
                        vcfHeader.getInfoHeaderLine(key.getName()).getCountType();
                switch (key.getType()) {
                    case Integer:
                        addNumberInfo(permittedInfo, info, count, countType,
                                VCFHeaderLineType.Integer, key);
                        break;
                    case Float:
                        addNumberInfo(permittedInfo, info, count, countType,
                                VCFHeaderLineType.Float, key);
                        break;
                    case Flag:
                        permittedInfo
                                .put(key.getName(), parseFlagInfo(permittedInfo, info, count, key));
                        break;
                    default:
                        permittedInfo.put(key.getName(), info.get(key.getName()));
                }
            }
        });

        return permittedInfo;
    }

    private void addNumberInfo(Map<String, Object> permittedInfo, Map<String, Object> info,
            int count, VCFHeaderLineCount countType, VCFHeaderLineType type, InfoItem key) {
        Object value;
        if (isVariableLength(countType)) {
            value = info.get(key.getName()).toString();
        } else if (count > 1) {
            value = parseNumberArray(type, info.get(key.getName()));

            if (value == null) {
                LOGGER.error(MessageHelper.getMessage(
                        MessagesConstants.ERROR_FEATURE_INDEX_WRITING_WRONG_PARAMETER_TYPE,
                        key.getName(), key.getType(), info.get(key.getName()).toString()));
                return;
            }

            permittedInfo.put("_" + key.getName() + "_v", info.get(key.getName()).toString());
        } else {
            String numberString = info.get(key.getName()).toString();

            if (NumberUtils.isNumber(numberString)) {
                value = parseNumber(type, info.get(key.getName()));
            } else {
                LOGGER.error(MessageHelper.getMessage(
                        MessagesConstants.ERROR_FEATURE_INDEX_WRITING_WRONG_PARAMETER_TYPE,
                        key.getName(), key.getType(), numberString));
                return;
            }
        }

        permittedInfo.put(key.getName(), value);
    }

    private Object parseNumberArray(VCFHeaderLineType type, Object infoObject) {
        switch (type) {
            case Integer:
                return Utils.parseIntArray(infoObject.toString());
            case Float:
                return Utils.parseFloatArray(infoObject.toString());
            default:
                throw new IllegalArgumentException(MessageHelper.getMessage(
                    MessagesConstants.ERROR_FEATURE_INDEX_INVALID_NUMBER_FORMAT, type));
        }
    }

    private Object parseNumber(VCFHeaderLineType type, Object infoObject) {
        switch (type) {
            case Integer:
                return Integer.parseInt(infoObject.toString());
            case Float:
                return Float.parseFloat(infoObject.toString());
            default:
                throw new IllegalArgumentException(MessageHelper.getMessage(
                    MessagesConstants.ERROR_FEATURE_INDEX_INVALID_NUMBER_FORMAT, type));
        }
    }

    private Object parseFlagInfo(Map<String, Object> permittedInfo, Map<String, Object> info, int count, InfoItem key) {
        if (count > 1) {
            permittedInfo.put("_" + key.getName() + "_v", info.get(key.getName()).toString());
            return Utils.parseBooleanArray(info.get(key.getName()).toString());
        } else {
            return Boolean.parseBoolean(info.get(key.getName()).toString());
        }
    }

    private boolean isVariableLength(VCFHeaderLineCount countType) {
        return countType != VCFHeaderLineCount.INTEGER;
    }

    private List<VcfIndexEntry> splitAmbiguousInfoFields(List<VcfIndexEntry> entries,
                                                             List<String> ambigousInfoFields) {
        ArrayList<VcfIndexEntry> queue = new ArrayList<>(entries);
        List<VcfIndexEntry> simplifiedEntries = new ArrayList<>();
        for (String key : ambigousInfoFields) {
            ArrayList<VcfIndexEntry> nextIteration = new ArrayList<>();
            for (FeatureIndexEntry e : queue) {
                VcfIndexEntry vcfIndexEntry = (VcfIndexEntry) e;
                boolean found = false;

                if (vcfIndexEntry.getInfo().containsKey(key) && vcfIndexEntry.getInfo().get(key) instanceof Object[]) {
                    found = true;
                    Object[] arr = (Object[]) vcfIndexEntry.getInfo().get(key);
                    makeCopies(vcfIndexEntry, arr, nextIteration, key);
                }

                if (!found) {
                    simplifiedEntries.add(vcfIndexEntry);
                }
            }

            queue = nextIteration;
        }

        queue.addAll(simplifiedEntries);
        return queue;
    }

    private void makeCopies(VcfIndexEntry vcfIndexEntry, Object[] infoArray,
                            ArrayList<VcfIndexEntry> nextIteration, String key) {
        for (Object element : infoArray) {
            VcfIndexEntry copy = new VcfIndexEntry(vcfIndexEntry);
            copy.getInfo().put(key, element);
            nextIteration.add(copy);
        }
    }

    private static class VariationGeneInfo {
        private String geneId;
        private String geneName;
        private boolean isExon;

        VariationGeneInfo(String geneId, String geneName, boolean isExon) {
            this.geneId = geneId;
            this.geneName = geneName;
            this.isExon = isExon;
        }

        @Override
        public int hashCode() {
            return geneId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass() == this.getClass() && Objects
                    .equals(((VariationGeneInfo) obj).geneId, geneId);
        }
    }
}
