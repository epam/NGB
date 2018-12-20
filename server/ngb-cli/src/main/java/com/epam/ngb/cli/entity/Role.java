/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.ngb.cli.entity;

import com.epam.ngb.cli.manager.printer.Printable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class Role implements Printable<Role> {

    public static final String ROLE_PREFIX = "ROLE_";

    private Long id;
    private String name;
    private boolean predefined;
    private boolean userDefault;

    public Role() {
        this.predefined = false;
        this.userDefault = false;
    }

    public Role(String name) {
        this();
        this.name = name;
    }

    public Role(Long id, String name) {
        this(name);
        this.id = id;
    }

    @Override
    public String getFormatString(List<Role> table) {
        Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
        for (FieldFormat field : FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }
        getItemFormat(table, formatMap);
        return formatMap.values().stream().map(v -> "%" + (v + 1) + "s").collect(Collectors.joining());
    }

    private void getItemFormat(List<Role> table, Map<FieldFormat, Integer> formatMap) {
        for (Role item : table) {
            calculateFieldWidth(formatMap, FieldFormat.ID, String.valueOf(item.getId()));
            calculateFieldWidth(formatMap, FieldFormat.NAME, item.getName());
            calculateFieldWidth(formatMap, FieldFormat.PREDEFINED, Boolean.FALSE.toString());
            calculateFieldWidth(formatMap, FieldFormat.USER_DEFAULT, Boolean.FALSE.toString());
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

    @Override
    public String formatItem(String format) {
        return String.format(format, String.valueOf(getId()), getName(),
                isPredefined(), isUserDefault());
    }

    @Override
    public String formatHeader(String format) {
        String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(format, (Object[]) names);
    }

    private enum FieldFormat {
        ID,
        NAME,
        PREDEFINED,
        USER_DEFAULT
    }
}
