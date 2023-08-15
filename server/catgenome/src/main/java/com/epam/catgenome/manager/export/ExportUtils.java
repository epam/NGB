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
package com.epam.catgenome.manager.export;

import com.epam.catgenome.util.FileFormat;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.Utils.DOT;
import static com.epam.catgenome.util.Utils.NEW_LINE;
import static com.epam.catgenome.util.Utils.NULL_STR;

public final class ExportUtils {
    private ExportUtils() {
        // no operations by default
    }

    public static <T> byte[] export(final List<T> result, final List<ExportField<T>> exportFields,
                                    final FileFormat format, final boolean includeHeader)
            throws IOException, ParseException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (includeHeader) {
            outputStream.write(getFileHeader(exportFields, format.getSeparator()).getBytes());
        }
        write(format, exportFields, result, outputStream);
        return outputStream.toByteArray();
    }

    private static <T> void write(final FileFormat format,
                       final List<ExportField<T>> exportFields,
                       final List<T> entries,
                       final ByteArrayOutputStream outputStream) throws IOException {
        for (T indexEntry : entries) {
            List<String> fieldValues = new ArrayList<>();
            for (ExportField<T> exportField: exportFields) {
                String value = exportField.getGetter().apply(indexEntry);
                fieldValues.add(value == null || value.equals(NULL_STR) ? DOT : value);
            }
            String line = String.join(format.getSeparator(), fieldValues) + NEW_LINE;
            outputStream.write(line.getBytes());
        }
    }

    private static <T> String getFileHeader(final List<ExportField<T>> exportFields, final String separator) {
        final List<String> fieldNames = exportFields.stream().map(ExportField::getLabel).collect(Collectors.toList());
        return String.join(separator, fieldNames) + NEW_LINE;
    }
}
