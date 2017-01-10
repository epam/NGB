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

package com.epam.catgenome.manager.reference.io.cytoband;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.entity.reference.cytoband.Cytoband;
import com.epam.catgenome.entity.reference.cytoband.GiemsaStain;
import com.epam.catgenome.util.IOHelper;

/**
 * Source:      CytobandReader.java
 * Created:     11/24/15, 6:55 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code CytobandReader} represents a reader that's designed to read and parse
 * file that describe cytobands.
 * <p>
 * More information describing format of a file with  cytobands you can follow
 * <a href="https://www.broadinstitute.org/igv/Cytoband">this link</a>.
 */
public final class CytobandReader {

    private static final Logger LOG = LoggerFactory.getLogger(CytobandReader.class);

    /**
     * {@code String} represents a separator that according to the specification
     * should be used to separate different data columns from each other.
     */
    private static final String COLUMN_SEPARATOR = "\t";

    /**
     * {@code Set} specifies all supported file formats that can be used to specify cytobands.
     */
    private static final Set<String> CYTOBAND_EXTENSIONS = new HashSet<>();
    static {
        CYTOBAND_EXTENSIONS.add(".txt");
        CYTOBAND_EXTENSIONS.add(".txt.gz");
    }

    /**
     * {@code Set} defines collection of names for chromosomes that are expected to
     * be found in the provided input. In fact this collection is used as the dictionary
     * to switch parser to the strict mode.
     * <p>
     * However it's optional and if the dictionary is provided, it automatically switches
     * a parser to <tt>strict</tt> mode, otherwise validation
     * <p>
     * In cases when parser run in the strict mode encounters illegal chromosome name
     * (it means name can't be found in the dictionary), it generates and an exception
     * immediately.
     */
    private final Set<String> chromosomes;

    /**
     * {@code Map} defines map that describes mapping between chromosome's name and
     * all related cytobands that were extracted from the provided file.
     */
    private final Map<String, CytobandRecord> records;

    private CytobandReader(final File file, final Set<String> chromosomes) throws IOException {
        if (!isSupported(file.getName())) {
            throw new IllegalArgumentException(MessageHelper.getMessage("error.cytobands.illegal.file.type",
                                                                        StringUtils.join(CYTOBAND_EXTENSIONS, ", ")));
        }
        records = new HashMap<>();
        this.chromosomes = chromosomes;
        parse(file);
    }

    /**
     * Returns {@code CytobandRecord} associated with the given chromosome's name. If no
     * such record is found, it returns <tt>null</tt>.
     *
     * @param chromosome {@code String} specifies a name for chromosome which cytobands
     *                   should be returned
     * @return {@code CytobandRecord} that provides data about all bands that are available
     * for a chromosome with the given name or <tt>null</tt>
     */
    public CytobandRecord getRecord(final String chromosome) {
        return records.get(chromosome);
    }

    /**
     * Creates a new instance of {@code CytobandReader} based on the given {@code File} and
     * parses it.
     * <p>
     * A parser will be run without chromosomes' names validation, that means it returns all
     * bands that are specified in it.
     *
     * @param file {@code File} represents a reference on *.txt or *.txt.gz file that contain
     *             definitions for cytobands
     * @return {@code CytobandReader} represents a reference on a reader
     * @throws IOException              will be thrown if any I/O errors occur
     * @throws IllegalArgumentException will be thrown in cases when a given file isn't well-formed or
     *                                  there is at least one record that doesn't provide mandatory values
     */
    public static CytobandReader getInstance(final File file) throws IOException {
        return getInstance(file, null);
    }

