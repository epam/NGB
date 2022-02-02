/*
 * MIT License
 *
 * Copyright (c) 2017-2021 EPAM Systems
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

package com.epam.catgenome.manager.vcf;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_REGISTER_FILE;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_VCF_ID_INVALID;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_VCF_INDEX;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epam.catgenome.controller.vo.ga4gh.CallSet;
import com.epam.catgenome.controller.vo.ga4gh.CallSetSearch;
import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.dao.index.indexer.BigVcfFeatureIndexBuilder;
import com.epam.catgenome.dao.index.indexer.VcfFeatureIndexBuilder;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.exception.VcfReadingException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.gene.GeneTrackManager;
import com.epam.catgenome.manager.vcf.reader.VcfGa4ghReader;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.IndexUtils;
import com.epam.catgenome.util.InfoFieldParser;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.feature.reader.AbstractEnhancedFeatureReader;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.interval.IntervalTreeIndex;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFSimpleHeaderLine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jettison.json.JSONException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.entity.vcf.InfoItem;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.entity.vcf.VariationQuery;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.entity.vcf.VcfSample;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.DownloadFileManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.reader.AbstractVcfReader;
import com.epam.catgenome.manager.vcf.reader.VcfReader;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.TribbleException;
import htsjdk.variant.variantcontext.VariantContext;
import org.springframework.util.CollectionUtils;

/**
 * {@code VcfManager} represents a service class designed to encapsulate all business
 * logic operations required to manage {@code VcfFile} and corresponded tracks, e.g. to process
 * variants uploads, position-based and/or zoom queries etc.
 */
@Slf4j
@Service
public class VcfManager {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private VcfFileManager vcfFileManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private TrackHelper trackHelper;

    @Autowired
    private HttpDataManager httpDataManager;

    @Autowired
    private DownloadFileManager downloadFileManager;

    @Autowired
    private GeneTrackManager geneTrackManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    public static final double HTSJDK_WRONG_QUALITY = -10.0;

    @Value("#{catgenome['vcf.filter.whitelist']}")
    private String[] whiteArray;
    private List<String> whiteList;

    @Value("${vcf.extended.info.patterns}")
    private String extendedInfoTemplates;

    private InfoFieldParser infoFieldParser;

    @Value("#{catgenome['search.indexer.buffer.size'] ?: 256}")
    private int indexBufferSize;

