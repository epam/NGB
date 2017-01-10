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

package com.epam.catgenome.manager.seg.parser;

import java.util.regex.Pattern;

import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;

/**
 * {@code SegCodec} is a codec for parsing data from a seg file according to the
 * specified file format
 */
public class SegCodec extends AsciiFeatureCodec<SegFeature> {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\t|( +)");
    private static final int START_OFFSET_VALUE = 1;
    private static final int ID_OFFSET = 0;
    private static final int CHR_OFFSET = 1;
    private static final int START_OFFSET = 2;
    private static final int END_OFFSET = 3;
    private static final int NUM_MARK_OFFSET = 4;
    private static final int SEG_MEAN_OFFSET = 5;

    public SegCodec() {
        super(SegFeature.class);
    }

    /**
     * Creates a {@code SegFeature} from a file line
     * @param line to parse
     * @return a parsed {@code SegFeature}
     */
    @Override
    public SegFeature decode(String line) {
        if (line.trim().isEmpty()) {
            return null;
        }
        if (line.startsWith("'") || line.startsWith("Sample")) {
            return null;
        }
        String[] tokens = SPLIT_PATTERN.split(line, -1);
        return decode(tokens);
    }

    /**
     * Header isn't read in this implementation
     * @param reader file reader
     * @return always null
     */
    @Override
    public Object readActualHeader(LineIterator reader) {
        return null;
    }

    /**
     * Method isn't used in this implementation
     * @param path to a file
     * @return always true
     */
    @Override
    public boolean canDecode(String path) {
        return true;
    }

    private SegFeature decode(String[] tokens) {
        // The first 3 columns are non optional for SEG.  We will relax this
        // and only require 2.
        if (tokens.length < END_OFFSET) {
            return null;
        }

        String chr = tokens[CHR_OFFSET];
        String id = tokens[ID_OFFSET];

        // The BED format uses a first-base-is-zero convention,  Tribble features use 1 => add 1.
        int start = Integer.parseInt(tokens[START_OFFSET]) + START_OFFSET_VALUE;

        int end = start;
        if (tokens.length > END_OFFSET) {
            end = Integer.parseInt(tokens[END_OFFSET]);
        }

        NggbSegFeature feature = new NggbSegFeature(chr, start, end, id);

        if (tokens.length > NUM_MARK_OFFSET) {
            parseNumMark(tokens[NUM_MARK_OFFSET], feature);
        }
        if (tokens.length > SEG_MEAN_OFFSET) {
            parseSegMean(tokens[SEG_MEAN_OFFSET], feature);
        }

        return feature;
    }

    private void parseSegMean(String token, NggbSegFeature feature) {
        try {
            feature.setSegMean(Float.parseFloat(token));
        } catch (NumberFormatException e) {
            feature.setSegMean(null);
        }
    }

    private void parseNumMark(String token, NggbSegFeature feature) {
        try {
            feature.setNumMark(Integer.parseInt(token));
        } catch (NumberFormatException e) {
            feature.setNumMark(null);
        }
    }
}
