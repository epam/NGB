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

import com.epam.catgenome.util.NgbFileUtils;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.SortingCollection;
import htsjdk.tribble.readers.AsciiLineReader;

import java.io.*;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractFeatureSorter {

    private static final int ESTIMATED_RECORD_SIZE = 150;
    private static final String UTF_8 = "UTF-8";

    private File inputFile;
    private File outputFile;

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("(.+)(\\.[\\d\\w]+)$");
    private static final Pattern EXTENSION_WITH_GZ_PATTERN = Pattern.compile("(.+)(\\.[\\d\\w]+\\.gz)$");

    private static final String SORTED_SUFFIX = ".sorted";

    /**
     * Directory used for storing temporary data files
     */
    private File tmpDir;

    protected Comparator<SortableRecord> comparator = getDefaultComparator();

    public AbstractFeatureSorter(File inputFile, File outputFile, File tmpDir) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.tmpDir = tmpDir;
    }

    /**
     * @param maxMemory - in megabytes
     * @throws IOException
     *
     */
    public void run(int maxMemory) throws IOException {
        try (
                PrintWriter writer = NgbFileUtils.isGzCompressed(outputFile.getName()) ?
                        new PrintWriter(new OutputStreamWriter(new BlockCompressedOutputStream(outputFile), UTF_8)) :
                        new PrintWriter(outputFile, UTF_8);
                AsciiLineReader reader = NgbFileUtils.isGzCompressed(outputFile.getName()) ?
                        new AsciiLineReader(new BlockCompressedInputStream(inputFile)) :
                        new AsciiLineReader(new FileInputStream(inputFile))
        ) {

            SortableRecordCodec codec = new SortableRecordCodec();

            SortingCollection cltn = SortingCollection.newInstance(
                    SortableRecord.class, codec, comparator, maxMemory * 1024 * 1024 / ESTIMATED_RECORD_SIZE, tmpDir);

            Parser parser = getParser();

            String firstDataRow = writeHeader(reader, writer);
            if (firstDataRow != null) {
                cltn.add(parser.createRecord(firstDataRow));
            }

            SortableRecord next;
            while ((next = parser.readNextRecord(reader)) != null) {
                cltn.add(next);
            }

            CloseableIterator<SortableRecord> iter = cltn.iterator();
            while (iter.hasNext()) {
                SortableRecord al = iter.next();
                writer.println(al.getText());

            }
            iter.close();
        }
    }

    public static Comparator<SortableRecord> getDefaultComparator() {
        return (o1, o2) -> {
            int nameComp = o1.getChromosome().compareTo(o2.getChromosome());
            if (nameComp == 0) {
                return o1.getStart() - o2.getStart();
            } else {
                return nameComp;
            }
        };
    }

    abstract Parser getParser() throws IOException;

    /**
     * Write the header to the output file. Since many readers can't help but read
     * one feature line, that line should be returned and will then be treated as a record
     *
     * @param reader
     * @param writer
     * @return
     * @throws IOException
     */
    abstract String writeHeader(AsciiLineReader reader, PrintWriter writer) throws IOException;

    static boolean isEmptyLine(String line) {
        for (char c : line.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    public static String addSortedSuffix(String fileName) {
        Pattern extensionPattern = EXTENSION_PATTERN;
        if (NgbFileUtils.isGzCompressed(fileName)) {
            extensionPattern = EXTENSION_WITH_GZ_PATTERN;
        }

        Matcher m = extensionPattern.matcher(fileName);

        if (!m.matches()) {
            //No extension case
            return fileName + SORTED_SUFFIX;
        }

        final String beforeExtension = m.group(1);
        final String extension = m.group(2);

        return beforeExtension + SORTED_SUFFIX + extension;
    }
}
