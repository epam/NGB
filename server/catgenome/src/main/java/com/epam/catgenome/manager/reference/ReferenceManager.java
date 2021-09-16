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

package com.epam.catgenome.manager.reference;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import com.epam.catgenome.entity.reference.Species;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.track.ReferenceTrackMode;
import com.epam.catgenome.manager.AuthManager;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.reference.io.FastaSequenceFile;
import com.epam.catgenome.manager.reference.io.FastaUtils;
import com.epam.catgenome.util.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.ga4gh.ReferenceGA4GH;
import com.epam.catgenome.controller.vo.ga4gh.ReferenceSet;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.Ga4ghResourceUnavailableException;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.exception.RegistrationException;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.io.NibDataReader;
import com.epam.catgenome.manager.reference.io.NibDataWriter;

/**
 * Source:      ReferenceManager.java
 * Created:     10/9/15, 3:17 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * {@code ReferenceManager} represents a service class designed to encapsulate all business
 * logic operations required to manage references and corresponded tracks, e.g. to process
 * reference uploads, position-based and/or zoom queries etc.
 */
@Service public class ReferenceManager {

    private JsonMapper objectMapper = new JsonMapper();

    @Autowired private HttpDataManager httpDataManager;

    @Autowired private TrackHelper trackHelper;

    @Autowired private FileManager fileManager;

    @Autowired private ReferenceGenomeManager referenceGenomeManager;

    @Autowired private NibDataReader nibDataReader;

    @Autowired private NibDataWriter nibDataWriter;

    @Autowired private GffManager gffManager;

    @Autowired private GeneFileManager geneFileManager;

