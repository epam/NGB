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

package com.epam.ngb.cli.manager;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * {@code JsonMapper} is a service class, providing a configured mapper for
 * JSON (de)serialization.
 */
public final class JsonMapper extends ObjectMapper {

    private static final long serialVersionUID = -1414537788709027470L;
    private static final JsonMapper MAPPER = new JsonMapper();

    /**
     * {@code String} specifies date format without offset used to serialize
     * or deserialize dates with Jackson
     */
    private static final String FMT_ISO_LOCAL_DATE = "yyyy-MM-dd";

    private JsonMapper() {
        // calls the default constructor
        super();

        // configures ISO8601 formatter for date without time zone
        // the used format is 'yyyy-MM-dd'
        setDateFormat(new SimpleDateFormat(FMT_ISO_LOCAL_DATE));

        // enforces to skip null and empty values in the serialized JSON output
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // enforces to skip null references in the serialized output of map
        configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        // enables serialization failures, when mapper encounters unknown properties names
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // configures the format to prevent writing of the serialized output for dates
        // instances as timestamps; any date should be written in ISO format
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * @return a {@code JsonMapper} instance
     */
    public static JsonMapper getMapper() {
        return MAPPER;
    }
}
