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
package com.epam.ngb.cli.entity.target;

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
public class Target implements Printable<Target> {
    private Long id;
    private String targetName;
    private String owner;
    private TargetType type;
    private List<String> diseases;
    private List<String> products;
    private List<TargetGene> targetGenes;
    /**
     * Calculates a formatting string for a {@code List} of {@code Target} objects
     * for table output. Method calculates the width of columns for {@code Target}'s fields
     * from the length of all items' fields in the list. Printed columns are defined by
     * {@code FieldFormat}, width of each column will be equal to the width of the field with
     * the longest string value.
     * @param table {@code List} of {@code Target} for formatting
     * @return format String to be applied to the {@param table} printing
     */
    @Override
    public String getFormatString(List<Target> table) {
        Map<Target.FieldFormat, Integer> formatMap = new EnumMap<>(Target.FieldFormat.class);
        for (Target.FieldFormat field : Target.FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }
        getItemFormat(table, formatMap);
        return formatMap.values().stream().map(v -> "%" + (v+1) + "s").collect(Collectors.joining());
    }

    /**
     * Creates a String representation of {@code Target} according to the {@param formatString}
     * @param formatString to apply to {@code Target} formatting
     * @return a String representation of {@code Target}
     */
    @Override
    public String formatItem(String formatString) {
        return String.format(formatString,
                id,
                targetName,
                type,
                products,
                diseases
        );
    }

    @Override
    public String formatHeader(String formatString) {
        String[] names = Arrays.stream(Target.FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(formatString, (Object[]) names);
    }

    private void getItemFormat(List<Target> table, Map<Target.FieldFormat, Integer> formatMap) {
        for (Target dbs : table) {
            calculateFieldWidth(formatMap, FieldFormat.TARGET_ID, String.valueOf(dbs.getId()));
            calculateFieldWidth(formatMap, FieldFormat.TARGET_NAME, dbs.getTargetName());
            calculateFieldWidth(formatMap, FieldFormat.TYPE, dbs.getType().name());
            calculateFieldWidth(formatMap, FieldFormat.PRODUCTS, String.valueOf(dbs.getProducts()));
            calculateFieldWidth(formatMap, FieldFormat.DISEASES, String.valueOf(dbs.getDiseases()));
        }
    }

    private void calculateFieldWidth(Map<Target.FieldFormat, Integer> formatMap, Target.FieldFormat field,
                                     String value) {
        if (value == null) {
            return;
        }
        if (formatMap.get(field) < value.length()) {
            formatMap.put(field, value.length());
        }
    }

    /**
     * Represent the fields of the {@code Target}, that will be included in the table for printing
     */
    private enum FieldFormat {
        TARGET_ID,
        TARGET_NAME,
        TYPE,
        PRODUCTS,
        DISEASES;
    }
}