    /**
     * Creates a new instance of {@code CytobandReader} based on the given {@code File} and
     * parses it.
     * <p>
     * To run a parser in the strict mode, you can provide {@code Collection} with chromosomes'
     * names that are expected to be found in the given input.
     *
     * @param file        {@code File} represents a reference on *.txt or *.txt.gz file that contain
     *                    definitions for cytobands
     * @param chromosomes {@code Collection} specifies valid chromosomes' names that are expected to
     *                    be encountered in the given input; if it contains at least
     *                    one value a parser will be run in the strict mode
     * @return {@code CytobandReader} represents a reference on a reader
     * @throws IOException              will be thrown if any I/O errors occur
     * @throws IllegalArgumentException will be thrown in cases when a given file isn't well-formed or
     *                                  there is at least one record that doesn't provide mandatory values
     *                                  or specifies illegal chromosome's name
     */
    public static CytobandReader getInstance(final File file, final Set<String> chromosomes) throws IOException {
        return new CytobandReader(file, chromosomes);
    }

    private boolean isSupported(final String fn) {
        for (String ext : CYTOBAND_EXTENSIONS) {
            if (fn.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void parse(final File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(IOHelper.openStream(file),
                Charset.defaultCharset()))) {
            String row;
            int lineNum = 1;
            // strict is TRUE means that parser should validate chromosome's name extracted from the given
            // input using the provided dictionary that specifies all valid names for chromosomes
            final boolean strict = CollectionUtils.isNotEmpty(chromosomes);
            while ((row = reader.readLine()) != null) {
                // get all columns that provides required data
                String[] columns = row.split(COLUMN_SEPARATOR);
                if (columns.length != Cols.NCOLS.index()) {
                    throw new IllegalArgumentException(MessageHelper.getMessage(
                        "error.cytobands.unexpected.row.cols", lineNum));
                }

                // validates name for a chromosome and specifies a record to handle its band; if
                // it's necessary a new record will be created and added to the result
                final String chromosome = getText(columns, Cols.CHROM);
                if (strict && !chromosomes.contains(chromosome)) {
                    throw new IllegalArgumentException(MessageHelper.getMessage("error.cytobands.illegal.chromosome",
                            chromosome, lineNum));
                }
                CytobandRecord record = records.get(chromosome);
                if (record == null) {
                    record = new CytobandRecord(chromosome);
                    records.put(chromosome, record);
                }

                // parses other columns and fills in information about a band
                final Cytoband band = new Cytoband();
                band.setChromosome(chromosome);
                band.setName(getText(columns, Cols.BAND_NAME));
                band.setGiemsaStain(getGiemsaStain(columns));
                band.setEndIndex(getInt(columns, Cols.END_INDEX, lineNum));
                band.setStartIndex(getInt(columns, Cols.START_INDEX, lineNum));

                record.getBands().add(band);
                lineNum++;
            }
        }
    }

    private GiemsaStain getGiemsaStain(final String[] columns) {
        final String txtValue = getText(columns, Cols.GIEMSA_STAIN);
        try {
            return GiemsaStain.valueOf(txtValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.error("Error while getting GiemsaStain", e);
            // by default "N/A" value should be used that indicates cases for unsupported or
            // omitted Giemsa stain values
            return GiemsaStain.NA;
        }
    }

    private String getText(final String[] columns, final Cols col) {
        return StringUtils.trimToEmpty(columns[col.index()]);
    }

    private Integer getInt(final String[] columns, final Cols col, final int lineNum) {
        try {
            return Integer.parseInt(getText(columns, col));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MessageHelper.getMessage("error.cytobands.illegal.number.format",
                    col.name(), lineNum), e);
        }
    }

    /**
     * {@code Cols} specifies indexes of columns that are expected to be found in
     * each line of the given file with cytobands.
     */
    enum Cols {

        // chromosome's name
        CHROM(0),
        // beginning position for a band in chromosome's coordinates
        START_INDEX(1),
        // ending position for a band in chromosome's coordinates
        END_INDEX(2),
        // name for a band
        BAND_NAME(3),
        // Giemsa stain value for a band
        GIEMSA_STAIN(4),
        // total number of columns in each line
        NCOLS(5);

        private int idx;

        Cols(final int idx) {
            this.idx = idx;
        }

        public int index() {
            return idx;
        }

    }
}
