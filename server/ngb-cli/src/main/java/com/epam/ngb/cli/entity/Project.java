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

package com.epam.ngb.cli.entity;

import static com.epam.ngb.cli.manager.printer.AbstractResultPrinter.DATE_FORMAT;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.epam.ngb.cli.manager.printer.Printable;

/**
 * {@code {@link Project}} is an entity,  representing a dataset (ex. project) on NGB server.
 * Dataset is a folder for a set of files, grouped by one sample, experiment or some other feature.
 * Also datasets may correspond to file organization in the file system and work just as directories.
 * Datasets support hierarchical organization and may include other datasets as nested projects.
 */
public class Project extends BaseEntity implements Printable<Project>, RequestPayload {

    /**
     * Data, when the dataset was created
     */
    private Date createdDate;
    /**
     * List of files, included in the dataset. Al least one reference file is required for each dataset.
     */
    private List<BiologicalDataItem> items;
    private Integer itemsCount;

    /**
     * Number of files in the dataset
     */
    private List<ProjectNote> notes;
    /**
     * A helper {@code Map}, that stores information on number of files of each file format, present
     * in the dataset
     */
    private Map<BiologicalDataItemFormat, Integer> itemsCountPerFormat;
    /**
     * Date, when the project was last time opened (requested) by the client
     */
    private Date lastOpenedDate;
    /**
     * {@code List} of nested {@code Projects} included in this dataset
     */
    private List<Project> nestedProjects;

    /**
     * ID of a parent dataset, Null for top-level datasets
     */
    private Long parentId;

    public Date getCreatedDate() {
        return createdDate;
    }

    public List<Project> getNestedProjects() {
        return nestedProjects;
    }

    public void setNestedProjects(List<Project> nestedProjects) {
        this.nestedProjects = nestedProjects;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public List<BiologicalDataItem> getItems() {
        return items;
    }

    public void setItems(List<BiologicalDataItem> items) {
        this.items = items;
    }

    public Integer getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Integer itemsCount) {
        this.itemsCount = itemsCount;
    }

    public List<ProjectNote> getNotes() {
        return notes;
    }

    public void setNotes(List<ProjectNote> notes) {
        this.notes = notes;
    }

    public Map<BiologicalDataItemFormat, Integer> getItemsCountPerFormat() {
        return itemsCountPerFormat;
    }

    public void setItemsCountPerFormat(Map<BiologicalDataItemFormat, Integer> itemsCountPerFormat) {
        this.itemsCountPerFormat = itemsCountPerFormat;
    }

    public Date getLastOpenedDate() {
        return lastOpenedDate;
    }

    public void setLastOpenedDate(Date lastOpenedDate) {
        this.lastOpenedDate = lastOpenedDate;
    }

    //TODO move to a separate class

    /**
     * Calculates a formatting string for a {@code List} of {@code Project} objects
     * for table output. Method calculates the width of columns for {@code Project}'s fields
     * from the length of all items' fields in the list. Printed columns are defined by
     * {@code FieldFormat}, width of each column will be equal to the width of the field with
     * the longest string value.
     * @param table {@code List} of {@code Project} for formatting
     * @return format String to be applied to the {@param table} printing
     */
    @Override public String getFormatString(List<Project> table) {
        Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
        for (FieldFormat field : FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }
        getItemFormat(table, formatMap);
        return formatMap.values().stream().map(v -> "%" + (v + 1) + "s").collect(Collectors.joining());
    }

    private void getItemFormat(List<Project> table, Map<FieldFormat, Integer> formatMap) {
        for (Project item : table) {
            calculateFieldWidth(formatMap, FieldFormat.ID, String.valueOf(item.getId()));
            calculateFieldWidth(formatMap, FieldFormat.PARENT_ID, item.getParentId() == null ? "" :
                    String.valueOf(item.getParentId()));
            calculateFieldWidth(formatMap, FieldFormat.NAME, item.getName());
            calculateFieldWidth(formatMap, FieldFormat.CREATED_DATE, DATE_FORMAT.format(item.getCreatedDate()));
            calculateFieldWidth(formatMap, FieldFormat.ITEMS_COUNT, String.valueOf(itemsCount));
            calculateFieldWidth(formatMap, FieldFormat.LAST_OPENED_DATE, DATE_FORMAT.format(lastOpenedDate));
            if (item.getNestedProjects() != null) {
                getItemFormat(item.getNestedProjects(), formatMap);
            }
        }
    }

    /**
     * Creates a String representation of {@code Project} according to the {@param formatString}
     * @param formatString to apply to {@code Project} formatting
     * @return a String representation of {@code Project}
     */
    @Override public String formatItem(String formatString) {
        StringBuilder result = new StringBuilder();
        result.append(String.format(formatString,
                String.valueOf(getId()),
                getParentId() == null ? "" : String.valueOf(getParentId()), getName(),
                DATE_FORMAT.format(createdDate),
                String.valueOf(calculateItemsCount()),
                DATE_FORMAT.format(lastOpenedDate)));
        if (getNestedProjects() != null) {
            getNestedProjects().forEach(p -> result.append("\n").append(p.formatItem(formatString)));
        }
        return result.toString();
    }

    private Integer calculateItemsCount() {
        if (itemsCount != null) {
            return itemsCount;
        }
        if (items == null || items.size() <= 1) {
            return 0;
        }
        return items.size() - 1;
    }

    /**
     * Creates a String representation of a table header with the names of printed columns.
     * Printed columns are defined by {@code FieldFormat}.
     * @param formatString to apply to header formatting
     * @return a String representation of a table header
     */
    @Override public String formatHeader(String formatString) {
        String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(formatString, (Object[]) names);
    }

    private void calculateFieldWidth(Map<FieldFormat, Integer> formatMap, FieldFormat field, String value) {
        if (value == null) {
            return;
        }
        if (formatMap.get(field) < value.length()) {
            formatMap.put(field, value.length());
        }
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * Represent the fields of the {@code Project}, that will be included in the table for printing
     */
    private enum FieldFormat {
        ID,
        PARENT_ID,
        NAME,
        CREATED_DATE,
        ITEMS_COUNT,
        LAST_OPENED_DATE
    }
}
