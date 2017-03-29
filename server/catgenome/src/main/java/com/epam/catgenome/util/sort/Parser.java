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

package com.epam.catgenome.util.sort;

import htsjdk.tribble.readers.AsciiLineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

import static com.epam.catgenome.util.sort.AbstractFeatureSorter.isEmptyLine;


public class Parser {

    private static final Pattern TAB_PATTERN = Pattern.compile("\t");

    private int chrCol;
    private int startCol;
    private String commentPrefix;

    private static final Logger LOG = LoggerFactory.getLogger(SortableRecordCodec.class);

    public Parser(int chrCol, int startCol) {
        this.chrCol = chrCol;
        this.startCol = startCol;
        this.commentPrefix = "#";
    }

    public SortableRecord readNextRecord(AsciiLineReader reader) {
        String nextLine;
        try {
            nextLine = reader.readLine();
        } catch (IOException e) {
            LOG.error("Error reading line: {}", e);
            return null;
        }
        if (nextLine == null) {
            return null;
        } else if (isEmptyLine(nextLine) || nextLine.startsWith(commentPrefix)) {
            return readNextRecord(reader);
        }

        try {
            return createRecord(nextLine);
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Error creating record: {}", e);
            throw e;
        }
    }

    public SortableRecord createRecord(String nextLine) {
        String[] fields = TAB_PATTERN.split(nextLine, -1);

        String chr = fields[chrCol];

        int start;
        try {
            start = Integer.parseInt(fields[startCol].trim());
        } catch (NumberFormatException e) {
            start = Integer.MAX_VALUE;
        }

        return new SortableRecord(chr, start, nextLine);
    }
}
