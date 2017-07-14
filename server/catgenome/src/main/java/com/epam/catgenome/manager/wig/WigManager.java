package com.epam.catgenome.manager.wig;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.manager.TrackHelper;
import com.epam.catgenome.util.Utils;
import gnu.trove.list.TFloatList;
import gnu.trove.list.array.TFloatArrayList;
import kotlin.Pair;
import org.jetbrains.bio.big.BigSummary;
import org.jetbrains.bio.big.BigWigFile;
import org.jetbrains.bio.big.FixedStepSection;
import org.jetbrains.bio.big.WigSection;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Manages all work with Wig files.
 * */
@Service
public class WigManager extends AbstractWigManager {

    @Override
    protected Track<Wig> getWigFromFile(final WigFile wigFile, final Track<Wig> track, final Chromosome chromosome)
            throws IOException {
        Assert.notNull(wigFile, getMessage(MessagesConstants.ERROR_FILE_NOT_FOUND));
        TrackHelper.fillBlocks(track, indexes -> new Wig(indexes.getLeft(), indexes.getRight()));
        String downsamplePath = fileManager.getWigFilePath(wigFile, chromosome);
        if (dontNeedToUseDownsampling(track, chromosome)) {
            fillBlocksFromWig(wigFile.getPath(), track, chromosome.getName());
        } else {
            if (downsamplePath == null) {
                LOGGER.debug("Downsampled WIG for file {}:{} not found, using original", wigFile.getId(),
                        wigFile.getPath());
                fillBlocksFromWig(wigFile.getPath(), track, chromosome.getName());
            } else {
                fillBlocksFromWig(downsamplePath, track, chromosome.getName());
            }
        }
        return track;
    }

    @Override
    protected void assertFile(String requestPath) throws IOException {
        Assert.isTrue(parseWig(requestPath), getMessage(MessagesConstants.WRONG_WIG_FILE));
    }

    protected void splitByChromosome(final WigFile wigFile, final Map<String, Chromosome> chromosomeMap)
            throws IOException {
        try (BigWigFile bigWigFile = BigWigFile.read(new File(wigFile.getPath()).toPath())) {
            for (Object o : bigWigFile.getChromosomes().values()) {
                String chr = (String) o;
                if (chromosomeMap.containsKey(chr) || chromosomeMap.containsKey(Utils.changeChromosomeName(chr))) {
                    String realChrName = chr;
                    if (chromosomeMap.containsKey(Utils.changeChromosomeName(chr))) {
                        realChrName = Utils.changeChromosomeName(chr);
                    }
                    Chromosome chromosome = chromosomeMap.get(realChrName);
                    if (chromosome.getSize() < WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE) {
                        continue;
                    }
                    int start = 0;
                    int stop = chromosomeMap.get(realChrName).getSize();
                    LOGGER.debug("Processing chromosome " + chr);
                    int bp = start;
                    List<WigSection> sectionList = new ArrayList<>();
                    List<Pair<String, Integer>> chrSizes = Collections.singletonList(new Pair<>(chr, stop - start));

                    while (bp < stop) {
                        int chunkStart = bp;
                        int chunkStop = Math.min(bp + WIG_DOWNSAMPLING_WINDOW - 1, stop);

                        List<BigSummary> summaries = bigWigFile.summarize(chr, chunkStart, chunkStop, 1, true);
                        TFloatList values = new TFloatArrayList();
                        BigSummary bigSummary = summaries.get(0);
                        values.add((float) bigSummary.getMaxValue());
                        WigSection wigSection = new FixedStepSection(chr, chunkStart, chunkStop, 1, values);
                        sectionList.add(wigSection);

                        bp = chunkStop + 1;
                    }

                    fileManager.writeToBigWigFile(wigFile, sectionList, chrSizes, realChrName);
                }
            }
        }
    }

    private boolean parseWig(final String wigFilePath) throws IOException {
        Assert.isTrue(new File(wigFilePath).exists(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        Path path = Paths.get(wigFilePath);
        BigWigFile wigFile = BigWigFile.read(path);
        wigFile.close();
        return true;
    }

    private void fillBlocksFromWig(final String filePath, final Track<Wig> track, final String chromosomeName)
            throws IOException {
        final Path wigPath = Paths.get(filePath);
        LOGGER.debug(getMessage(MessagesConstants.DEBUG_FILE_READING, filePath));
        double time1 = Utils.getSystemTimeMilliseconds();
        try (BigWigFile bigWigFile = BigWigFile.read(wigPath)) {
            fillBlocksNew(track, chromosomeName, bigWigFile);
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Reading from WIG file {}, took {} ms", filePath, time2 - time1);
    }

    private void fillBlocksNew(final Track<Wig> track, final String chromosomeName, final BigWigFile bigWigFile) {
        String realName = null;
        for (Object o : bigWigFile.getChromosomes().values()) {
            String chr = (String) o;
            if (chromosomeName.equals(chr)) {
                realName = chr;
                break;
            } else if (Utils.changeChromosomeName(chromosomeName).equals(chr)) {
                realName = chr;
                break;
            }
        }
        if (realName == null) {
            LOGGER.info("Chromosome not found in big wig file");
        }
        for (Wig wigElement : track.getBlocks()) {
            double score = getNewArrays(bigWigFile, realName, wigElement.getStartIndex() - 1,
                    wigElement.getEndIndex());
            wigElement.setValue((float) score);
        }
    }

    private double getNewArrays(final BigWigFile bigWigFile, final String chrName, final int start, final int end) {
        try {
            double res = 0.0;
            res +=  queryWig(bigWigFile, chrName, start, end);
            return res;
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
            return 0;
        }
    }

    private double queryWig(BigWigFile bigWigFile, String chrName, int start, int end)
            throws IOException {
        List<BigSummary> summarize;
        try {
            summarize = bigWigFile.summarize(chrName, start, end, 1, true);
        } catch (NoSuchElementException e) {
            LOGGER.info(e.getMessage(), e);
            return 0;
        }
        double res = 0.0;
        for (BigSummary summary : summarize) {
            if (!Double.isNaN(summary.getMaxValue()) && !Double.isInfinite(summary.getMaxValue())) {
                res += summary.getMaxValue();
            }
        }
        return res;
    }
}
