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

package com.epam.catgenome.util.db;


import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DBQueryUtils {

    public static final String IN_CLAUSE = "%s in (%s)";
    public static final String GENE_ID = "gene_id";

    private DBQueryUtils() {
        // no operations by default
    }

    public static String getGeneIdsClause(final List<String> geneIds) {
        return getInClause(GENE_ID, geneIds);
    }

    public static String getInClause(final String fieldName, final List<String> ids) {
        return String.format(IN_CLAUSE, String.format("LOWER(%s)", fieldName), ids.stream()
                .map(g -> "'" + g.toLowerCase() + "'")
                .collect(Collectors.joining(",")));
    }

    public static String getInClause(final String fieldName, final Set<Long> ids) {
        return String.format(IN_CLAUSE, fieldName, StringUtils.join(ids, ","));
    }
}
