/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.ngb.cli.entity.coverage;

import com.epam.ngb.cli.entity.RequestPayload;
import com.epam.ngb.cli.manager.printer.Printable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Setter
@Getter
@Builder
public class BamCoverage implements RequestPayload, Printable<BamCoverage> {
    private Long coverageId;
    private Long bamId;
    private Integer step;
    private Float coverage;

    /**
     * Calculates a formatting string for a {@code List} of {@code BamCoverage} objects
     * for table output. Method calculates the width of columns for {@code BamCoverage}'s fields
     * from the length of all items' fields in the list. Printed columns are defined by
     * {@code FieldFormat}, width of each column will be equal to the width of the field with
     * the longest string value.
     * @param table {@code List} of {@code BamCoverage} for formatting
     * @return format String to be applied to the {@param table} printing
     */
    @Override
    public String getFormatString(List<BamCoverage> table) {
        Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
        for (FieldFormat field : FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }
        getItemFormat(table, formatMap);
        return formatMap.values().stream().map(v -> "%" + (v+1) + "s").collect(Collectors.joining());
    }

    /**
     * Creates a String representation of {@code BamCoverage} according to the {@param formatString}
     * @param formatString to apply to {@code BamCoverage} formatting
     * @return a String representation of {@code BamCoverage}
     */
    @Override
    public String formatItem(String formatString) {
        return String.format(formatString,
                coverageId,
                bamId,
                step,
                coverage
        );
    }

    @Override
    public String formatHeader(String formatString) {
        String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(formatString, (Object[]) names);
    }

    private void getItemFormat(List<BamCoverage> table, Map<FieldFormat, Integer> formatMap) {
        for (BamCoverage dbs : table) {
            calculateFieldWidth(formatMap, FieldFormat.COVERAGE_ID, String.valueOf(dbs.getCoverageId()));
            calculateFieldWidth(formatMap, FieldFormat.BAM_ID, String.valueOf(dbs.getBamId()));
            calculateFieldWidth(formatMap, FieldFormat.STEP, String.valueOf(dbs.getStep()));
            calculateFieldWidth(formatMap, FieldFormat.COVERAGE, String.valueOf(dbs.getCoverage()));
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
     * Represent the fields of the {@code BamCoverage}, that will be included in the table for printing
     */
    private enum FieldFormat {
        COVERAGE_ID,
        BAM_ID,
        STEP,
        COVERAGE;
    }
}
