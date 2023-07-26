/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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

package com.epam.ngb.cli.entity.pdb;

import com.epam.ngb.cli.manager.printer.Printable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class PdbFile implements Printable<PdbFile> {

    private String geneId;
    private Long pdbFileId;
    private Long bioDataItemId;
    private String name;
    private String prettyName;
    private String path;

    /**
     * Calculates a formatting string for a {@code List} of {@code PdbFile} objects
     * for table output. Method calculates the width of columns for {@code PdbFile}'s fields
     * from the length of all items' fields in the list. Printed columns are defined by
     * {@code FieldFormat}, width of each column will be equal to the width of the field with
     * the longest string value.
     * @param table {@code List} of {@code PdbFile} for formatting
     * @return format String to be applied to the {@param table} printing
     */
    @Override
    public String getFormatString(List<PdbFile> table) {
        Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
        for (FieldFormat field : FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }
        getItemFormat(table, formatMap);
        return formatMap.values().stream().map(v -> "%" + (v+1) + "s").collect(Collectors.joining());
    }

    /**
     * Creates a String representation of {@code PdbFile} according to the {@param formatString}
     * @param formatString to apply to {@code PdbFile} formatting
     * @return a String representation of {@code PdbFile}
     */
    @Override
    public String formatItem(String formatString) {
        return String.format(formatString,
                pdbFileId,
                bioDataItemId,
                geneId,
                name,
                prettyName,
                path
        );
    }

    @Override
    public String formatHeader(String formatString) {
        String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(formatString, (Object[]) names);
    }

    private void getItemFormat(List<PdbFile> table, Map<FieldFormat, Integer> formatMap) {
        for (PdbFile dbs : table) {
            calculateFieldWidth(formatMap, FieldFormat.PDB_FILE_ID, String.valueOf(dbs.getPdbFileId()));
            calculateFieldWidth(formatMap, FieldFormat.BIO_DATA_ITEM_ID, String.valueOf(dbs.getBioDataItemId()));
            calculateFieldWidth(formatMap, FieldFormat.GENE_ID, dbs.getGeneId());
            calculateFieldWidth(formatMap, FieldFormat.NAME, dbs.getName());
            calculateFieldWidth(formatMap, FieldFormat.PRETTY_NAME, dbs.getPrettyName());
            calculateFieldWidth(formatMap, FieldFormat.PATH, dbs.getPath());
        }
    }

    private void calculateFieldWidth(Map<FieldFormat, Integer> formatMap, FieldFormat field, String value) {
        if (value == null) {
            return;
        }
        if (formatMap.get(field) < value.length()) {
            formatMap.put(field, value.length());
        }
    }

    /**
     * Represent the fields of the {@code Lineage Tree}, that will be included in the table for printing
     */
    private enum FieldFormat {
        PDB_FILE_ID,
        BIO_DATA_ITEM_ID,
        GENE_ID,
        NAME,
        PRETTY_NAME,
        PATH;
    }
}
