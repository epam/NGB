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

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.wig.Wig;
import com.epam.catgenome.entity.wig.WigFile;
import com.epam.catgenome.manager.BiologicalDataItemManager;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import net.sf.ehcache.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract class for wig processors, contains common logic for all wig processors implementations
 * */
public abstract class AbstractWigProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWigProcessor.class);

    static final int WIG_DOWNSAMPLING_WINDOW = 100_000;
    static final int WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE = 10_000_000;
    static final float WIG_DOWNSAMPLING_SCALE_FACTOR = 0.00005F;

    final BiologicalDataItemManager biologicalDataItemManager;
    final FileManager fileManager;

    public AbstractWigProcessor(BiologicalDataItemManager biologicalDataItemManager, FileManager fileManager) {
        this.biologicalDataItemManager = biologicalDataItemManager;
        this.fileManager = fileManager;
    }

    boolean dontNeedToUseDownsampling(Track<Wig> track, Chromosome chromosome) {
        return track.getScaleFactor() > WIG_DOWNSAMPLING_SCALE_FACTOR || chromosome.getSize() <
                WIG_MIN_DOWNSAMPLING_CHROMOSOME_SIZE;
    }

    abstract void assertFile(String requestPath) throws IOException;

    abstract Track<Wig> getWigFromFile(WigFile wigFile, Track<Wig> track,
                                       Chromosome chromosome, EhCacheBasedIndexCache indexCache) throws IOException;

    abstract void splitByChromosome(WigFile wigFile, Map<String, Chromosome> chromosomeMap, EhCacheBasedIndexCache indexCache) throws IOException;

    void prepareWigFileToWork(final WigFile wigFile) throws IOException {
     //no-op
    }
}