    /**
     * Registers a VCF file in the system to make it available to browse. Creates Tribble/Tabix index if absent
     * and a feature index to allow fast search for variations
     *
     * @param request a request for file registration, containing path to file, reference ID and optional parameters:
     *                path to index file and vcf file name to save in the system
     * @return a {@code VcfFile} that was registered
     */
    public VcfFile registerVcfFile(final FeatureIndexedFileRegistrationRequest request) {
        final String requestPath = request.getPath();
        Assert.isTrue(StringUtils.isNotBlank(requestPath), getMessage(
                MessagesConstants.ERROR_NULL_PARAM, "path"));
        Assert.notNull(request.getReferenceId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "referenceId"));
        final double time1 = Utils.getSystemTimeMilliseconds();
        VcfFile vcfFile;
        final Reference reference = referenceGenomeManager.load(request.getReferenceId());
        final Map<String, Chromosome> chromosomeMap = reference.getChromosomes().stream().collect(
                Collectors.toMap(BaseEntity::getName, chromosome -> chromosome));
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }
        switch (request.getType()) {
            case GA4GH:
                vcfFile = getVcfFileFromGA4GH(request, requestPath);
                break;
            case FILE:
            case S3:
            case AZ:
                vcfFile = createVcfFromFile(request, chromosomeMap, reference, request.isDoIndex());
                break;
            case DOWNLOAD:
                vcfFile = downloadVcfFile(request, requestPath, chromosomeMap, reference,
                        request.isDoIndex());
                break;
            case URL:
                vcfFile = createVcfFromUrl(request, chromosomeMap, reference);
                break;
            default:
                throw new IllegalArgumentException(
                        getMessage(MessagesConstants.ERROR_INVALID_PARAM));
        }
        final double time2 = Utils.getSystemTimeMilliseconds();
        log.debug("File registration took {} ms", time2 - time1);
        return vcfFile;
    }

    /**
     * Delete vcf file metadata from database and feature file directory.
     *
     * @param vcfFileId id of file to remove
     * @return deleted file
     * @throws IOException if error occurred during deleting feature file directory
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public VcfFile unregisterVcfFile(final Long vcfFileId) throws IOException {
        Assert.notNull(vcfFileId, MessagesConstants.ERROR_INVALID_PARAM);
        Assert.isTrue(vcfFileId > 0, MessagesConstants.ERROR_INVALID_PARAM);
        final VcfFile vcfFile = vcfFileManager.load(vcfFileId);
        Assert.notNull(vcfFile, MessagesConstants.ERROR_NO_SUCH_FILE);
        vcfFileManager.deleteVcfFile(vcfFile);
        if (vcfFile.getType() == BiologicalDataItemResourceType.GA4GH) {
            return vcfFile;
        }
        fileManager.deleteFeatureFileDirectory(vcfFile);
        return vcfFile;
    }

    /**
     * Loads variations for a specified track, for a specified sample
     *
     * @param track    a {@code Track} to load variations for
     * @param sampleId specifies sample to load variations for
     * @param loadInfo specifies if extended info should be loaded
     * @param collapse flag determines if variations should be collapsed on small scale
     * @return a {@code Track} with variations
     */
    public Track<Variation> loadVariations(final Track<Variation> track, final Long sampleId, final boolean loadInfo,
                                           final boolean collapse) throws VcfReadingException {
        final double time1 = Utils.getSystemTimeMilliseconds();
        final Chromosome chromosome = trackHelper.validateTrack(track);

        final VcfFile vcfFile = vcfFileManager.load(track.getId());
        Assert.notNull(vcfFile, getMessage(ERROR_VCF_ID_INVALID, track.getId()));
        final Integer sampleIndex = getSampleIndex(sampleId, vcfFile);
        Assert.notNull(vcfFile.getIndex(), getMessage(ERROR_VCF_INDEX, track.getId()));
        if (track.getType() == null) {
            track.setType(TrackType.VCF);
        }

        AbstractVcfReader.createVcfReader(vcfFile.getType(), httpDataManager, fileManager,
                referenceGenomeManager).readVariations(vcfFile, track, chromosome, sampleIndex,
                loadInfo, collapse, indexCache);

        final double time2 = Utils.getSystemTimeMilliseconds();
        log.debug("Track request took {} ms", time2 - time1);
        return track;
    }

    /**
     * Loads variations for a specified track, for a specified sample
     *
     * @param track    a {@code Track} to load variations for
     * @param sampleIndex specifies sample to load variations for
     * @param fileUrl URL of VCF file resource
     * @param indexUrl URL of VCF index resource
     * @param loadInfo specifies if extended info should be loaded
     * @param collapse flag determines if variations should be collapsed on small scale
     * @return a {@code Track} with variations
     */
    public Track<Variation> loadVariations(final Track<Variation> track, final String fileUrl, final String indexUrl,
                                           final Integer sampleIndex, final boolean loadInfo, final boolean collapse)
        throws VcfReadingException {
        final double time1 = Utils.getSystemTimeMilliseconds();
        final Chromosome chromosome = trackHelper.validateUrlTrack(track, fileUrl, indexUrl);

        final VcfFile notRegisteredFile = makeTemporaryVcfFileFromUrl(fileUrl, indexUrl, chromosome);

        if (track.getType() == null) {
            track.setType(TrackType.VCF);
        }

        AbstractVcfReader.createVcfReader(BiologicalDataItemResourceType.URL, httpDataManager, fileManager,
                                          referenceGenomeManager).readVariations(notRegisteredFile, track, chromosome,
                                                                                 sampleIndex != null ? sampleIndex : 0,
                                                                                    loadInfo, collapse, indexCache);
        final double time2 = Utils.getSystemTimeMilliseconds();
        log.debug("Track request took {} ms", time2 - time1);
        return track;
    }

    /**
     * Loads a single variation with extended info
     *
     * @param query {@code VariationQuery}, defining variation to load
     * @return desired {@code Variation} from VCF file
     */
    public Variation loadVariation(final VariationQuery query) throws FeatureFileReadingException {
        // converts query to a simple track corresponded to a single nucleotide position, where a particular
        // variation should be presented

        final Track<Variation> track = new Track<>();
        track.setScaleFactor(1D);
        track.setId(query.getId());
        track.setEndIndex(query.getPosition());
        track.setStartIndex(query.getPosition());
        track.setChromosome(referenceGenomeManager.loadChromosome(query.getChromosomeId()));

        // tries to load variation
        loadVariations(track, query.getSampleId(), true, false);
        Assert.notEmpty(track.getBlocks(), getMessage(MessagesConstants.ERROR_NO_SUCH_VARIATION, query.getPosition()));
        final Variation variation = track.getBlocks().get(0);
        extendInfoFields(variation);
        final VcfFile vcfFile = vcfFileManager.load(query.getId());
        final Reference reference = referenceGenomeManager.load(vcfFile.getReferenceId());
        if (reference.getGeneFile() != null) {
            Set<String> geneIds = geneTrackManager.fetchGeneIds(variation.getStartIndex(), variation.getEndIndex(),
                    Collections.singletonList(reference.getGeneFile()), track.getChromosome());
            variation.setGeneNames(geneIds);
        }
        return variation;
    }

    /**
     * Loads a single variation with extended info
     *
     * @param query {@code VariationQuery}, defining variation to load
     * @param fileUrl URL of VCF file resource
     * @param indexUrl URL of VCF index resource
     * @return desired {@code Variation} from VCF file
     */
    public Variation loadVariation(final VariationQuery query, final String fileUrl, final String indexUrl)
        throws FeatureFileReadingException {
        // converts query to a simple track corresponded to a single nucleotide position, where a particular
        // variation should be presented

        final Track<Variation> track = new Track<>();
        track.setScaleFactor(1D);
        track.setEndIndex(query.getPosition());
        track.setStartIndex(query.getPosition());
        track.setChromosome(new Chromosome(query.getChromosomeId()));

        // tries to load variation
        loadVariations(track, fileUrl, indexUrl, query.getSampleId() != null ? query.getSampleId().intValue() : null,
                       true, false);
        Assert.notEmpty(track.getBlocks(), getMessage(MessagesConstants.ERROR_NO_SUCH_VARIATION, query.getPosition()));
        final Variation variation = track.getBlocks().get(0);
        extendInfoFields(variation);
        final Reference reference = referenceGenomeManager.load(track.getChromosome().getReferenceId());
        if (reference.getGeneFile() != null) {
            Set<String> geneIds = geneTrackManager.fetchGeneIds(variation.getStartIndex(), variation.getEndIndex(),
                    Collections.singletonList(reference.getGeneFile()), track.getChromosome());
            variation.setGeneNames(geneIds);
        }
        return variation;
    }

    /**
     * Returns next/previous variation of the specified chromosome in specified VCF file
     *
     * @param fromPosition {@code int} the position from which look for next/previous variation
     * @param vcfFileId    {@code int} ID of the VCF file
     * @param chromosomeId {@code int} ID of the chromosome
     * @param sampleId     {@code Integer} ID of the desired sample to search, can be null
     * @param forward      {@code boolean} flag that determines direction to look for feature
     * @return {@code Gene} next or previous feature
     */
    public Variation getNextOrPreviousVariation(final int fromPosition, final Long vcfFileId, final Long sampleId,
                                                final long chromosomeId, final boolean forward, final String fileUrl,
                                                final String indexUrl) throws VcfReadingException {
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(chromosomeId);
        Assert.isTrue(vcfFileId != null ||
                      (StringUtils.isNotBlank(fileUrl) && StringUtils.isNotBlank(indexUrl)),
                      getMessage(MessagesConstants.ERROR_NULL_PARAM));
        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));

        final int end = forward ? chromosome.getSize() : 0;
        if ((forward && fromPosition + 1 >= end) || (!forward && fromPosition - 1 <= end)) { // no next features
            return null;
        }

        VcfFile vcfFile;
        if (vcfFileId != null) {
            vcfFile = vcfFileManager.load(vcfFileId);
            Assert.notNull(vcfFile, getMessage(ERROR_VCF_ID_INVALID, vcfFileId));
            Assert.notNull(vcfFile.getIndex(), getMessage(ERROR_VCF_INDEX, vcfFileId));
        } else {
            vcfFile = makeTemporaryVcfFileFromUrl(fileUrl, indexUrl, chromosome);
        }
        final VcfReader vcfReader = AbstractVcfReader.createVcfReader(vcfFile.getType(), httpDataManager, fileManager,
                referenceGenomeManager);
        final Integer sampleIndex = getSampleIndex(sampleId, vcfFile);
        return vcfReader.getNextOrPreviousVariation(fromPosition, vcfFile, sampleIndex,
                chromosome, forward, indexCache);
    }

    /**
     * Loads VCF FILTER and INFO data for a {@code Collection} of VCF files
     * @param vcfFileIds {@code Collection} specifies VCF files of interest
     * @return  VCF FILTER and INFO data
     * @throws IOException if an error with file system occurred
     */
    public VcfFilterInfo getFiltersInfo(final Collection<Long> vcfFileIds) throws IOException {
        final VcfFilterInfo filterInfo = new VcfFilterInfo();
        Map<String, InfoItem> infoItems = new HashMap<>();
        final Set<String> availableFilters = new HashSet<>();
        final Set<String> sampleNames = new LinkedHashSet<>();

        for (Long fileId : vcfFileIds) {
            VcfFile vcfFile = vcfFileManager.load(fileId);
            Assert.notNull(vcfFile, getMessage(ERROR_VCF_ID_INVALID, fileId));

            try (FeatureReader<VariantContext> reader =
                    AbstractEnhancedFeatureReader.getFeatureReader(vcfFile.getPath(),
                    new VCFCodec(), false, indexCache)) {
                VCFHeader header = (VCFHeader) reader.getHeader();
                Collection<VCFInfoHeaderLine> headerLines = header.getInfoHeaderLines();
                infoItems.putAll(headerLines.stream()
                        .filter(l -> !isExtendedInfoLine(l.getDescription()))    // Exclude ANN from fields,
                        .map(InfoItem::new)                                 // we don't need it in the index
                        .collect(Collectors.toMap(InfoItem::getName, i -> i)));
                availableFilters.addAll(header.getFilterLines().stream().map(VCFSimpleHeaderLine::getID)
                        .collect(Collectors.toList()));

            }

            for (VcfSample sample : vcfFile.getSamples()) {
                sampleNames.add(sample.getName());
            }
        }

        List<String> filtersWhiteList = getFilterWhiteList();
        if (!filtersWhiteList.isEmpty()) {
            infoItems = scourFilterList(infoItems, filtersWhiteList);
        }

        infoItems.put(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(), new InfoItem(FeatureIndexDao
                .FeatureIndexFields.IS_EXON.getFieldName(), VCFHeaderLineType.Flag, "Defines if a " +
                "variation is located in exon region"));
        filterInfo.setInfoItemMap(infoItems);
        filterInfo.setAvailableFilters(availableFilters);
        filterInfo.setSampleNames(sampleNames);

        return filterInfo;
    }

    /**
     * Creates a feature index for {@link VcfFile}. If an index already exists, it will be deleted and created
     * from scratch
     * @param vcfFileId an ID of VCF file to reindex.
     * @param rewriteTabixIndex
     * @throws FeatureIndexException if an error occurred while writing index
     */
    public VcfFile reindexVcfFile(final long vcfFileId, final Boolean rewriteTabixIndex) throws FeatureIndexException {
        final VcfFile vcfFile = vcfFileManager.load(vcfFileId);
        final Reference reference = referenceGenomeManager.load(vcfFile.getReferenceId());
        final Map<String, Chromosome> chromosomeMap = reference.getChromosomes().stream()
                .collect(Collectors.toMap(BaseEntity::getName, chromosome -> chromosome));
        try {
            fileManager.deleteFileFeatureIndex(vcfFile);
            if (rewriteTabixIndex) {
                writeTabixIndex(vcfFile);
            }
            try (FeatureReader<VariantContext> reader =
                    AbstractEnhancedFeatureReader
                            .getFeatureReader(vcfFile.getPath(), new VCFCodec(), false, indexCache)) {
                Map<String, Pair<Integer, Integer>> metaMap =
                        readMetaMap(vcfFile, chromosomeMap, reader, reference, true);
                fileManager.makeIndexMetadata(vcfFile, metaMap);
            }
        } catch (IOException e) {
            throw new FeatureIndexException(vcfFile, e);
        }
        return vcfFile;
    }

    public InfoFieldParser getExtendedInfoParser() {
        if (infoFieldParser != null) {
            return infoFieldParser;
        }
        if (extendedInfoTemplates == null || extendedInfoTemplates.isEmpty()) {
            infoFieldParser = new InfoFieldParser("");
        } else {
            infoFieldParser = new InfoFieldParser(extendedInfoTemplates);
        }
        return infoFieldParser;
    }

    public void setIndexBufferSize(int indexBufferSize) {
        this.indexBufferSize = indexBufferSize;
    }

    @NotNull
    private VcfFile makeTemporaryVcfFileFromUrl(final String fileUrl, final String indexUrl,
                                                final Chromosome chromosome) throws VcfReadingException {
        try {
            return Utils.createNonRegisteredFile(VcfFile.class, fileUrl, indexUrl, chromosome);
        } catch (InvocationTargetException e) {
            throw new VcfReadingException(fileUrl, e);
        }
    }

    private VcfFilterInfo getFiltersInfo(final FeatureReader<VariantContext> reader) {
        final VcfFilterInfo filterInfo = new VcfFilterInfo();

        final VCFHeader header = (VCFHeader) reader.getHeader();
        final Collection<VCFInfoHeaderLine> headerLines = header.getInfoHeaderLines();
        Map<String, InfoItem> infoItems = headerLines.stream()
                                .filter(l -> !isExtendedInfoLine(l.getDescription()))    // Exclude ANN from fields,
                                .map(InfoItem::new)                                 // we don't need it in the index
                                .collect(Collectors.toMap(InfoItem::getName, i -> i));
        filterInfo.setAvailableFilters(header.getFilterLines().stream().map(VCFSimpleHeaderLine::getID)
                                    .collect(Collectors.toSet()));

        List<String> filtersWhiteList = getFilterWhiteList();
        if (!filtersWhiteList.isEmpty()) {
            infoItems = scourFilterList(infoItems, filtersWhiteList);
        }

        filterInfo.setInfoItemMap(infoItems);

        return filterInfo;
    }

    /**
     * Returns a white list of VCF Info fields, that are available for filtering
     *
     * @return a {@code List} of field names
     */
    private List<String> getFilterWhiteList() {
        if (whiteList == null) {
            if (whiteArray == null) {
                return Collections.emptyList();
            } else {
                whiteList = Arrays.asList(whiteArray);
            }
        }
        return whiteList;
    }

    private VcfFile createVcfFromFile(final IndexedFileRegistrationRequest request,
            final Map<String, Chromosome> chromosomeMap, final Reference reference, final boolean doIndex) {
        VcfFile vcfFile = null;
        try (FeatureReader<VariantContext> reader = AbstractEnhancedFeatureReader
                .getFeatureReader(request.getPath(), request.getIndexPath(), new VCFCodec(),
                        request.getIndexPath() != null, indexCache)) {
            vcfFile = createVcfFile(request, reader);
            fileManager.makeVcfDir(vcfFile.getId());
            if (StringUtils.isBlank(request.getIndexPath())) {
                fileManager.makeVcfIndex(vcfFile);
            }

            // In order to fix bugs with zipped VCF
            final Map<String, Pair<Integer, Integer>> metaMap =
                    readMetaMap(vcfFile, chromosomeMap, reader, reference, doIndex);
            fileManager.makeIndexMetadata(vcfFile, metaMap);
            biologicalDataItemManager.createBiologicalDataItem(vcfFile.getIndex());
            vcfFileManager.create(vcfFile);
        } catch (IOException e) {
            throw new RegistrationException(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
        } finally {
            if (vcfFile != null && vcfFile.getId() != null &&
                vcfFileManager.load(vcfFile.getId()) == null) {
                biologicalDataItemManager.deleteBiologicalDataItem(vcfFile.getBioDataItemId());
                try {
                    fileManager.deleteFeatureFileDirectory(vcfFile);
                } catch (IOException e) {
                    log.error("Unable to delete directory for " + vcfFile.getName(), e);
                }
            }
        }
        return vcfFile;
    }

    private VcfFile createVcfFromUrl(final IndexedFileRegistrationRequest request,
                                      final Map<String, Chromosome> chromosomeMap, final Reference reference) {
        final VcfFile vcfFile;
        try (FeatureReader<VariantContext> reader = AbstractEnhancedFeatureReader.getFeatureReader(request.getPath(),
                                           request.getIndexPath(), new VCFCodec(), true, indexCache)) {
            vcfFile = createVcfFile(request, reader);
            boolean hasVariations = false;
            for (Map.Entry<String, Chromosome> chrEntry : chromosomeMap.entrySet()) {
                CloseableIterator<VariantContext> iterator = Utils.query(reader, chrEntry.getKey(), 1,
                                                                         chrEntry.getValue().getSize());
                if (iterator.hasNext()) {
                    hasVariations = true;
                    break;
                }
            }

            Assert.isTrue(hasVariations, getMessage(MessagesConstants.ERROR_FILE_CORRUPTED_OR_EMPTY, request.getPath(),
                                                    reference.getName()));
        } catch (IOException e) {
            throw new RegistrationException(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
        }
        biologicalDataItemManager.createBiologicalDataItem(vcfFile.getIndex());
        vcfFileManager.create(vcfFile);
        return vcfFile;
    }

    @NotNull
    private Map<String, Pair<Integer, Integer>> readMetaMap(final VcfFile file,
                                                            final Map<String, Chromosome> chromosomeMap,
                                                            final FeatureReader<VariantContext> reader,
                                                            final Reference reference, final boolean doIndex)
            throws IOException {
        final Map<String, Pair<Integer, Integer>> metaMap = new HashMap<>();
        final CloseableIterator<VariantContext> iterator = reader.iterator();
        int startPosition = 1;
        int endPosition = 1;
        String currentKey = null;
        VariantContext variantContext = null;
        VariantContext lastFeature = null;

        final VcfFilterInfo info = getFiltersInfo(reader);
        final VCFHeader vcfHeader = (VCFHeader) reader.getHeader();

        final List<GeneFile> geneFiles  = reference.getGeneFile() != null ?
                                    Collections.singletonList(reference.getGeneFile()) : Collections.emptyList();

        BigVcfFeatureIndexBuilder indexer = null;
        if (doIndex) {
            indexer = new BigVcfFeatureIndexBuilder(info, vcfHeader, featureIndexManager, file,
                            fileManager, geneFiles, indexBufferSize);
        }

        while (iterator.hasNext()) {
            variantContext = iterator.next();
            if (!variantContext.getContig().equals(currentKey)) {
                if (checkMetaMapKey(chromosomeMap, currentKey)) {
                    metaMap.put(currentKey, new ImmutablePair<>(startPosition, endPosition));
                    if (doIndex) {
                        indexer.clear();
                        log.info(getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentKey));
                    }
                }
                startPosition = variantContext.getStart();
                currentKey = variantContext.getContig();
            }
            checkSorted(file, variantContext, lastFeature);
            indexVariation(variantContext, chromosomeMap, indexer, doIndex);
            lastFeature = variantContext;
            // Put the last one in metaMap
            endPosition = variantContext.getStart();
            if (checkMetaMapKey(chromosomeMap, currentKey)) {
                metaMap.put(currentKey, new ImmutablePair<>(startPosition, endPosition));
            }
        }
        // Put the last one
        if (variantContext != null && checkMetaMapKey(chromosomeMap, currentKey) && doIndex) {
            indexer.clear();
            log.info(getMessage(MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentKey));
        }
        if (doIndex) {
            indexer.close();
        }
        return metaMap;
    }

    private void indexVariation(final VariantContext variantContext, final Map<String, Chromosome> chromosomeMap,
                               final VcfFeatureIndexBuilder indexer, final boolean doIndex) {
        if (doIndex) {
            indexer.add(variantContext, chromosomeMap);
        }
    }

    private boolean checkMetaMapKey(final Map<String, Chromosome> chromosomeMap, final String currentKey) {
        return currentKey != null && Utils.chromosomeMapContains(chromosomeMap, currentKey);
    }

    private VcfFile createVcfGA4GH(final IndexedFileRegistrationRequest request) {

        final VcfFile vcfFile = new VcfFile();
        vcfFile.setId(vcfFileManager.createVcfFileId());
        vcfFile.setCompressed(true);
        vcfFile.setPath(request.getPath());
        vcfFile.setSource(request.getPath());
        vcfFile.setName(request.getName() != null ? request.getName() : request.getPath());
        vcfFile.setPrettyName(request.getPrettyName());
        vcfFile.setType(BiologicalDataItemResourceType.GA4GH); // For now we're working only with files
        vcfFile.setCreatedDate(new Date());
        vcfFile.setReferenceId(request.getReferenceId());
        final VcfGa4ghReader reader = new VcfGa4ghReader(httpDataManager, referenceGenomeManager);
        CallSetSearch callSetSearch;
        try {
            callSetSearch = reader.callSetSearch(vcfFile.getPath());
        } catch (JSONException | InterruptedException | ExternalDbUnavailableException | IOException e) {
            throw new RegistrationException(vcfFile.getName(), e);
        }
        final Map<String, Integer> sampleMap = getSampleNameToOffset(callSetSearch.getCallSets());
        if (!CollectionUtils.isEmpty(sampleMap)) {
            final List<VcfSample> samples = sampleMap.entrySet().stream()
                    .map(e -> new VcfSample(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            vcfFile.setSamples(samples);
        }
        if (StringUtils.isNotBlank(request.getIndexPath())) {
            final BiologicalDataItem indexItem = new BiologicalDataItem();
            indexItem.setCreatedDate(new Date());
            indexItem.setPath(request.getIndexPath());
            indexItem.setSource(request.getIndexPath());
            indexItem.setFormat(BiologicalDataItemFormat.VCF_INDEX);
            indexItem.setType(BiologicalDataItemResourceType.GA4GH);
            indexItem.setName("");

            vcfFile.setIndex(indexItem);
        }
        final long vcfId = vcfFile.getId();
        biologicalDataItemManager.createBiologicalDataItem(vcfFile);
        vcfFile.setBioDataItemId(vcfFile.getId());
        vcfFile.setId(vcfId);
        log.info(getMessage(MessagesConstants.INFO_GENE_REGISTER, vcfFile.getId(), vcfFile.getPath()));
        return vcfFile;
    }

    private Map<String, Integer> getSampleNameToOffset(final List<CallSet> callSets) {
        final HashMap<String, Integer> map = new HashMap<>();
        for (CallSet callSet : callSets) {
            String sample = callSet.getId();
            int index = sample.indexOf('-');
            if (index == -1) {
                Assert.isTrue(false, "SampleId error");
            }
            int sampleId = Integer.parseInt(sample.substring(index + 1, sample.length()));
            map.put(callSet.getSampleId(), sampleId);
        }
        return map;
    }

    private VcfFile createVcfFile(final IndexedFileRegistrationRequest request,
            final FeatureReader<VariantContext> reader) {
        final VcfFile vcfFile = new VcfFile();

        final VCFHeader header = (VCFHeader) reader.getHeader();
        final Map<String, Integer> sampleMap = header.getSampleNameToOffset();

        if (!CollectionUtils.isEmpty(sampleMap)) {
            final List<VcfSample> samples = sampleMap.entrySet().stream()
                    .map(e -> new VcfSample(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            vcfFile.setSamples(samples);
        }
        vcfFile.setMultiSample(sampleMap != null && sampleMap.size() > 1);

        final BiologicalDataItemResourceType resourceType = BiologicalDataItemResourceType.translateRequestType(
            request.getType());
        final String fileName = FilenameUtils.getName(request.getPath());

        vcfFile.setCompressed((resourceType == BiologicalDataItemResourceType.FILE
                || resourceType == BiologicalDataItemResourceType.S3) && IOHelper.isGZIPFile(fileName));
        vcfFile.setName(request.getName() != null ? request.getName() : fileName);
        vcfFile.setPrettyName(request.getPrettyName());
        vcfFile.setId(vcfFileManager.createVcfFileId());
        vcfFile.setPath(request.getPath());
        vcfFile.setSource(request.getPath());
        vcfFile.setType(resourceType);
        vcfFile.setCreatedDate(new Date());
        vcfFile.setReferenceId(request.getReferenceId());

        if (StringUtils.isNotBlank(request.getIndexPath())) {
            BiologicalDataItem indexItem = new BiologicalDataItem();
            indexItem.setCreatedDate(new Date());
            indexItem.setPath(request.getIndexPath());
            indexItem.setSource(request.getIndexPath());
            indexItem.setFormat(BiologicalDataItemFormat.VCF_INDEX);
            indexItem.setType(BiologicalDataItemResourceType.translateRequestType(request.getIndexType()));
            indexItem.setName(vcfFile.getName() + "_index");

            vcfFile.setIndex(indexItem);
        }

        final long vcfId = vcfFile.getId();
        biologicalDataItemManager.createBiologicalDataItem(vcfFile);
        vcfFile.setBioDataItemId(vcfFile.getId());
        vcfFile.setId(vcfId);
        log.info(getMessage(MessagesConstants.INFO_GENE_REGISTER, vcfFile.getId(), vcfFile.getPath()));
        return vcfFile;
    }

    @NotNull
    private VcfFile getVcfFileFromGA4GH(final IndexedFileRegistrationRequest request, final String requestPath) {
        final VcfFile vcfFile = createVcfGA4GH(request);
        final BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(requestPath);
        indexItem.setSource(requestPath);
        indexItem.setFormat(BiologicalDataItemFormat.VCF_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.GA4GH);
        indexItem.setName("");
        vcfFile.setIndex(indexItem);
        biologicalDataItemManager.createBiologicalDataItem(vcfFile.getIndex());
        vcfFileManager.create(vcfFile);
        return vcfFile;
    }

    private VcfFile downloadVcfFile(final IndexedFileRegistrationRequest request, final String requestPath,
            final Map<String, Chromosome> chromosomeMap, final Reference reference, final boolean doIndex) {

        final File newFile;
        try {
            newFile = downloadFileManager.downloadFromURL(request.getPath());
        } catch (IOException e) {
            throw new RegistrationException(getMessage(ERROR_REGISTER_FILE, request.getName()), e);
        }
        request.setIndexPath(null);
        request.setName(request.getName() != null ? request.getName() : FilenameUtils.getBaseName(requestPath));
        request.setPath(newFile.getPath());
        return createVcfFromFile(request, chromosomeMap, reference, doIndex);
    }

    private void checkSorted(final VcfFile vcfFile,
                             final VariantContext variantContext,
                             final VariantContext lastFeature) {
        if (lastFeature != null && variantContext.getStart() < lastFeature.getStart() &&
                lastFeature.getContig().equals(variantContext.getContig())) {
            throw new TribbleException.MalformedFeatureFile("Input file is not sorted by start position. \n" +
                    "We saw a record with a start of " + variantContext.getContig()
                    + ":" + variantContext.getStart()
                    + " after a record with a start of "
                    + lastFeature.getContig() + ":" + lastFeature
                    .getStart(), vcfFile.getName());
        }
    }

    private Integer getSampleIndex(final Long sampleId, final VcfFile vcfFile) {
        if (!CollectionUtils.isEmpty(vcfFile.getSamples())) {
            final Map<Long, VcfSample> sampleMap = new LinkedHashMap<>();
            vcfFile.getSamples().forEach(s -> sampleMap.put(s.getId(), s));

            if (sampleId != null) {
                return sampleMap.get(sampleId).getIndex();
            } else {
                return sampleMap.get(sampleMap.keySet().iterator().next()).getIndex();
            }
        }
        return null;
    }

    private Map<String, InfoItem> scourFilterList(final Map<String, InfoItem> map, final List<String> whiteList) {
        return whiteList.stream()
            .filter(map::containsKey)
            .map(map::get)
            .collect(Collectors.toMap(InfoItem::getName, i -> i));
    }

    private boolean isExtendedInfoLine(final String description) {
        final InfoFieldParser parser = getExtendedInfoParser();
        return parser.isExtendedInfoField(description);
    }

    protected void setExtendedInfoTemplates(String extendedInfoTemplates) {
        this.extendedInfoTemplates = extendedInfoTemplates;
    }

    private void extendInfoFields(final Variation variation) {
        final Map<String, Variation.InfoField> infoFieldMap = variation.getInfo();
        final InfoFieldParser parser = getExtendedInfoParser();
        for (Map.Entry<String, Variation.InfoField> infoEntry : infoFieldMap.entrySet()) {
            Variation.InfoField infoField = infoEntry.getValue();
            if (parser.isExtendedInfoField(infoField.getDescription())) {
                extendInfoField(infoField, parser);
            }
        }
    }

    private void extendInfoField(final Variation.InfoField infoField, final InfoFieldParser parser) {
        List<String> values;
        if (infoField.getValue() instanceof List) {
            values = (List<String>) infoField.getValue();
        } else if (infoField.getValue() instanceof String) {
            values = Collections.singletonList((String) infoField.getValue());
        } else {
            return;
        }
        final List<String> header = parser.extractHeaderFromLine(infoField.getDescription());
        final List<List<String>> lines = new ArrayList<>();
        for (String line : values) {
            List<String> data = parser.extractDataFromLine(line);
            if (data.size() == header.size()) {
                lines.add(data);
            } else {
                log.error("Extended info field value doesn't match the format defined in the file header.");
                return;
            }
        }
        infoField.setType(Variation.InfoFieldTypes.TABLE);
        infoField.setHeader(header);
        infoField.setValue(lines);
    }

    private void writeTabixIndex(final VcfFile vcfFile) throws IOException {
        final VCFCodec codec = new VCFCodec();
        final File file = new File(vcfFile.getPath());
        final File indexFile = new File(vcfFile.getIndex().getPath());
        indexFile.delete();
        log.info(getMessage(MessagesConstants.INFO_VCF_INDEX_WRITING, indexFile.getAbsolutePath()));
        if (vcfFile.getCompressed()) {
            final TabixIndex index = IndexUtils.createTabixIndex(vcfFile, codec, TabixFormat.VCF);
            index.write(indexFile);
        } else {
            final IntervalTreeIndex intervalTreeIndex = IndexFactory.createIntervalIndex(file, codec);
            IndexFactory.writeIndex(intervalTreeIndex, indexFile);
        }
    }
}
