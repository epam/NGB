/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.ngb.cli.entity;

import com.epam.ngb.cli.manager.printer.Printable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an container that contains permissions info for NGB entity to print.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionsContainer implements Printable<PermissionsContainer> {

    private boolean isPrincipal;
    private String userName;
    private String prettyMask;

    @Override
    public String getFormatString(final List<PermissionsContainer> table) {
        Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
        for (FieldFormat field : FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }

        table.forEach(line -> {
            calculateFieldWidth(formatMap, FieldFormat.SID_NAME, line.getUserName());
            calculateFieldWidth(formatMap, FieldFormat.PERMISSIONS, line.getPrettyMask());
        });

        return formatMap.values().stream().map(v -> "%" + (v + 1) + "s").collect(Collectors.joining());
    }

    @Override
    public String formatItem(final String format) {
        return String.format(format, userName, prettyMask);
    }

    @Override
    public String formatHeader(String format) {
        String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(format, (Object[]) names);
    }

    private enum FieldFormat {
        SID_NAME,
        PERMISSIONS
    }

    private static void calculateFieldWidth(Map<FieldFormat, Integer> formatMap, FieldFormat field, String value) {
        if (value == null) {
            return;
        }
        if (formatMap.get(field) < value.length()) {
            formatMap.put(field, value.length());
        }
    }
}
