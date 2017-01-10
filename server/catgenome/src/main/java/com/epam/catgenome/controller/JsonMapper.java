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

package com.epam.catgenome.controller;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Source:      JsonMapper.java
 * Created:     10/2/15, 2:29 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code JsonMapper} extends {@code ObjectMapper},  which according to documentation provides functionality
 * for converting between Java objects (instances of JDK provided core classes, beans), and matching JSON
 * constructs. It'll use instances of {@code JsonParser} and {@code JsonGenerator} for implementing actual
 * reading/writing of JSON.
 * <p>
 * {@code JsonMapper} doesn't actually extend functionality of {@code ObjectMapper}, but configures it in
 * the way different from the default one, e.g. enforces that {@code java.util.Date} will be serialized in
 * the desired format etc.
 *
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
public class JsonMapper extends ObjectMapper {

    private static final long serialVersionUID = -1414537788709027470L;

    /**
     * {@code String} specifies date format without offset used to serialize
     * or deserialize dates with Jackson
     */
    private static final String FMT_ISO_LOCAL_DATE = "yyyy-MM-dd";

    public JsonMapper() {
        // calls the default constructor
        super();

        // configures ISO8601 formatter for date without time zone
        // the used format is 'yyyy-MM-dd'
        super.setDateFormat(new SimpleDateFormat(FMT_ISO_LOCAL_DATE));

        // enforces to skip null and empty values in the serialized JSON output
        super.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // enforces to skip null references in the serialized output of Map
        super.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        // enables serialization failures, when mapper encounters unknown properties names
        super.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        // configures the format to prevent writing of the serialized output for java.util.Date
        // instances as timestamps. any date should be written in ISO format
        super.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

}
