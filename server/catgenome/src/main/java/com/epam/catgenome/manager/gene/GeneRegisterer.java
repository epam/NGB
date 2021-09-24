/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager.gene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.epam.catgenome.util.IndexUtils;
import com.epam.catgenome.util.PositionalOutputStream;
import com.epam.catgenome.util.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.util.LittleEndianOutputStream;


/**
 * Source:      GeneRegisterer
 * Created:     20.10.16, 12:51
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * A class that registers GeneFile's in the system: creates index if required and all the helper files: large scale
 * file, transcript file and histogram
 *
 *
 */
public class GeneRegisterer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneRegisterer.class);
    private static final String CHARSET_NAME = "UTF-8";


    private FileManager fileManager;
    private FeatureIndexManager featureIndexManager;
    private GeneFile geneFile;

    private PositionalOutputStream largeScaleOS = null;
    private BlockCompressedOutputStream largeScaleBCOS = null;
    private PositionalOutputStream transcriptOS = null;
    private BlockCompressedOutputStream transcriptBCOS = null;
    private BufferedWriter writerTranscript = null;
    private BufferedWriter writerLargeScale = null;
    private IndexUtils.FeatureIterator<GeneFeature, LineIterator> iterator = null;
    private LittleEndianOutputStream indexOutputStream = null;
    private LittleEndianOutputStream largeScaleIndexOutputStream = null;
    private LittleEndianOutputStream transcriptIndexOutputStream = null;
    private long largeScalePosition = 0;
    private long transcriptPosition = 0;
    private boolean largeScaleWritten = false;
    private boolean transcriptWritten = false;
    private Map<String, GeneFeature> geneMap = new HashMap<>();
    private Map<String, GeneFeature> transcriptMap = new HashMap<>();
    private TabixIndexCreator indexCreator = new TabixIndexCreator(null, TabixFormat.GFF);
    private TabixIndexCreator largeScaleIndexCreator = new TabixIndexCreator(TabixFormat.GFF);
    private TabixIndexCreator transcriptIndexCreator = new TabixIndexCreator(TabixFormat.GFF);
    private Map<String, Chromosome> chromosomeMap;

    // stuff for IndexMetadata
    private int startPosition;
    private int endPosition;
    private String currentKey;
    private Map<String, Pair<Integer, Integer>> metaMap = new HashMap<>();  // to fix bugs with compressed files
    private GeneFeature lastFeature;

    // histogram stuff
    private int histogramSize;
    private int intervalLength;
    private int intervalEnd;
    private Wig currentWig;
    private List<Wig> histogram = new ArrayList<>();
    private Chromosome currentChromosome;
    private boolean createHelperFiles;

    private static final double HISTOGAM_BLOCK_SIZE_PART = 0.000025F;
    private static final int HISTOGAM_SIZE_LIMIT = 1000;

    public GeneRegisterer(ReferenceGenomeManager referenceGenomeManager, FileManager fileManager,
                          FeatureIndexManager featureIndexManager, GeneFile geneFile, boolean createHelperFiles) {
        this.fileManager = fileManager;
        this.featureIndexManager = featureIndexManager;
        this.geneFile = geneFile;

        chromosomeMap = referenceGenomeManager.loadChromosomes(geneFile.getReferenceId())
                .stream().collect(Collectors.toMap(BaseEntity::getName, c -> c));
        this.createHelperFiles = createHelperFiles;
    }

    /**
     * Processes a gene file (GTF/GFF) for a registration in NGGB system. Creates an index if necessary, and
     * additional helper files for boosting their browsing speed.
     *
     * @param request a request object to register Gene file
     * @throws IOException
     */
    public void processRegistration(final FeatureIndexedFileRegistrationRequest request)
            throws IOException {
        createFileIndices(request.getPath(), request.getIndexPath(),
                StringUtils.isEmpty(request.getIndexPath()), request.isDoIndex());
    }

    public void reIndexFile(boolean createTabix)
            throws IOException {
        createFileIndices(geneFile.getPath(), geneFile.getIndex().getPath(), createTabix, true);
    }

    private void createFileIndices(String filePath, String indexPath,
            boolean createTabixIndex, boolean createFeatureIndex)
            throws IOException {
        File indexFile = fileManager.makeFileForGeneIndex(geneFile, GeneFileType.ORIGINAL);
        File largeScaleIndexFile = null;
        File transcriptIndexFile = null;
        if (createHelperFiles) {
            largeScaleIndexFile = fileManager.makeFileForGeneIndex(geneFile, GeneFileType.LARGE_SCALE);
            if (largeScaleIndexFile.exists()) {
                largeScaleIndexFile.delete();
            }
            transcriptIndexFile = fileManager.makeFileForGeneIndex(geneFile, GeneFileType.TRANSCRIPT);
            if (transcriptIndexFile.exists()) {
                transcriptIndexFile.delete();
            }
        }
        GeneFeature firstFeature;
        try {
            openStreams(geneFile, filePath);

            firstFeature = processFileContents(geneFile, indexFile, indexPath,
                    largeScaleIndexFile, transcriptIndexFile, createTabixIndex, createFeatureIndex);
        } finally {
            closeStreams(geneFile);
        }

        if (createHelperFiles) {
            if (!largeScaleWritten) {
                fileManager.deleteGeneHelperFile(geneFile, firstFeature.getClass(), GeneFileType.LARGE_SCALE);
            }
            if (!transcriptWritten) {
                fileManager.deleteGeneHelperFile(geneFile, firstFeature.getClass(), GeneFileType.TRANSCRIPT);
            }
        }
    }

    private GeneFeature processFileContents(GeneFile geneFile, File indexFile, String indexPath,
                                     File largeScaleIndexFile, File transcriptIndexFile,
                                     boolean createTabixIndex, boolean createFeatureIndex)
            throws IOException {

        GeneFeature feature = null;
        GeneFeature firstFeature = null;
        lastFeature = null;

        // histogram stuff
        int featuresCount = 0;

        List<FeatureIndexEntry> allEntries = new ArrayList<>();
        // main loop - here we process gene file, add it's features to an index and create helper files: large scale
        // and transcript
        while (iterator.hasNext()) {
            // read the next line if available
            final long filePointer = iterator.getPosition();
            //add the feature to the index
            feature = (GeneFeature) iterator.next();

            if (firstFeature == null) {
                firstFeature = feature;
                lastFeature = feature;
                initializeHistogram(firstFeature);
            }

            featuresCount = processFeature(feature, featuresCount, createTabixIndex, allEntries, createFeatureIndex,
                                           filePointer);
        }

        processLastFeature(feature, featuresCount, geneFile, allEntries, createFeatureIndex);

        makeIndexes(geneFile, metaMap, indexFile, largeScaleIndexFile,
                transcriptIndexFile, createTabixIndex, indexPath);

        if (createHelperFiles) {
            writerLargeScale.flush();
            writerTranscript.flush();
        }

        return firstFeature;
    }

    private void processLastFeature(GeneFeature feature, int featuresCount, GeneFile geneFile,
                                    List<FeatureIndexEntry> allEntries, boolean doFeatureIndex) throws IOException {
        // Put the last one in metaMap
        if (feature != null) {
            endPosition = feature.getStart();
            if (currentKey != null) {
                metaMap.put(currentKey, new ImmutablePair<>(startPosition, endPosition));
                // Put the last one
                if (Utils.chromosomeMapContains(chromosomeMap, currentKey) && doFeatureIndex) {
                    featureIndexManager.writeLuceneIndexForFile(geneFile, allEntries, null);
                    allEntries.clear();
                }
            }
        }

        if (featuresCount > 0 && currentWig != null && currentChromosome != null) {
            currentWig.setValue((float) featuresCount);
            histogram.add(currentWig);
            fileManager.writeHistogram(geneFile, currentChromosome.getName(), histogram);
        }
    }

    private int processFeature(GeneFeature feature, int featuresCount, boolean doIndex,
                               final List<FeatureIndexEntry> allEntries, boolean doFeatureIndex, final long filePointer)
        throws IOException {
        int currFeatureCount = featuresCount;
        if (feature != null) {
            Utils.checkSorted(feature, lastFeature, this.geneFile);
            if (doIndex) {
                indexCreator.addFeature(feature, filePointer);
            }

            addToHelperFiles(feature, this.geneFile);

            // populate meta-map and write histogram for current chromosome
            if (!feature.getContig().equals(currentKey)) {
                addToMetamapAndHistogram(metaMap, chromosomeMap, feature, featuresCount, this.geneFile);
                currFeatureCount = 0;

                writeEntriesForChromosome(allEntries, doFeatureIndex);
            }

            if (currentChromosome != null && feature.getEnd() > intervalEnd) {
                currentWig.setValue((float) featuresCount);
                histogram.add(currentWig);

                currentWig = new Wig(intervalEnd + 1, intervalEnd + 1 + intervalLength);
                intervalEnd = intervalEnd + 1 + intervalLength;
                currFeatureCount = 0;
            }

            indexFeature(feature, allEntries, doFeatureIndex);

            currFeatureCount++;
            endPosition = feature.getStart();
            lastFeature = feature;
        }

        return currFeatureCount;
    }

    private void writeEntriesForChromosome(List<FeatureIndexEntry> allEntries, boolean doFeatureIndex)
        throws IOException {
        if (currentKey != null && Utils.chromosomeMapContains(chromosomeMap, currentKey) && doFeatureIndex) {

            featureIndexManager.writeLuceneIndexForFile(geneFile, allEntries, null);
            LOGGER.info(MessageHelper.getMessage(
                MessagesConstants.INFO_FEATURE_INDEX_CHROMOSOME_WROTE, currentKey));
            allEntries.clear();
        }
    }

    private void indexFeature(GeneFeature feature, List<FeatureIndexEntry> allEntries, boolean doFeatureIndex) {
        if (doFeatureIndex) {
            featureIndexManager.addGeneFeatureToIndex(allEntries, feature, chromosomeMap);
        }
    }

    private void initializeHistogram(GeneFeature firstFeature) {
        currentKey = firstFeature.getContig();
        currentChromosome = chromosomeMap.containsKey(currentKey) ? chromosomeMap.get(currentKey) : chromosomeMap
                .get(Utils.changeChromosomeName(currentKey));
        startPosition = firstFeature.getStart();
        endPosition = firstFeature.getStart();
        histogramSize = currentChromosome != null ? Math.min(
            (int) Math.ceil(currentChromosome.getSize() * HISTOGAM_BLOCK_SIZE_PART), HISTOGAM_SIZE_LIMIT) : 0;

        intervalLength =
                (currentChromosome != null) ? (currentChromosome.getSize() / histogramSize) : 0;
        currentWig = new Wig(1, intervalLength, 0);
        intervalEnd = intervalLength;
    }

    private void addToMetamapAndHistogram(Map<String, Pair<Integer, Integer>> metaMap,
                                          Map<String, Chromosome> chromosomeMap, GeneFeature feature,
                                          int featuresCount, GeneFile geneFile) throws IOException {
        if (currentChromosome != null) {
            metaMap.put(currentChromosome.getName(), new ImmutablePair<>(startPosition, endPosition));
            currentWig.setValue((float) featuresCount);
            histogram.add(currentWig);
            fileManager.writeHistogram(geneFile, currentChromosome.getName(), histogram);
            histogram.clear();
        }

        startPosition = feature.getStart();
        currentKey = feature.getContig();

        currentChromosome = chromosomeMap.containsKey(currentKey) ? chromosomeMap.get(currentKey) :
                chromosomeMap.get(Utils.changeChromosomeName(currentKey));
        // calculate histogram blocks size for next chromosome
        if (currentChromosome != null) {
            histogramSize = Math.min(
                (int) Math.ceil(currentChromosome.getSize() * HISTOGAM_BLOCK_SIZE_PART), HISTOGAM_SIZE_LIMIT);
            intervalLength = currentChromosome.getSize() / histogramSize;
            intervalEnd = intervalLength;
            currentWig = new Wig(1, intervalEnd);
        } else {
            currentWig = null;
        }
    }

    private void makeIndexes(GeneFile geneFile, Map<String, Pair<Integer, Integer>> metaMap, File indexFile,
                             File largeScaleIndexFile, File transcriptIndexFile, boolean doIndex,
                             String indexPath)
            throws IOException {
        fileManager.makeIndexMetadata(geneFile, metaMap);

        // write the index to a file
        TabixIndex index;
        if (doIndex) {
            iterator.close();
            index = (TabixIndex) indexCreator.finalizeIndex(iterator.getPosition());
            // VERY important! either use write based on input file or pass the little endian a BGZF stream
            index.write(indexFile);
        }

        if (createHelperFiles) {
            writerLargeScale.flush();
            index = (TabixIndex) largeScaleIndexCreator.finalizeIndex(largeScalePosition);
            index.write(largeScaleIndexFile);
            writerTranscript.flush();
            index = (TabixIndex) transcriptIndexCreator.finalizeIndex(transcriptPosition);
            index.write(transcriptIndexFile);
        }

        geneFile.setIndex(doIndex ? createIndexItem(indexFile.getAbsolutePath()) :
                createIndexItem(indexPath));
    }

    private BiologicalDataItem createIndexItem(String indexPath) {
        BiologicalDataItem indexItem = new BiologicalDataItem();
        indexItem.setCreatedDate(new Date());
        indexItem.setPath(indexPath);
        indexItem.setSource(indexPath);
        indexItem.setFormat(BiologicalDataItemFormat.GENE_INDEX);
        indexItem.setType(BiologicalDataItemResourceType.FILE);
        indexItem.setName("");

        return indexItem;
    }

    private void addToHelperFiles(GeneFeature feature, GeneFile geneFile) throws IOException {
        if (!createHelperFiles) {
            return;
        }

        if (GeneUtils.isGene(feature) || GeneUtils.isTranscript(feature)) {
            boolean written = addFeatureToLargeScaleFile(feature);
            if (written) {
                writerLargeScale.flush();
                largeScaleIndexCreator.addFeature(feature, largeScalePosition);
                largeScalePosition = getFilePosition(geneFile, largeScaleBCOS, largeScaleOS);
            }
            largeScaleWritten = largeScaleWritten || written;

            written = addFeatureToTranscriptFile(feature);
            if (written) {
                writerTranscript.flush();
                transcriptIndexCreator.addFeature(feature, transcriptPosition);
                transcriptPosition = getFilePosition(geneFile, transcriptBCOS, transcriptOS);
            }
            transcriptWritten = transcriptWritten || written;
        }
    }

    private long getFilePosition(GeneFile geneFile, BlockCompressedOutputStream bcos, PositionalOutputStream os) {
        return geneFile.getCompressed() ? bcos.getFilePointer() : os.getPosition();
    }

    private boolean addFeatureToLargeScaleFile(final GeneFeature feature) throws IOException {
        if (!createHelperFiles) {
            return false;
        }

        if (GeneUtils.isGene(feature)) {
            geneMap.put(feature.getGroupId(), feature);
            writerLargeScale.write(feature.toString());
            writerLargeScale.write('\n');
            return true;
        }

        return false;
    }

    private boolean addFeatureToTranscriptFile(GeneFeature feature) throws IOException {
        if (!createHelperFiles) {
            return false;
        }

        String transcriptId = GeneUtils.getTranscriptId(feature);
        if (GeneUtils.isTranscript(feature) && transcriptId != null
                && !transcriptMap.containsKey(transcriptId)) {
            transcriptMap.put(transcriptId, feature);
            writerTranscript.write(feature.toString());
            writerTranscript.write('\n');
            return true;
        }

        return false;
    }

    private void openStreams(GeneFile geneFile, String filePath)
        throws
        IOException {

        final String extension = Utils.getFileExtension(filePath);
        GffCodec.GffType gffType = GffCodec.GffType.forExt(extension);
        AsciiFeatureCodec<GeneFeature> codec = new GffCodec(gffType);

        if (createHelperFiles) {
            if (geneFile.getCompressed()) {
                largeScaleOS = null;
                largeScaleBCOS = fileManager.makeGeneBlockCompressedOutputStream(gffType, geneFile,
                        GeneFileType.LARGE_SCALE);
                transcriptOS = null;
                transcriptBCOS = fileManager.makeGeneBlockCompressedOutputStream(gffType, geneFile,
                        GeneFileType.TRANSCRIPT);
                writerTranscript = new BufferedWriter(new OutputStreamWriter(transcriptBCOS,
                        Charset.forName(CHARSET_NAME)));
                writerLargeScale = new BufferedWriter(new OutputStreamWriter(largeScaleBCOS,
                        Charset.forName(CHARSET_NAME)));
            } else {
                largeScaleOS = fileManager.makePositionalOutputStream(gffType, geneFile, GeneFileType.LARGE_SCALE);
                largeScaleBCOS = null;
                transcriptOS = fileManager.makePositionalOutputStream(gffType, geneFile, GeneFileType.TRANSCRIPT);
                transcriptBCOS = null;
                writerTranscript = new BufferedWriter(new OutputStreamWriter(transcriptOS,
                        Charset.forName(CHARSET_NAME)));
                writerLargeScale = new BufferedWriter(new OutputStreamWriter(largeScaleOS,
                        Charset.forName(CHARSET_NAME)));
            }
        }

        iterator = new IndexUtils.FeatureIterator<>(filePath, codec);
    }

    private void closeStreams(GeneFile geneFile) {
        IOUtils.closeQuietly(indexOutputStream);
        IOUtils.closeQuietly(largeScaleIndexOutputStream);
        IOUtils.closeQuietly(transcriptIndexOutputStream);
        IOUtils.closeQuietly(iterator);
        IOUtils.closeQuietly(writerLargeScale);
        IOUtils.closeQuietly(writerTranscript);
        if (!geneFile.getCompressed()) { // is already closed
            IOUtils.closeQuietly(transcriptBCOS);
        }
        IOUtils.closeQuietly(transcriptOS);
        if (!geneFile.getCompressed()) { // is already closed
            IOUtils.closeQuietly(largeScaleBCOS);
        }
        IOUtils.closeQuietly(largeScaleOS);
    }
}
