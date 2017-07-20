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

package com.epam.catgenome.manager.wig.reader;

import htsjdk.tribble.AsciiFeatureCodec;
import htsjdk.tribble.readers.LineIterator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Codec for parsing file with BED GRAPH format. Codec decodes {@link BedGraphFeature}
 * from file line.
 */
public class BedGraphCodec extends AsciiFeatureCodec<BedGraphFeature> {

    private static final List<String> BED_GRAPH_EXTENSIONS = new ArrayList<>();
    private static final String COMMENT_LINE = "#";
    private static final String TRACK_LINE = "track";
    private static final String BROWSER_LINE = "browser";
    public static final String TYPE_LINE = "type";

    static {
        BED_GRAPH_EXTENSIONS.add(".bg");
        BED_GRAPH_EXTENSIONS.add(".bg.gz");
        BED_GRAPH_EXTENSIONS.add(".bdg");
        BED_GRAPH_EXTENSIONS.add(".bdg.gz");
        BED_GRAPH_EXTENSIONS.add(".bedGraph");
        BED_GRAPH_EXTENSIONS.add(".bedGraph.gz");
    }

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\t|( +)");

    public BedGraphCodec() {
        super(BedGraphFeature.class);
    }

    /**
     * Creates {@link BedGraphFeature} from file line
     * @param line to decode
     * @return parsed {@link BedGraphFeature}
     */
    @Override
    public BedGraphFeature decode(String line) {

        if (line.trim().isEmpty()) {
            return null;
        }

        if (line.startsWith(COMMENT_LINE) || line.startsWith(TRACK_LINE) || line.startsWith(BROWSER_LINE)) {
            return null;
        }

        String[] tokens = SPLIT_PATTERN.split(line, -1);
        Assert.isTrue(tokens.length == 4);
        return new BedGraphFeature(
                tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Float.parseFloat(tokens[3])
        );
    }

    /**
     * This implementation doesn't parse header
     * @param reader for a file
     * @return always null
     */
    @Override
    public Object readActualHeader(LineIterator reader) {
        while (reader.hasNext() &&  reader.peek().isEmpty() || reader.peek().startsWith(COMMENT_LINE)
                || reader.peek().startsWith(TYPE_LINE)|| reader.peek().startsWith(TRACK_LINE) ||
                reader.peek().startsWith(BROWSER_LINE)) {
            reader.next();
        }
        return null;
    }

    /**
     * Check that the codec can decode provided file
     * @param path to the file
     * @return true if the file extension one of the bed graph extensions
     */
    @Override
    public boolean canDecode(String path) {
        return BED_GRAPH_EXTENSIONS.stream().anyMatch(path::endsWith);
    }
}
