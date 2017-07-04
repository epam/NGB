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

package com.epam.catgenome.manager.parallel;

import org.slf4j.Logger;

import com.epam.catgenome.entity.track.Track;

/**
 * Created: 2/29/2016
 * Project: CATGenome Browser
 *
 * <p>
 * An Util class, containing common action, related to parallel task execution
 * </p>
 */
public final class ParallelTaskExecutionUtils {
    /**
     * Maximum size of index block to read it in 1 thread. All larger blocks are separated in several callable tasks to
     * read.
     */
    public static final Integer MAX_BLOCK_SIZE = 1000000;

    private ParallelTaskExecutionUtils() {
        // no-op
    }

    /**
     * Returns a number of intervals, that track can be split
     * @param track a track to split
     * @param logger logger to log debug info
     * @return a number of intervals, that track can be split
     */
    public static int splitFileReadingInterval(final Track track, final Logger logger, final int maxTaskNumber) {
        return splitFileReadingInterval(track.getStartIndex(), track.getEndIndex(), logger, maxTaskNumber);
    }

    /**
     * Returns a number of intervals, that interval, specified by start and end indexes, can be split
     * @param startIndex start index of interval to split
     * @param endIndex end index of interval to split
     * @param logger logger to log debug info
     * @return a number of intervals, that track can be split
     */
    public static int splitFileReadingInterval(final int startIndex, final int endIndex, final Logger logger,
                                               final int maxTaskNumber) {
        int numOfSubIntervals = 1;
        int interval = endIndex - startIndex;
        if (interval >= ParallelTaskExecutionUtils.MAX_BLOCK_SIZE) {
            numOfSubIntervals = interval / ParallelTaskExecutionUtils.MAX_BLOCK_SIZE;
            if (numOfSubIntervals > maxTaskNumber) {
                numOfSubIntervals = maxTaskNumber;
            }
            logger.info("Number of callable tasks: {} ", numOfSubIntervals);
        }
        return numOfSubIntervals;
    }
}
