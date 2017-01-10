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

package com.epam.catgenome.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.exception.HistogramWritingException;

/**
 * Source:
 * Created:     8/1/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 * <p>
 * An utility class, containing methods for various tasks, connected with histogram building
 * </p>
 */
public final class HistogramUtils {
    public static final Double HISTOGAM_BLOCK_SIZE_PART = 0.000025;
    public static final int HISTOGRAM_SIZE_LIMIT = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(HistogramUtils.class);

    private HistogramUtils() {
        //no-op
    }

    /**
     * Creates histogram block intervals for a histogram of specified size
     *
     * @param trackStart start index of histogram track
     * @param trackEnd end index of histogram track
     * @return list of intervals, representing histogram blocks
     */
    public static List<Pair<Integer, Integer>> createIntervals(final int trackStart, final int trackEnd) {
        // limit to 1000!!!
        final int histogramSize = Math.min((int) Math.ceil(trackEnd * HISTOGAM_BLOCK_SIZE_PART), HISTOGRAM_SIZE_LIMIT);
        final int intervalLength = (trackEnd - trackStart) / histogramSize;
        final List<Pair<Integer, Integer>> intervals = new ArrayList<>(histogramSize);
        for (int i = 0; i < histogramSize; i++) {
            final int startIndex = trackStart + intervalLength * i;
            int endIndex = trackStart + intervalLength * (i + 1);
            if (endIndex > trackEnd) {
                endIndex = trackEnd;
            }

            intervals.add(new ImmutablePair<>(startIndex, endIndex));
        }
        return intervals;
    }

    /**
     * Executes histogram creation, done by callable tasks
     * @param executorService an ExecutorService to execute histogram building tasks
     * @param histogramTasks callable tasks, that create histogram blocks
     * @return a List of {@link Wig} blocks, representing histogram blocks
     * @throws HistogramWritingException if something goes wrong during execution
     */
    public static List<Wig> executeHistogramCreation(final ExecutorService executorService,
                                                     final List<Callable<List<Wig>>> histogramTasks)
        throws HistogramWritingException {
        final List<Wig> histogram = new ArrayList<>();
        try {
            executorService.invokeAll(histogramTasks).forEach(future -> addBlockToHistogram(future, histogram));
        } catch (InterruptedException | AssertionError e) {
            throw new HistogramWritingException(e);
        }
        return histogram;
    }

    /**
     * Creates a {@link Wig} histogram block and adds it to histogram portion, if blockValue > 0
     * @param histogram a List of {@link Wig} blocks, representing histogram
     * @param blockValue a value of histogram block, e.g. amount of features, fitting in interval
     * @param interval block's interval
     */
    public static void addToHistogramPortion(final List<Wig> histogram, final int blockValue,
                                             final Pair<Integer, Integer> interval) {
        if (blockValue > 0) {
            Wig result = new Wig();
            result.setStartIndex(interval.getLeft());
            result.setEndIndex(interval.getRight());
            result.setValue((float) blockValue);

            histogram.add(result);
        }
    }

    private static void addBlockToHistogram(Future<List<Wig>> future, final List<Wig> histogram) {
        try {
            if (future != null) {
                List<Wig> portion = future.get();
                if (portion != null) {
                    histogram.addAll(portion);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception while making histogram :", e);
        }
    }
}
