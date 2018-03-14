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

package com.epam.catgenome.manager.wig;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.wig.reader.BedGraphCodec;
import com.epam.catgenome.manager.wig.reader.BedGraphFeature;
import com.epam.catgenome.manager.wig.reader.BedGraphReader;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.NgbFileUtils;
import com.epam.catgenome.util.Utils;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import htsjdk.samtools.util.PeekableIterator;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.interval.IntervalTreeIndex;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Manages all work with BedGraph files.
 * */
public class BedGraphProcessor extends AbstractWigProcessor {

    private static final String IDX_EXTENSION = ".idx";

    public BedGraphProcessor(BiologicalDataItemManager biologicalDataItemManager, FileManager fileManager) {
        super(biologicalDataItemManager, fileManager);
    }

    @Override
    protected Track<Wig> getWigFromFile(final WigFile wigFile, final Track<Wig> track,
                                        final Chromosome chromosome, EhCacheBasedIndexCache indexCache)
            throws IOException {
        Assert.notNull(wigFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        TrackHelper.fillBlocks(track, indexes -> new Wig(indexes.getLeft(), indexes.getRight()));
        String downsamplePath = fileManager.getDownsampledBedGraphFilePath(wigFile);
        if (dontNeedToUseDownsampling(track, chromosome)) {
            fillBlocksFromFile(wigFile.getPath(), wigFile.getIndex().getPath(),
                    track, chromosome.getName(), indexCache);
        } else {
            if (downsamplePath == null) {
                LOGGER.debug("Downsampled BedGraph for file {}:{} not found, using original", wigFile.getId(),
                        wigFile.getPath());
                fillBlocksFromFile(wigFile.getPath(), wigFile.getIndex().getPath(),
                        track, chromosome.getName(), indexCache);
            } else {
                fillBlocksFromFile(
                        downsamplePath, getDownsampledBedGraphIndex(downsamplePath),
                        track, chromosome.getName(), indexCache
                );
            }
        }
        return track;
    }

    @Override
    protected void prepareWigFileToWork(WigFile wigFile) throws IOException {
        wigFile.setCompressed(IOHelper.isGZIPFile(wigFile.getPath()));
        fileManager.makeBedGraphIndex(wigFile);
        biologicalDataItemManager.createBiologicalDataItem(wigFile.getIndex());
    }

    @Override
    protected void splitByChromosome(WigFile wigFile, Map<String, Chromosome> chromosomeMap,
                                     EhCacheBasedIndexCache indexCache) throws IOException {
        List<BedGraphFeature> sectionList = new ArrayList<>();
        for (Chromosome chromosome : chromosomeMap.values()) {
            String realChrName = fetchRealChrName(wigFile.getIndex().getPath(), chromosome.getName());
            try (PeekableIterator<BedGraphFeature> query = new PeekableIterator<>(
                    new BedGraphReader(wigFile.getPath(), wigFile.getIndex().getPath(), indexCache).query(
                            realChrName, 1, chromosome.getSize() - 1))) {
                int start = 0;
                int stop = chromosome.getSize();
                int bp = start;
                while (bp < stop) {
                    int chunkStart = bp;
                    int chunkStop = Math.min(bp + WIG_DOWNSAMPLING_WINDOW - 1, stop);
                    float chunkScore = getScoreForBounds(query, chunkStart, chunkStop);
                    bp = chunkStop + 1;
                    sectionList.add(new BedGraphFeature(chromosome.getName(), chunkStart, chunkStop, chunkScore));
                }
            }
        }
        File downsampled = fileManager.writeToBedGraphFile(wigFile, sectionList);
        File indexFile = new File(getDownsampledBedGraphIndex(downsampled.getPath()));
        LOGGER.debug("Writing BED_GRAPH index at {}", indexFile.getAbsolutePath());
        IntervalTreeIndex intervalTreeIndex = IndexFactory.createIntervalIndex(downsampled, new BedGraphCodec());
        IndexFactory.writeIndex(intervalTreeIndex, indexFile); // Write it to a file
    }

    @Override
    protected void assertFile(String requestPath) {
        Assert.isTrue(FacadeWigManager.BED_GRAPH_EXTENSIONS.stream()
                        .anyMatch(NgbFileUtils.getFileExtension(requestPath)::contains),
                getMessage(MessagesConstants.WRONG_BED_GRAPH_FILE));
    }

    private void fillBlocksFromFile(String bedGraphPath, String bedGraphIndexPath, Track<Wig> track,
                                    String chromosomeName, EhCacheBasedIndexCache indexCache) throws IOException {
        String realChrName = fetchRealChrName(bedGraphIndexPath, chromosomeName);
        try (PeekableIterator<BedGraphFeature> bedGraphFeatureIterator = new PeekableIterator<>(
                new BedGraphReader(bedGraphPath, bedGraphIndexPath, indexCache)
                        .query(realChrName, track.getStartIndex(), track.getEndIndex())
        )) {
            for (Wig trackBlock : track.getBlocks()) {
                float score = getScoreForBounds(
                        bedGraphFeatureIterator, track.getStartIndex(), trackBlock.getEndIndex()
                );
                trackBlock.setValue(score);
            }
        }
    }

    private String fetchRealChrName(String bedGraphIndexPath, String chromosomeName) {
        Index index = IndexFactory.loadIndex(bedGraphIndexPath);
        String realName = chromosomeName;
        for (String chr : index.getSequenceNames()) {
            if (chromosomeName.equals(chr)) {
                realName = chr;
                break;
            } else if (Utils.changeChromosomeName(chromosomeName).equals(chr)) {
                realName = chr;
                break;
            }
        }
        return realName;
    }

    private float getScoreForBounds(PeekableIterator<BedGraphFeature> query, int chunkStart, int chunkStop) {
        float score = 0.0f;
        while (query.hasNext()) {
            BedGraphFeature bedGraphFeature = query.peek();
            if (bedGraphFeature.getStart() < chunkStop && bedGraphFeature.getEnd() > chunkStart) {
                score = score < bedGraphFeature.getValue() ? bedGraphFeature.getValue() : score;
            }

            if (chunkStop >= bedGraphFeature.getEnd()) {
                //let's skip future because we already move forward
                query.next();
            } else {
                // we should keep bedGraphFeature for next track block
                break;
            }
        }
        return score;
    }

    private String getDownsampledBedGraphIndex(String downsamplePath) {
        return downsamplePath + IDX_EXTENSION;
    }

}
