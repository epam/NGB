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

package com.epam.catgenome.component;

/**
 * Source:      MessageCode.java
 * Created:     10/22/15, 5:18 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code MessageCode} defines persistent types of messages' codes used to retrieve
 * original messages from resources.
 * <p>
 * Here you can find and add codes that are regularly used by application managers,
 * controllers atc.
 */
public enum MessageCode {

    RESOURCE_NOT_FOUND("error.io.not.found"),
    MANDATORY_FILE_NAME("error.io.file.name.mandatory"),
    MAKE_FILE_STATUS("info.files.status.resource.at.path"),

    NO_SUCH_REFERENCE("error.no.such.reference"),
    NO_SUCH_CHROMOSOME("error.no.such.chromosome"),
    NO_CHROMOSOME_NAME("error.no.chromosome.name"),
    UNKNOWN_REFERENCE_ID("error.reference.aborted.saving.id"),
    NO_SUCH_SPECIES("error.no.such.species"),

    UNSORTED_BAM("error.unsorted.bam"),
    WRONG_BAM_INDEX_FILE("error.bam.index.file"),
    WRONG_NAME("name.already.exists"),

    ERROR_GENBANK_FILE_READING("error.genbank.file.reading"),
    ERROR_GENE_PRED_FILE_READING("error.genepred.file.reading"),
    ERROR_NO_QUALIFIERS("error.no.qualifiers");

    private final String code;

    MessageCode(final String code) {
        this.code = code;
    }

    public final String getCode() {
        return code;
    }

}
