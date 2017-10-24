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
 * Represents an NGB entity: a file or another data source registered on the NGB server.
 */
public class BiologicalDataItem extends BaseEntity implements Printable<BiologicalDataItem> {

    /**
     * {code bioDataItemId} is an ID used to identify all {@code BiologicalDataItem}, it is unique
     * among all entities
     */

    private Long bioDataItemId;

    /**
     * Type if the data source: file system, url, s3 bucket and any other one supported by the server
     */
    private String type;

    /**
     * Path to the data source, for files it is path to the file on the server, for http resource - URL,
     * and so on
     */
    private String path;

    /**
     * Format of the item, supported by the server and CLI
     */
    private BiologicalDataItemFormat format;

    /**
     * ID of the user, who created the item
     */
    private Long createdBy;

    /**
     * Date of the item creation
     */
    private Date createdDate;

    private SpeciesEntity species;

    public SpeciesEntity getSpecies() {
        return species;
    }

    public void setSpecies(SpeciesEntity species) {
        this.species = species;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public BiologicalDataItemFormat getFormat() {
        return format;
    }

    public void setFormat(BiologicalDataItemFormat format) {
        this.format = format;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Long getBioDataItemId() {
        return bioDataItemId;
    }

    public void setBioDataItemId(Long bioDataItemId) {
        this.bioDataItemId = bioDataItemId;
    }


    //TODO move to a separate class
    /**
     * Calculates a formatting string for a {@code List} of {@code BiologicalDataItem} objects
     * for table output. Method calculates the width of columns for {@code BiologicalDataItem}'s fields
     * from the length of all items' fields in the list. Printed columns are defined by
     * {@code FieldFormat}, width of each column will be equal to the width of the field with
     * the longest string value.
     * @param table {@code List} of {@code BiologicalDataItem} for formatting
     * @return format String to be applied to the {@param table} printing
     */
    @Override
    public String getFormatString(List<BiologicalDataItem> table) {
        Map<FieldFormat, Integer> formatMap = new EnumMap<>(FieldFormat.class);
        for (FieldFormat field : FieldFormat.values()) {
            formatMap.put(field, field.name().length());
        }
        for (BiologicalDataItem item : table) {
            String currentId = String.valueOf(item.getBioDataItemId() == null ? item.getId()
                    : item.getBioDataItemId());
            calculateFieldWidth(formatMap, FieldFormat.BIO_DATA_ITEM_ID, currentId);
            calculateFieldWidth(formatMap, FieldFormat.ID, String.valueOf(item.getId()));
            calculateFieldWidth(formatMap, FieldFormat.NAME, item.getName());
            calculateFieldWidth(formatMap, FieldFormat.TYPE, item.getType());
            calculateFieldWidth(formatMap, FieldFormat.PATH, item.getPath());
            calculateFieldWidth(formatMap, FieldFormat.FORMAT, item.getFormat().name());
            calculateFieldWidth(formatMap, FieldFormat.CREATED_BY, String.valueOf(item.getCreatedBy()));
            calculateFieldWidth(formatMap, FieldFormat.CREATED_DATE, DATE_FORMAT.format(item.getCreatedDate()));
            calculateFieldWidth(formatMap, FieldFormat.SPECIES_VERSION,
                item.getSpecies() == null ? "" : item.getSpecies().getVersion());
        }
        return formatMap.values().stream().map(v -> "%" + (v + 1) + "s").collect(Collectors.joining());
    }
    /**
     * Creates a String representation of {@code BiologicalDataItem} according to the {@param formatString}
     * @param formatString to apply to {@code BiologicalDataItem} formatting
     * @return a String representation of {@code BiologicalDataItem}
     */
    @Override
    public String formatItem(String formatString) {
        String idStr = String.valueOf(bioDataItemId == null ? getId() : bioDataItemId);
        return String.format(formatString, idStr, String.valueOf(getId()), getName(), type,
                path, format.name(),
                String.valueOf(createdBy), DATE_FORMAT.format(createdDate), species == null ? "" : species.getVersion());
    }

    /**
     * Creates a String representation of a table header with the names of printed columns.
     * Printed columns are defined by {@code FieldFormat}.
     * @param format to apply to header formatting
     * @return a String representation of a table header
     */
    @Override public String formatHeader(String format) {
        String[] names = Arrays.stream(FieldFormat.values()).map(Enum::name).toArray(String[]::new);
        return String.format(format, (Object[]) names);
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
     * Describes the fields that should be included into a table for printing {@code BiologicalDataItem}
     */
    private enum FieldFormat {
        BIO_DATA_ITEM_ID,
        ID,
        NAME,
        TYPE,
        PATH,
        FORMAT,
        CREATED_BY,
        CREATED_DATE,
        SPECIES_VERSION
    }
}
