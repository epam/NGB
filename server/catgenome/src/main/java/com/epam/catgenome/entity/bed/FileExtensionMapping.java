/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.entity.bed;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class FileExtensionMapping {

    private final List<String> extensions;
    private final List<ColumnMapping> mapping;

    public FileExtensionMapping(@JsonProperty("mapping") final List<ColumnMapping> mapping,
                                @JsonProperty("extension") final List<String> extensions) {
        this.mapping = mapping;
        this.extensions = extensions;
    }

    public List<ColumnMapping> getMapping() {
        return mapping;
    }

    @Getter
    public static class ColumnMapping {

        private final int index;
        private final String column;
        private final BedColumnCaster cast;

        public ColumnMapping(@JsonProperty("index") final int index,
                             @JsonProperty("column") final String column,
                             @JsonProperty("cast") final BedColumnCaster cast) {
            this.index = index;
            this.column = column;
            this.cast = cast;
        }

        public Object casted(String value) {
            try {
                return cast.cast.apply(value);
            } catch (IllegalArgumentException e) {
                return value;
            }
        }

    }

    public enum BedColumnCaster {
        STRING(v -> v),
        INT(Integer::parseInt),
        FLOAT(Float::parseFloat);

        private final Function<String, Object> cast;

        BedColumnCaster(final Function<String, Object> cast) {
            this.cast = cast;
        }
    }
}
