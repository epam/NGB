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

package com.epam.catgenome.entity.vcf;

import java.util.List;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;
import com.epam.catgenome.entity.security.AclClass;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents VCF file instance in a database. Contents various metadata to operate VCF files.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VcfFile extends FeatureFile {

    private List<VcfSample> samples;
    private Boolean multiSample;

    /**
     * Creates an empty {@code VcfFile} instance with a specified {@code BiologicalDataItemFormat.VCF}
     * format
     */
    public VcfFile() {
        setFormat(BiologicalDataItemFormat.VCF);
    }

    @Override
    public AclClass getAclClass() {
        return AclClass.VCF;
    }
}
