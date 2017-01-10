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

package com.epam.catgenome.entity.externaldb;


import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Information about db type
 * </p>
 */
public enum ExternalDBType {
    NCBI_DB_ID(1),
    ENSEMBL_DB_ID(2);

    private static Map<Long, ExternalDBType> map = new HashMap<>();

    static {
        map.put(NCBI_DB_ID.getId(), NCBI_DB_ID);
        map.put(ENSEMBL_DB_ID.getId(), ENSEMBL_DB_ID);
    }

    private long id;

    ExternalDBType(long id) {
        this.id = id;
    }

    public static ExternalDBType getById(long id) {
        return map.get(id);
    }

    public long getId() {
        return id;
    }
}
