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

package com.epam.catgenome.manager.bed.parser;

import com.epam.catgenome.entity.bed.FileExtension.BedColumnMapping;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Codec for parsing data from the Bed files and conversion it into a {@code NggbBedFeature}
 */
public class NggbMultiFormatBedCodec extends AsciiFeatureCodec<NggbBedFeature> {

    private static final int CHR_OFFSET = 0;
    private static final int START_OFFSET = 1;
    private static final int END_OFFSET = 2;
    private final int startOffsetValue;
    private final List<BedColumnMapping> mapping;

    private static final Logger LOGGER = LoggerFactory.getLogger(NggbMultiFormatBedCodec.class);

    /**
     * Calls {@link #NggbMultiFormatBedCodec(StartOffset, List)} with an argument
     * of {@code StartOffset.ONE}
     * @param mapping
     */
    public NggbMultiFormatBedCodec(final List<BedColumnMapping> mapping) {
        this(StartOffset.ONE, mapping);
    }

    /**
     * BED format is 0-based, but Tribble is 1-based.
     * Set desired start position at either ZERO or ONE
     */
    public NggbMultiFormatBedCodec(final StartOffset startOffset,
                                   final List<BedColumnMapping> mapping) {
        super(NggbBedFeature.class);
        this.startOffsetValue = startOffset.value();
        this.mapping = mapping;
    }

    /**
     * Parses Bed file line, creates and initializes {@code NggbBedFeature}
     * @param line from a Bed file
     * @return parsed {@code NggbBedFeature}
     */
    @Override
    public NggbBedFeature decode(String line) {
        if (line.trim().isEmpty()) {
            return null;
        }
        if (line.startsWith("#") || line.startsWith("track") || line.startsWith("browser")) {
            this.readHeaderLine();
            return null;
        }
        String[] tokens = line.split("\t");
        return decode(tokens);
    }

    /**
     * This implementation doesn't read file header
     * @param reader
     * @return always null
     */
    @Override
    public Object readActualHeader(LineIterator reader) {
        return null;
    }

    /**
     * @param path to a file
     * @return true if input file has '*.bed' extension, otherwise false
     */
    @Override
    public boolean canDecode(final String path) {
        return path.toLowerCase().endsWith(".bed");
    }

    public int getStartOffset() {
        return this.startOffsetValue;
    }

    private NggbBedFeature decode(String[] tokens) {
        int tokenCount = tokens.length;
        // The first 3 columns are non optional for BED.  We will relax this
        // and only require 2.
        if (tokenCount < 2) {
            return null;
        }
        NggbMultiFormatBedFeature feature = getNggbMultiFormatBedFeature(tokens, tokenCount);
        mapping.forEach(column -> {
            if (column.getIndex() > tokens.length - 1) {
                throw new IllegalArgumentException(String.format(
                        "MultiFormat BED record doesn't have token with index: %d", column.getIndex())
                );
            }
            feature.additional.put(column.getColumn(), tokens[column.getIndex()]);
        });
        return feature;
    }

    @NotNull private NggbMultiFormatBedFeature getNggbMultiFormatBedFeature(String[] tokens, int tokenCount) {
        String chr = tokens[CHR_OFFSET];
        // The BED format uses a first-base-is-zero convention,  Tribble features use 1 => add 1.
        int start = Integer.parseInt(tokens[START_OFFSET]) + startOffsetValue;
        int end = start;
        if (tokenCount > 2) {
            end = Integer.parseInt(tokens[END_OFFSET]);
        }
        return new NggbMultiFormatBedFeature(start, end, chr);
    }

    private boolean readHeaderLine() {
        //We don't parse BED header
        return false;
    }

    /**
     * Indicate whether co-ordinates or 0-based or 1-based.
     * <p/>
     * Tribble uses 1-based, BED files use 0.
     * e.g.:
     * start_position = bedline_start_position - startIndex.value()
     */
    public enum StartOffset {
        ZERO(0),
        ONE(1);
        private int start;

        StartOffset(int start) {
            this.start = start;
        }

        public int value() {
            return this.start;
        }
    }
}
