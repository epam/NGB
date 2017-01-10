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

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.AbstractTrack;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.HistogramWritingException;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.parallel.ParallelTaskExecutionUtils;
import com.epam.catgenome.parallel.TaskExecutor;
import com.epam.catgenome.util.HistogramUtils;
import com.epam.catgenome.util.Utils;

/**
 * Source:      TrackHelper
 * Created:     08.04.16, 16:12
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * A helper service class, incorporating common operations, related with tracks
 * </p>
 */
@Service
public class TrackHelper {
    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private FileManager fileManager;

    private static final String CHROMOSOME_FILED = "chromosome";

    private ExecutorService executorService = TaskExecutor.getExecutorService();
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackHelper.class);

    /**
     * Load fixed track bounds for specified chromosome.
     *
     * @param geneFile   gene file
     * @param chromosome chromosome
     * @return pair of indexes start index and end index
     * @throws IOException if error occurred during working with files
     */
    public Pair<Integer, Integer> loadBounds(final FeatureFile geneFile, final Chromosome chromosome) throws
            IOException {
        final Map<String, Pair<Integer, Integer>> metaMap = fileManager.loadIndexMetadata(geneFile);
        Pair<Integer, Integer> bounds = metaMap.get(chromosome.getName());
        if (bounds == null) {
            bounds = metaMap.get(Utils.changeChromosomeName(chromosome.getName()));
        }

        return bounds;
    }

    /**
     * Sets fixed bounds for track
     * @param track Track to fix bounds
     * @param bounds Pair of bounds to set
     */
    public void setBounds(final Track track, final Pair<Integer, Integer> bounds) {
        if (bounds == null) {
            return;
        }

        if (track.getStartIndex() < bounds.getLeft()) {
            track.setStartIndex(bounds.getLeft());
        }
        if (track.getEndIndex() > bounds.getRight()) {
            track.setEndIndex(bounds.getRight());
        }
    }

    /**
     * Fix Track's bounds to real start/end of features in required FeatureFile for specified Chromosome
     * @param track Track to fix bounds
     * @param geneFile a FeatureFile, from which to load fixed bounds
     * @param chromosome a Chromosome, from which to load fixed bounds
     * @return true if bounds were set successfully. False, if no required Chromosome is found in bounds map or if
     * track bounds don't match real file feature bounds - the are no features in track
     * @throws IOException
     */
    public boolean loadAndSetBounds(final Track track, final FeatureFile geneFile, final Chromosome chromosome) throws
            IOException {
        final Pair<Integer, Integer> bounds = loadBounds(geneFile, chromosome);
        if (bounds == null) {
            track.setBlocks(Collections.emptyList());
            return false;
        }

        // If we are out of variation bounds, return an empty track
        if (track.getStartIndex() > bounds.getRight() || track.getEndIndex() < bounds.getLeft()) {
            track.setBlocks(Collections.emptyList());
            return false;
        }

        setBounds(track, bounds);
        return true;
    }

    /**
     * Validates track parameters before loading any blocks
     *
     * @param track a track to validate
     * @return loaded valid Chromosome for that track
     */
    public Chromosome validateTrack(final AbstractTrack track) {
        Assert.notNull(track.getId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "id"));

        return validateCommonTrack(track);
    }

    /**
     * Validates track parameters, loaded from URL before loading any blocks
     *
     * @param track a track to validate
     * @return loaded valid Chromosome for that track
     */
    public Chromosome validateUrlTrack(final AbstractTrack track, String fileUrl, String indexUrl) {
        Assert.isTrue(StringUtils.isNotBlank(fileUrl));
        Assert.isTrue(StringUtils.isNotBlank(indexUrl));

        return validateCommonTrack(track);
    }

    private Chromosome validateCommonTrack(final AbstractTrack track) {
        Assert.notNull(track.getChromosome(), getMessage(MessagesConstants.ERROR_NULL_PARAM, CHROMOSOME_FILED));
        Assert.notNull(track.getChromosome().getId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "chromosome.id"));
        checkScaleFactor(track.getScaleFactor());
        checkStartEndPosition(track.getStartIndex(), track.getEndIndex());
        final Chromosome chromosome = referenceGenomeManager.loadChromosome(track.getChromosome().getId());
        track.setChromosome(chromosome);

        Assert.notNull(chromosome, getMessage(MessagesConstants.ERROR_CHROMOSOME_ID_NOT_FOUND));
        Assert.isTrue(track.getEndIndex() <= chromosome.getSize(), getMessage(MessagesConstants
                .ERROR_INVALID_PARAM_TRACK_END_GREATER_CHROMOSOME_SIZE, chromosome.getSize(), track.getEndIndex()));

        return chromosome;
    }

    /**
     * Validates a histogram tack
     * @param track a histogram track to validate
     */
    public static void validateHistogramTrack(final Track track) {
        Assert.notNull(track.getId(), getMessage(MessagesConstants.ERROR_NULL_PARAM, "id"));
        Assert.notNull(track.getChromosome(), getMessage(MessagesConstants.ERROR_NULL_PARAM, CHROMOSOME_FILED));
    }

    /**
     * Validates that track's blocks count is less than link com.epam.catgenome.constant.Constants.MAX_INTERVAL.
     * Useful in BAM queries
     * @param track a track to validate
     * @return loaded valid chromosome
     */
    public Chromosome validateTrackWithBlockCount(final Track track) {
        final Chromosome chromosome = validateTrack(track);
        final double scaleFactor = track.getScaleFactor();
        if (scaleFactor < 1) {
            Assert.isTrue((double)(track.getEndIndex() - track.getStartIndex()) * track.getScaleFactor() <=
                    Constants.MAX_INTERVAL, getMessage(MessagesConstants.ERROR_INVALID_PARAM_QUERY_SO_LARGE));
        } else {
            Assert.isTrue((track.getEndIndex() - track.getStartIndex()) <=
                    Constants.MAX_INTERVAL, getMessage(MessagesConstants.ERROR_INVALID_PARAM_QUERY_SO_LARGE));
        }
        return chromosome;
    }

    private static void checkScaleFactor(final Double scaleFactor) {
        Assert.notNull(scaleFactor, getMessage(MessagesConstants.ERROR_NULL_PARAM, "scaleFactor"));
        Assert.isTrue(scaleFactor > 0,
                getMessage(MessagesConstants.ERROR_INVALID_PARAM_TRACK_SCALE_FACTOR_BELOW_ZERO, scaleFactor));
    }

    private static void checkStartEndPosition(final Integer start, final Integer end) {
        Assert.notNull(start, getMessage(MessagesConstants.ERROR_NULL_PARAM, "startIndex"));
        Assert.notNull(end, getMessage(MessagesConstants.ERROR_NULL_PARAM, "endIndex"));
        Assert.isTrue(start > 0 && end > 0, getMessage(MessagesConstants.ERROR_INVALID_PARAM_TRACK_INDEXES_BELOW_ZERO,
                start, end));
        Assert.isTrue(start <= end, getMessage(MessagesConstants.ERROR_INVALID_PARAM_TRACK_START_GREATER_THEN_END,
                start, end));
    }

    /**
     * Fills track with blocks, created by createNewBlockFunction function
     * @param track a Track to fill
     * @param createNewBlockFunction a function, that generates blocks
     * @param <T> Track's type
     */
    public static <T extends Block> void fillBlocks(final Track<T> track,
                                                    final Function<Pair<Integer, Integer>, T> createNewBlockFunction) {
        final List<T> list = new ArrayList<>();
        final double scaleFactor = track.getScaleFactor();
        final int endIndex = track.getEndIndex();
        final int step = (int) Math.max(1, Math.round(1.0 / scaleFactor));
        int start = track.getStartIndex();
        int helpEnd = start + step - 1;
        while (helpEnd <= endIndex) {
            list.add(createNewBlockFunction.apply(new ImmutablePair<>(start, helpEnd)));
            start = helpEnd + 1;
            helpEnd += step;
        }
        if (start <= endIndex) {
            list.add(createNewBlockFunction.apply(new ImmutablePair<>(start, endIndex)));
        }
        track.setBlocks(list);
    }

    /**
     * Creates a histogram, represented by {@code Track} of {@code Wig} blocks for a specified {@code FeatureFile}
     *
     * @param track          a {@code Track} to fill with histogram
     * @param chromosome     a {@code Chromosome} for which to create a histogram
     * @param featureFile    a {@code FeatureFile} from where to read histogram
     * @param readerFunction a function, describing how to read histogram
     * @return a {@code Track}, filled with {@code Wig} blocks, representing the histogram
     * @throws HistogramWritingException
     */
    public Track<Wig> createHistogram(final Track<Wig> track, final Chromosome chromosome,
                                      final FeatureFile featureFile, final HistogramReaderFunction readerFunction)
        throws HistogramWritingException {
        track.setStartIndex(0);
        track.setEndIndex(chromosome.getSize());

        // Create a list of intervals
        final int realStart = track.getStartIndex();
        final int realEnd = track.getEndIndex();
        final List<Pair<Integer, Integer>> intervals = HistogramUtils.createIntervals(realStart, realEnd);

        final double time1 = Utils.getSystemTimeMilliseconds();
        final int numberOfThreads = ParallelTaskExecutionUtils.NUMBER_OF_THREADS;
        final int portionSize = intervals.size() / numberOfThreads;
        final List<Callable<List<Wig>>> callables = new ArrayList<>(numberOfThreads);

        // Create threads and pass each a portion of intervals
        for (int i = 0; i < numberOfThreads; i++) {
            final List<Pair<Integer, Integer>> portion;
            if (i == numberOfThreads - 1) {
                final int remainder = intervals.size() % numberOfThreads;
                portion = intervals.subList(portionSize * i,
                                            Math.min(portionSize * (i + 1) + remainder, intervals.size()));

            } else {
                portion = intervals.subList(portionSize * i, Math.min(portionSize * (i + 1), intervals.size()));
            }

            callables.add(() -> readerFunction.apply(track, featureFile, chromosome, portion));
        }
        final List<Wig> newHistogram = HistogramUtils.executeHistogramCreation(executorService, callables);
        final double time2 = Utils.getSystemTimeMilliseconds();
        LOGGER.debug("Reading histogram, took {} ms", time2 - time1);

        try {
            fileManager.writeHistogram(featureFile, chromosome.getName(), newHistogram);
        } catch (IOException e) {
            throw new HistogramWritingException(e);
        }

        track.setBlocks(newHistogram);
        return track;
    }

    /**
     * A function, that processes {@code FeatureFile} an produces a histogram {@code Wig} {@code Track}
     */
    @FunctionalInterface
    public interface HistogramReaderFunction {

        /**
         * Process a FeatureFile to get list of Wig blocks, representing histogram blocks
         * @param track a Track, representing histogram bounds
         * @param featureFile a FeatureFile to read
         * @param chromosome a Chromosome to read
         * @param portion a List of intervals to calculate histogram blocks
         * @return a List of Wig blocks, representing histogram blocks
         */
        List<Wig> apply(Track<Wig> track, FeatureFile featureFile, Chromosome chromosome, List<Pair<Integer, Integer>>
                portion);

        default HistogramReaderFunction andThen(final HistogramReaderFunction after) {
            Objects.requireNonNull(after);
            return after::apply;
        }
    }
}
