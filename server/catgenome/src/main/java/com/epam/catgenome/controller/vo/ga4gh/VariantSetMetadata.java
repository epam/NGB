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

package com.epam.catgenome.controller.vo.ga4gh;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>
 * Class for data from GA4GH (variant set metadata)
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariantSetMetadata {

    /**
     * {@code String} The top-level key
     */
    private String key;

    /**
     * {@code String} The value field for simple metadata
     */
    private String value;

    /**
     * {@code String} User-provided ID field, not enforced by this API.
     * Two or more pieces of structured metadata with identical id and key fields are considered equivalent.
     */
    private String id;

    /**
     * {@code enum} The type of data. Possible types include: Integer, Float, Flag, Character, and String.
     */
    private Type type;

    /**
     * {@code String} The number of values that can be included in a field described by this metadata.
     */
    private String number;

    /**
     * {@code String} A textual description of this metadata.
     */
    private String description;

    /**
     * {@code map} Remaining structured metadata key-value pairs.
     * This must be of the form map<string, string[]> (string key mapping to a list of string values).
     */
    private Map<String, ArrayList<String>> info;

    public VariantSetMetadata() {
        //no-op
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, ArrayList<String>> getInfo() {
        return info;
    }

    public void setInfo(Map<String, ArrayList<String>> info) {
        this.info = info;
    }


    enum Type {
        TYPE_UNSPECIFIED,
        INTEGER,
        FLOAT,
        FLAG,
        CHARACTER,
        STRING
    }

}