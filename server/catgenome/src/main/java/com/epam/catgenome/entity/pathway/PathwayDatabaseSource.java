/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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
package com.epam.catgenome.entity.pathway;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public enum PathwayDatabaseSource {
    CUSTOM(1L, ".sbgn"),
    BIOCYC(2L, ".owl"),
    COLLAGE(3L, ".json");

    private Long sourceId;
    private String extension;

    public static final List<String> BIO_PAX_EXTENSIONS = Collections.singletonList(BIOCYC.getExtension());
    public static final List<String> PATHWAY_EXTENSIONS = Arrays.asList(CUSTOM.getExtension(), COLLAGE.getExtension());

    /**
     * @param sourceId
     * @return a {@code PathwayDatabaseSource} instance corresponding to the input ID
     */
    public static PathwayDatabaseSource getById(final Long sourceId) {
        for (PathwayDatabaseSource source : PathwayDatabaseSource.values()) {
            if (source.getSourceId().equals(sourceId)) {
                return source;
            }
        }
        return null;
    }

    public static PathwayDatabaseSource getByExtension(final String extension) {
        for (PathwayDatabaseSource source : PathwayDatabaseSource.values()) {
            if (source.getExtension().equals(extension)) {
                return source;
            }
        }
        return null;
    }
}