    @Autowired private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private AuthManager authManager;

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceManager.class);

    /**
     * @param track {@code Track} Track with information about query
     *              (the most important: chromosome name, Id, start index, end index and scaleFactor)
     * @return {@code Track<Sequence>} return the track-filled sequence
     */
    public Track<Sequence> getNucleotidesResultFromNib(Track<Sequence> track)
            throws ReferenceReadingException {
        track.setType(TrackType.REF);
        try {
            return getNucleotidesTrackFromNib(track);
        } catch (Ga4ghResourceUnavailableException | IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ReferenceReadingException(String.valueOf(track.getId()), e);
        }
    }

    /**
     * Registers a new Reference genome in the database and converts input fasta file into
     * a set of chromosome files, for further efficient querying
     *
     * @param request client VO
     * @return an {@code Reference} instance persisted in the system
     * @throws IOException
     */
    public Reference registerGenome(final ReferenceRegistrationRequest request) throws IOException {

        final String name;
        if (request.getType() == null) {
            request.setType(BiologicalDataItemResourceType.FILE);
        }
        if (request.getType() == BiologicalDataItemResourceType.GA4GH) {
            name = request.getName();
        } else {
            name = parse(request.getPath(), request.getName());
        }
        // prepares to start processing of a reference genome: generates ID, creates a directory
        // to store data for a genome
        final Long referenceId = referenceGenomeManager.createReferenceId();
        final Reference reference = new Reference(referenceId, name);
        reference.setPath(request.getPath());
        reference.setPrettyName(request.getPrettyName());
        if (!request.isNoGCContent()) {
            fileManager.makeReferenceDir(reference);
        }
        reference.setType(request.getType());
        // processes data for a genome and generates all required resources: meta-information,
        // files with NT-sequence and GC-content per each chromosome etc.
        boolean succeeded = false;
        try {
            if (reference.getCreatedDate() == null) {
                reference.setCreatedDate(new Date());
            }
            if (reference.getType() == null) {
                reference.setType(BiologicalDataItemResourceType.FILE);
            }
            biologicalDataItemManager.createBiologicalDataItem(reference);
            reference.setBioDataItemId(reference.getId());
            reference.setId(referenceId);

            long lengthOfGenome;
            if (request.getType() == BiologicalDataItemResourceType.GA4GH) {
                lengthOfGenome = registerGA4GH(request, referenceId, reference);
            } else {
                lengthOfGenome =
                        registerReference(referenceId, reference, !request.isNoGCContent());
            }
            // saves meta-information about the processed genome, including its chromosomes
            reference.setSize(lengthOfGenome);

            if (request.getGeneFileId() != null) {
                Assert.isTrue(request.getGeneFileRequest() == null,
                        getMessage(MessagesConstants.ERROR_REFERENCE_REGISTRATION_PARAMS));
                GeneFile geneFile = geneFileManager.load(request.getGeneFileId());
                reference.setGeneFile(geneFile);
            }
            if (request.getSpecies() != null) {
                String version = request.getSpecies().getVersion();
                Species species = referenceGenomeManager.loadSpeciesByVersion(version);
                Assert.notNull(species, getMessage(MessageCode.NO_SUCH_SPECIES, version));
                reference.setSpecies(species);
            }

            referenceGenomeManager.create(reference);
            processGeneRegistrationRequest(request, reference);
            // sets this flag to 'true' that means all activities are performed successfully and no
            // rollback for applied changes are required
            succeeded = true;
        } catch (InterruptedException | ExternalDbUnavailableException e) {
            LOG.info(String.format("Failed to register reference %s.", request.getName()), e);
        } finally {
            // reverts all changes that have been made in the file system, if something was going wrong
            // and we cannot create a genome in the system)
            if (!succeeded) {
                fileManager.deleteReferenceDir(reference);
                if (reference.getBioDataItemId() != null && !referenceGenomeManager
                        .isRegistered(reference.getId())) {
                    biologicalDataItemManager
                            .deleteBiologicalDataItem(reference.getBioDataItemId());
                }
            }
        }
        return reference;
    }

    private void processGeneRegistrationRequest(ReferenceRegistrationRequest request,
            Reference reference) throws IOException {
        if (request.getGeneFileRequest() != null) {
            try {
                request.getGeneFileRequest().setReferenceId(reference.getId());

                GeneFile geneFile = gffManager.registerGeneFile(request.getGeneFileRequest());
                reference.setGeneFile(geneFile);
                referenceGenomeManager
                        .updateReferenceGeneFileId(reference.getId(), geneFile.getId());
            } catch (RegistrationException e) {
                fileManager.deleteDir(reference.getPath());
                unregisterGenome(reference.getId());
                throw e;
            }
        }
    }

    /**
     * @param startPosition  {@code int} start position at chromosome
     * @param endPosition    {@code int} end position at chromosome
     * @param referenceId    {@code long} need for open the file
     * @param chromosomeName {@code String} need for open the file
     * @return {@code char[]} return char array of nucleotides, at file(relating to referenceId and chromosomeName)
     * started at startPosition and length sequenceLength
     */
    public List<Sequence> getNucleotidesFromNibFile(int startPosition, final int endPosition,
            final long referenceId, final String chromosomeName) throws IOException {
        final Reference reference = referenceGenomeManager.getOnlyReference(referenceId);
        if (isNibReference(reference.getPath())) {
            try (BlockCompressedDataInputStream strm = fileManager
                    .makeRefInputStream(referenceId, chromosomeName);
                    DataInputStream indexStrm = fileManager
                            .makeRefIndexInputStream(referenceId, chromosomeName)) {
                return nibDataReader
                        .getNucleotidesFromNibFile(startPosition, endPosition, strm, indexStrm);
            }
        } else {
            List<Sequence> sequencesList = new ArrayList<>();

            FastaSequenceFile ref = new FastaSequenceFile(reference.getPath(),
                    getIndexPath(reference));
            String bases = new String(ref.getSequence(chromosomeName, startPosition, endPosition),
                    Charset.defaultCharset());
            for (int i = 0; i < bases.length(); i++) {
                sequencesList.add(new Sequence(startPosition + i, String.valueOf(bases.charAt(i))));
            }
            return sequencesList;
        }
    }

    /**
     * Get reference set from the Global Alliance (Google).
     *
     * @param referenceSetId id of reference set
     * @return reference set from genomic google
     * @throws InterruptedException if the thread is interrupted, either before or during the activity
     * @throws IOException          if an error occurred during deleting directory
     */
    private ReferenceSet getReferenceSet(final String referenceSetId)
            throws IOException, InterruptedException, ExternalDbUnavailableException {

        ParameterNameValue[] params = new ParameterNameValue[] {};

        String locationReference =
                Constants.URL_GOOGLE_GENOMIC_API + Constants.URL_REFERENCE_SET + referenceSetId
                        + Constants.GOOGLE_API_KEY;
        String geneData = httpDataManager.fetchData(locationReference, params);
        return objectMapper.readValue(geneData, ReferenceSet.class);
    }

    /**
     * Get reference from the Global Alliance (Google).
     *
     * @param referenceId id of reference
     * @return reference from genomic google
     * @throws InterruptedException if the thread is interrupted, either before or during the activity
     * @throws IOException          if an error occurred during deleting directory
     */
    private ReferenceGA4GH getReference(final String referenceId)
            throws InterruptedException, IOException, ExternalDbUnavailableException {

        ParameterNameValue[] params = new ParameterNameValue[] {};

        String locationReference =
                Constants.URL_GOOGLE_GENOMIC_API + Constants.URL_REFERENCE + referenceId
                        + Constants.GOOGLE_API_KEY;

        String geneData = httpDataManager.fetchData(locationReference, params);
        return objectMapper.readValue(geneData, ReferenceGA4GH.class);
    }

    /**
     * Unregister reference file: delete metadata from database and file directory.
     *
     * @param referenceId id of reference to delete
     * @return deleted reference
     * @throws IOException if an error occurred during deleting directory
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Reference unregisterGenome(final long referenceId) throws IOException {
        Assert.notNull(referenceId, MessagesConstants.ERROR_INVALID_PARAM);
        Assert.isTrue(referenceId > 0, MessagesConstants.ERROR_INVALID_PARAM);
        Reference reference = referenceGenomeManager.load(referenceId);
        Assert.notNull(reference, MessagesConstants.ERROR_NO_SUCH_FILE);

        referenceGenomeManager.delete(reference);
        fileManager.deleteReferenceDir(reference);
        return reference;
    }

    /**
     * Loads a reference sequence in a given interval for a specified reference ID and chromosome name
     *
     * @param startIndex     of the interval of interest
     * @param endIndex       of the interval of interest
     * @param referenceId    to load
     * @param chromosomeName to load
     * @return a {@code String} representation of a reference sequence for the interval of interest
     * @throws IOException
     */
    public String getSequenceString(final int startIndex, final int endIndex,
            final Long referenceId, final String chromosomeName) throws IOException {
        final Reference reference = referenceGenomeManager.getOnlyReference(referenceId);
        if (isNibReference(reference.getPath())) {
            try (BlockCompressedDataInputStream strm = fileManager
                    .makeRefInputStream(reference.getId(), chromosomeName);
                    DataInputStream indexStrm = fileManager
                            .makeRefIndexInputStream(reference.getId(), chromosomeName)) {
                return nibDataReader.getStringFromNibFile(startIndex, endIndex, strm, indexStrm);
            }
        } else {
            FastaSequenceFile ref = new FastaSequenceFile(reference.getPath(), getIndexPath(reference));
            return new String(ref.getSequence(chromosomeName, startIndex, endIndex),
                    Charset.defaultCharset());
        }
    }

    /**
     * Loads a reference sequence in a given interval for a specified reference ID and chromosome name
     *
     * @param startIndex     of the interval of interest
     * @param endIndex       of the interval of interest
     * @param referenceId    to load
     * @param chromosomeName to load
     * @return a byte array representation of a reference sequence for the interval of interest
     * @throws IOException
     */
    public byte[] getSequenceByteArray(final int startIndex, final int endIndex,
            final Long referenceId, final String chromosomeName) throws IOException {
        final Reference reference = referenceGenomeManager.getOnlyReference(referenceId);
        if (isNibReference(reference.getPath())) {
            try (BlockCompressedDataInputStream strm = fileManager
                    .makeRefInputStream(referenceId, chromosomeName);
                    DataInputStream indexStrm = fileManager
                            .makeRefIndexInputStream(referenceId, chromosomeName)) {
                return nibDataReader
                        .getByteNucleotidesFromNibFile(startIndex, endIndex, strm, indexStrm);
            }

        } else {
            FastaSequenceFile ref = new FastaSequenceFile(reference.getPath(), getIndexPath(reference));
            return ref.getSequence(chromosomeName, startIndex, endIndex);
        }
    }


    protected Track<Sequence> getNucleotidesTrackFromNib(Track<Sequence> track)
            throws IOException, Ga4ghResourceUnavailableException {
        Assert.notNull(track.getType(), getMessage(MessagesConstants.ERROR_NULL_PARAM));
        final Chromosome chr = trackHelper.validateTrackWithBlockCount(track);
        final long trackID = track.getId();
        final String cName = chr.getName();
        final Reference reference = referenceGenomeManager.getOnlyReference(trackID);
        final int startIndex = track.getStartIndex();
        final int endIndex = track.getEndIndex();
        final double scaleFactor = track.getScaleFactor();
        List<Sequence> sequencesList;
        if (scaleFactor > Constants.GC_FORMAT_FACTOR) {
            sequencesList =
                    getReferenceSequenceWithoutGC(chr, trackID, cName, reference, startIndex,
                            endIndex);
            track.setMode(ReferenceTrackMode.NUCLEOTIDES);
        } else {
            sequencesList =
                    getReferenceSequenceWithGC(chr, trackID, reference, startIndex, endIndex,
                            scaleFactor);
            if (sequencesList.isEmpty()) {
                track.setMode(ReferenceTrackMode.NO_GC_DATA);
            } else {
                track.setMode(ReferenceTrackMode.GC_CONTENT);
            }
        }
        track.setBlocks(sequencesList);
        return track;
    }

    private List<Sequence> getReferenceSequenceWithGC(Chromosome chr, long trackID,
            Reference reference, int startIndex, int endIndex, double scaleFactor)
            throws IOException {
        final int chromosomeSize = chr.getSize();
        final String chromosomeName = chr.getName();
        if (reference.getType() == BiologicalDataItemResourceType.GA4GH) {
            if ((endIndex - startIndex) > Constants.GA4GH_MAX_BASE_SIZE) {
                return Collections.emptyList();
            }
            return getGCForGA4GH(startIndex, endIndex, scaleFactor, chr.getPath());
        } else {
            return getGCData(trackID, startIndex, endIndex, scaleFactor, chromosomeSize,
                    chromosomeName, reference);
        }
    }

    private List<Sequence> getGCData(long trackID, int startIndex, int endIndex, double scaleFactor,
            int chromosomeSize, String chromosomeName, Reference reference) throws IOException {
        if (scaleFactor <= (1.0 / Constants.GC_CONTENT_STEP)
                && chromosomeSize > Constants.GC_CONTENT_MIN_LENGTH) {
            LOG.debug(getMessage(MessagesConstants.DEBUG_FILE_READING));
            try (BlockCompressedDataInputStream strm = fileManager
                    .makeGCInputStream(trackID, chromosomeName);
                    DataInputStream indexStrm = fileManager
                            .makeGCIndexInputStream(trackID, chromosomeName)) {
                return getGCFromGCFile(startIndex, endIndex, scaleFactor, strm, indexStrm);
            } catch (IllegalArgumentException e) {
                //gc content may be disabled
                LOG.debug(e.getMessage(), e);
                return Collections.emptyList();
            }

        } else {
            if (isNibReference(reference.getPath())) {
                LOG.debug(getMessage(MessagesConstants.DEBUG_FILE_READING));
                try (BlockCompressedDataInputStream strm = fileManager
                        .makeRefInputStream(trackID, chromosomeName);
                        DataInputStream indexStrm = fileManager
                                .makeRefIndexInputStream(trackID, chromosomeName)) {
                    return getGCFromNibFile(startIndex, endIndex, scaleFactor, strm, indexStrm);
                }
            } else {
                LOG.debug(getMessage(MessagesConstants.DEBUG_FILE_READING));
                String sequence =
                        getSequenceString(startIndex, endIndex, reference.getId(), chromosomeName);
                return nibDataReader
                        .fillSequenceOfGCFromFasta(startIndex, endIndex, scaleFactor, sequence);

            }
        }
    }

    private List<Sequence> getReferenceSequenceWithoutGC(Chromosome chr, long trackID, String cName,
            Reference reference, int startIndex, int endIndex)
            throws Ga4ghResourceUnavailableException, IOException {
        LOG.debug(getMessage(MessagesConstants.DEBUG_FILE_READING));
        if (reference.getType() == BiologicalDataItemResourceType.GA4GH) {
            return nibDataReader.getNucleotidesFromNibGA4GH(startIndex, endIndex, chr.getPath());
        } else {
            return getNucleotidesFromNibFile(startIndex, endIndex, trackID, cName);
        }
    }

    /**
     * Validates and parses the given to make sure that all mandatory properties,
     * describing genome data, are provided.
     * <p>
     * The default values will be assigned in cases when it is possible to do. E.g., to treat omitted
     * custom name for a genome it's possible to use an original name of corresponded file without
     * extension.
     *
     * @param path {@code File}     Path to fasta file
     * @param name {@code String}   Alternative name
     */
    private String parse(final String path, final String name) {
        Assert.notNull(path, getMessage(MessageCode.RESOURCE_NOT_FOUND));
        // checks that an original file name is provided, because it is used as a name
        // for a genome if custom name isn't specified
        String fileName = StringUtils.trimToNull(FilenameUtils.getName(path));
        Assert.notNull(fileName, getMessage(MessageCode.MANDATORY_FILE_NAME));
        // checks that file is in one of supported formats
        boolean supported = false;
        final Collection<String> formats = FastaUtils.getFastaExtensions();
        for (final String ext : formats) {
            if (fileName.endsWith(ext)) {
                supported = true;
                fileName = Utils.removeFileExtension(fileName, ext);
                break;
            }
        }
        if (!supported) {
            throw new IllegalArgumentException(getMessage("error.reference.illegal.file.type",
                    StringUtils.join(formats, ", ")));
        }
        // if no custom name is provided for a genome, then a file name without extension should be
        // used by default
        return StringUtils.defaultString(StringUtils.trimToNull(name), fileName);
    }

    private List<Sequence> getGCFromGCFile(int startPosition, final int endPosition,
            final double scaleFactor, final BlockCompressedDataInputStream gcContentStream,
            final DataInputStream indexStream) throws IOException {

        return nibDataReader.fillSequenceOfGCFromGCFile(startPosition, endPosition, scaleFactor,
                gcContentStream, indexStream);
    }

    private List<Sequence> getGCForGA4GH(final Integer startPosition, final Integer endPosition,
            final Double scaleFactor, final String referenceId) throws IOException {

        return nibDataReader
                .fillSequenceOfGCForGA4GH(startPosition, endPosition, scaleFactor, referenceId);
    }

    private List<Sequence> getGCFromNibFile(int startPosition, final int endPosition,
            final double scaleFactor, final BlockCompressedDataInputStream gcContentStream,
            final DataInputStream indexStream) throws IOException {
        //arrays started at zero position, but chromosome started ad first position
        return nibDataReader.fillSequenceOfGCFromNibFile(startPosition, endPosition, scaleFactor,
                gcContentStream, indexStream);
    }

    private long registerReference(Long referenceId, Reference reference, boolean createGC)
            throws IOException {
        String path = reference.getPath();
        setIndex(reference);
        long lengthOfGenome = 0;
        FastaSequenceFile referenceReader = new FastaSequenceFile(path, reference.getIndex().getPath());
        for (String chr : referenceReader.getChromosomeNames()) {
            lengthOfGenome += referenceReader.getSequenceSize(chr);
            // prepares meta-information about the current chromosome
            final Chromosome chromosome = new Chromosome();
            chromosome.setName(chr);
            chromosome.setSize(referenceReader.getSequenceSize(chr));
            chromosome.setReferenceId(referenceId);
            chromosome.setPath(reference.getPath());
            reference.getChromosomes().add(chromosome);

            //work with GC
            if (!NgbFileUtils.isRemotePath(path) && createGC) {
                byte[] sequence = referenceReader.getChromosome(chr);
                try (BlockCompressedDataOutputStream gcStream = fileManager
                        .makeGCOutputStream(referenceId, chromosome)) {
                    nibDataWriter.byteArrayToGCFile(sequence, gcStream);
                }
                fileManager.makeGcIndex(referenceId, chromosome.getName());
            }
        }
        return lengthOfGenome;
    }

    private void setIndex(Reference reference) {
        String path = reference.getPath();
        String indexPath;
        if (!NgbFileUtils.isRemotePath(path) && !FastaUtils.hasIndex(path)) {
            indexPath = fileManager.createReferenceIndex(reference);
        } else {
            indexPath = path + FastaUtils.FASTA_INDEX;
        }
        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(indexPath);
        indexItem.setFormat(BiologicalDataItemFormat.REFERENCE_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");
        indexItem.setOwner(authManager.getAuthorizedUser());
        reference.setIndex(indexItem);
    }

    private long registerGA4GH(ReferenceRegistrationRequest request, Long referenceId,
            Reference reference)
            throws IOException, InterruptedException, ExternalDbUnavailableException {
        final List<String> listReferenceId = getReferenceSet(request.getPath()).getReferenceIds();
        long lengthOfGenome = 0;
        for (String id : listReferenceId) {
            ReferenceGA4GH referenceGA4GH = getReference(id);
            lengthOfGenome += Integer.parseInt(referenceGA4GH.getLength());
            // prepares meta-information about the current chromosome
            final Chromosome chromosome = new Chromosome();
            chromosome.setName(referenceGA4GH.getName());
            chromosome.setSize(Integer.parseInt(referenceGA4GH.getLength()));
            chromosome.setReferenceId(referenceId);
            chromosome.setPath(referenceGA4GH.getId());
            reference.getChromosomes().add(chromosome);
        }
        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(request.getPath());
        indexItem.setFormat(BiologicalDataItemFormat.REFERENCE_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.GA4GH);
        indexItem.setName("");
        reference.setIndex(indexItem);
        return lengthOfGenome;
    }

    private boolean isNibReference(String path) {
        return !NgbFileUtils.isRemotePath(path) && !FastaUtils.isFasta(path);
    }

    //method to support intermediate references not nib but without registered index item
    private String getIndexPath(Reference reference) {
        BiologicalDataItem index = reference.getIndex();
        //it's a dummy index
        if (index.getFormat() == BiologicalDataItemFormat.INDEX) {
            return reference.getPath() + FastaUtils.FASTA_INDEX;
        } else {
            return index.getPath();
        }
    }
}
