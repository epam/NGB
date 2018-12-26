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

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.exception.BedFileParsingException;
import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.annotation.Strand;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.util.ParsingUtils;

/**
 * Codec for parsing data from the Bed files and conversion it into a {@code NggbBedFeature}
 */
public class NggbBedCodec extends AsciiFeatureCodec<NggbBedFeature> {

    private static final int CHR_OFFSET = 0;
    private static final int START_OFFSET = 1;
    private static final int END_OFFSET = 2;
    private static final int NAME_OFFSET = 3;
    private static final int SCORE_OFFSET = 4;
    private static final int STRAND_OFFSET = 5;
    private static final int THICK_START_OFFSET = 6;
    private static final int THICK_END_OFFSET = 6;
    private static final int COLOUR_OFFSET = 8;
    private static final int BLOCK_COUNT_OFFSET = 9;
    private static final int BLOCK_SIZES_OFFSET = 10;
    private static final int BLOCK_STARTS_OFFSET = 11;
    private static final int ID_OFFSET = 12;
    private static final int DESCRIPTION_OFFSET = 13;
    private final int startOffsetValue;

    private static final Logger LOGGER = LoggerFactory.getLogger(NggbBedCodec.class);

    /**
     * Calls {@link #NggbBedCodec(StartOffset)} with an argument
     * of {@code StartOffset.ONE}
     */
    public NggbBedCodec() {
        this(StartOffset.ONE);
    }

    /**
     * BED format is 0-based, but Tribble is 1-based.
     * Set desired start position at either ZERO or ONE
     */
    public NggbBedCodec(final StartOffset startOffset) {
        super(NggbBedFeature.class);
        this.startOffsetValue = startOffset.value();
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
        NggbSimpleBedFeature feature = getNggbSimpleBedFeature(tokens, tokenCount);
        // The rest of the columns are optional.  Stop parsing upon encountering
        // a non-expected value
        parseAndSetName(tokens, tokenCount, feature);
        parseAndSetScore(tokens, tokenCount, feature);
        parseAndSetStrand(tokens, tokenCount, feature);
        parseAndSetColour(tokens, tokenCount, feature);

        try {
            parseAndSetBlockCount(tokens, tokenCount, feature);
            parseAndSetBlockSizes(tokens, tokenCount, feature);
            parseAndSetBlockStarts(tokens, tokenCount, feature);
        } catch (BedFileParsingException e) {
            LOGGER.trace(MessageHelper.getMessage(MessagesConstants.ERROR_BED_PARSING), e);
            return feature;
        }

        if (tokenCount > ID_OFFSET) {
            feature.setId(tokens[ID_OFFSET]);
        }
        if (tokenCount > DESCRIPTION_OFFSET) {
            feature.setDescription(tokens[DESCRIPTION_OFFSET]);
        }
        return feature;
    }

    private void parseAndSetBlockStarts(String[] tokens, int tokenCount,
            NggbSimpleBedFeature feature) {
        if (tokenCount > BLOCK_STARTS_OFFSET) {
            String[] blockStartsStr = tokens[BLOCK_STARTS_OFFSET].split(",");
            if (blockStartsStr.length > 0) {
                int[] blockStarts = parseIntArray(blockStartsStr);
                feature.setBlockStarts(blockStarts);
            }
        }
    }


    private void parseAndSetBlockSizes(String[] tokens, int tokenCount,
            NggbSimpleBedFeature feature) {
        if (tokenCount > BLOCK_SIZES_OFFSET) {
            String[] blockSizesStr = tokens[BLOCK_SIZES_OFFSET].split(",");
            if (blockSizesStr.length > 0) {
                int[] blockSizes = parseIntArray(blockSizesStr);
                feature.setBlockSizes(blockSizes);
            }
        }
    }

    private int[] parseIntArray(String[] blockStartsStr) {
        int[] blockStarts = new int[blockStartsStr.length];
        for (int i = 0; i < blockStartsStr.length; i++) {
            try {
                blockStarts[i] = Integer.parseInt(blockStartsStr[i]);
            } catch (NumberFormatException e) {
                throw new BedFileParsingException(MessageHelper.getMessage(MessagesConstants.ERROR_BED_PARSING), e);
            }
        }
        return blockStarts;
    }

    private void parseAndSetBlockCount(String[] tokens, int tokenCount,
            NggbSimpleBedFeature feature) {
        if (tokenCount > BLOCK_COUNT_OFFSET) {
            if (NumberUtils.isNumber(tokens[BLOCK_COUNT_OFFSET])) {
                try {
                    feature.setBlockCount(Integer.parseInt(tokens[BLOCK_COUNT_OFFSET]));
                } catch (NumberFormatException e) {
                    throw new BedFileParsingException(MessageHelper.getMessage(MessagesConstants.ERROR_BED_PARSING), e);
                }
            } else {
                throw new BedFileParsingException(MessageHelper.getMessage(MessagesConstants.ERROR_BED_PARSING));
            }
        }
    }

    private void parseAndSetScore(String[] tokens, int tokenCount, NggbSimpleBedFeature feature) {
        if (tokenCount > SCORE_OFFSET && NumberUtils.isNumber(tokens[SCORE_OFFSET])) {
            try {
                float score = Float.parseFloat(tokens[SCORE_OFFSET]);
                feature.setScore(score);
            } catch (NumberFormatException numberFormatException) {
                feature.setScore(Float.NaN);
            }
        }
    }

    private void parseAndSetColour(String[] tokens, int tokenCount, NggbSimpleBedFeature feature) {
        if (tokenCount > COLOUR_OFFSET) {
            String colorString = tokens[COLOUR_OFFSET];
            feature.setColor(ParsingUtils.parseColor(colorString));
            // ThickStart and ThickEnd
            if (NumberUtils.isNumber(tokens[THICK_START_OFFSET])
                    && NumberUtils.isNumber(tokens[THICK_END_OFFSET])) {
                feature.setThickStart(Integer.parseInt(tokens[THICK_START_OFFSET]));
                feature.setThickEnd(Integer.parseInt(tokens[THICK_END_OFFSET]));
            }
        }
    }

    private void parseAndSetStrand(String[] tokens, int tokenCount, NggbSimpleBedFeature feature) {
        if (tokenCount > STRAND_OFFSET) {
            String strandString = tokens[STRAND_OFFSET].trim();
            char strand = strandString.isEmpty() ? ' ' : strandString.charAt(0);

            if (strand == '-') {
                feature.setStrand(Strand.NEGATIVE);
            } else if (strand == '+') {
                feature.setStrand(Strand.POSITIVE);
            } else {
                feature.setStrand(Strand.NONE);
            }
        }
    }

    private void parseAndSetName(String[] tokens, int tokenCount, NggbSimpleBedFeature feature) {
        if (tokenCount > NAME_OFFSET) {
            String name = tokens[NAME_OFFSET].replaceAll("\"", "");
            feature.setName(name);
        }
    }

    @NotNull private NggbSimpleBedFeature getNggbSimpleBedFeature(String[] tokens, int tokenCount) {
        String chr = tokens[CHR_OFFSET];
        // The BED format uses a first-base-is-zero convention,  Tribble features use 1 => add 1.
        int start = Integer.parseInt(tokens[START_OFFSET]) + startOffsetValue;
        int end = start;
        if (tokenCount > 2) {
            end = Integer.parseInt(tokens[END_OFFSET]);
        }
        return new NggbSimpleBedFeature(start, end, chr);
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
