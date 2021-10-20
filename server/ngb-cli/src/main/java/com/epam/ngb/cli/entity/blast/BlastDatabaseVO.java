/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2021 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package com.epam.ngb.cli.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
public class BlastDatabaseVO implements RequestPayload {
    public static final String NUCLEOTIDE = "nucl";
    public static final String PROTEIN = "prot";
    public static final List<String> BLAST_DATABASE_TYPES = Arrays.asList(PROTEIN, NUCLEOTIDE);
@AllArgsConstructor
public class BlastDatabaseVO implements RequestPayload {
    public static final List<String> BLAST_DATABASE_TYPES = Arrays.asList("prot", "nucl");
    public static final List<String> BLAST_DATABASE_SOURCES = Arrays.asList("CUSTOM", "NCBI");
    private String name;
    private String path;
    private String type;
    private String source;
    private Long referenceId;

    public BlastDatabaseVO(final String name, final String path, final String type, final String source) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.source = source;
    }
}
