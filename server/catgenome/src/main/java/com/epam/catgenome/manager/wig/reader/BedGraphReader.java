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

package com.epam.catgenome.manager.wig.reader;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.FeatureReader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * Helps to read BedGraph files.
 * */
public class BedGraphReader implements Closeable {

    private FeatureReader<BedGraphFeature> reader;

    public BedGraphReader(String wigFile, String index) {
        reader = AbstractFeatureReader.getFeatureReader(
                wigFile,
                index,
                new BedGraphCodec(),
                true);
    }

    /**
     * Return iterator over all features overlapping the given interval
     *
     * @param chromosome
     * @param start
     * @param stop
     * @return Iterator over all feature in the provided region
     * @throws IOException
     */
    public Iterator<BedGraphFeature> query(String chromosome, int start, int stop) throws IOException {
        return reader.query(chromosome, start, stop);
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }
}
