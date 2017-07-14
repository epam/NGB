package com.epam.catgenome.manager.wig;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.manager.wig.reader.BedGraphCodec;
import com.epam.catgenome.manager.wig.reader.BedGraphReader;
import com.epam.catgenome.manager.wig.reader.BedGraphFeature;
import com.epam.catgenome.util.IOHelper;
import com.epam.catgenome.util.NgbFileUtils;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.interval.IntervalTreeIndex;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
public class BedGraphManager extends AbstractWigManager {

    public static final String IDX_EXTENSION = "idx";

    @Override
    protected Track<Wig> getWigFromFile(final WigFile wigFile, final Track<Wig> track, final Chromosome chromosome)
            throws IOException {
        Assert.notNull(wigFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        TrackHelper.fillBlocks(track, indexes -> new Wig(indexes.getLeft(), indexes.getRight()));
        String downsamplePath = fileManager.getDownsampledBedGraphFilePath(wigFile);
        if (dontNeedToUseDownsampling(track, chromosome)) {
            fillBlocksFromFromFile(wigFile.getPath(), wigFile.getIndex().getPath(), track, chromosome.getName());
        } else {
            if (downsamplePath == null) {
                LOGGER.debug("Downsampled BedGraph for file {}:{} not found, using original", wigFile.getId(),
                        wigFile.getPath());
                fillBlocksFromFromFile(wigFile.getPath(), wigFile.getIndex().getPath(), track, chromosome.getName());
            } else {
                fillBlocksFromFromFile(
                        downsamplePath, getDownsampledBedGraphIndex(downsamplePath), track, chromosome.getName()
                );
            }
        }
        return track;
    }

    @Override
    protected void prepareToWorkWigFile(WigFile wigFile) throws IOException {
        wigFile.setCompressed(IOHelper.isGZIPFile(wigFile.getPath()));
        fileManager.makeBedGraphIndex(wigFile);
        biologicalDataItemManager.createBiologicalDataItem(wigFile.getIndex());
        super.prepareToWorkWigFile(wigFile);
    }



    @Override
    protected void splitByChromosome(WigFile wigFile, Map<String, Chromosome> chromosomeMap) throws IOException {
        List<BedGraphFeature> sectionList = new ArrayList<>();
        for (Chromosome chromosome : chromosomeMap.values()) {
            try (BedGraphReader bedGraphReader = new BedGraphReader(wigFile.getPath(), wigFile.getIndex().getPath())) {
                Iterator<BedGraphFeature> query = bedGraphReader.query(
                        chromosome.getName(), 1, chromosome.getSize() - 1
                );
                int start = 0;
                int stop = chromosome.getSize();
                int bp = start;
                BedGraphFeature next = query.hasNext() ? query.next() : null;
                while (bp < stop) {
                    int chunkStart = bp;
                    int chunkStop = Math.min(bp + WIG_DOWNSAMPLING_WINDOW - 1, stop);
                    float chunkValue = 0;
                    while (query.hasNext() && next.getStart() <= chunkStop) {
                        if (chunkStart <= next.getStart()) {
                            chunkValue = chunkValue < next.getValue() ? next.getValue() : chunkValue;
                        }
                        next = query.next();
                    }
                    bp = chunkStop + 1;
                    sectionList.add(new BedGraphFeature(chromosome.getName(), chunkStart, chunkStop, chunkValue));
                }
            }
        }
        File downsampled = fileManager.writeToBedGraphFile(wigFile, sectionList);
        File indexFile = new File(downsampled + ".idx");
        LOGGER.debug("Writing BED_GRAPH index at {}", indexFile.getAbsolutePath());
        IntervalTreeIndex intervalTreeIndex = IndexFactory.createIntervalIndex(downsampled, new BedGraphCodec());
        IndexFactory.writeIndex(intervalTreeIndex, indexFile); // Write it to a file
    }

    @Override
    protected void assertFile(String requestPath) {
        Assert.isTrue(AbstractWigManager.BED_GRAPH_EXTENSIONS.stream()
                        .anyMatch(NgbFileUtils.getFileExtension(requestPath)::contains),
                getMessage(MessagesConstants.WRONG_BED_GRAPH_FILE));
    }

    private String getDownsampledBedGraphIndex(String downsamplePath) {
        return downsamplePath + IDX_EXTENSION;
    }

    private void fillBlocksFromFromFile(String bedGraphPath, String bedGraphIndexPath, Track<Wig> track,
                                        String chromosome) throws IOException {
        try (BedGraphReader bedGraphReader = new BedGraphReader(bedGraphPath, bedGraphIndexPath)) {
            Iterator<BedGraphFeature> wigFeatureIterator = bedGraphReader.query(
                    chromosome, track.getStartIndex(), track.getEndIndex());
            BedGraphFeature next = wigFeatureIterator.hasNext() ? wigFeatureIterator.next() : null;
            for (Wig wig : track.getBlocks()) {
                float score = 0.0f;
                while (wigFeatureIterator.hasNext() && next.getStart() <= wig.getEndIndex()) {
                    score = score < next.getValue() ? next.getValue() : score;
                    if (wig.getEndIndex() >= next.getEnd()) {
                        next = wigFeatureIterator.next();
                    } else {
                        break;
                    }
                }
                wig.setValue(score);
            }
        }
    }

}
