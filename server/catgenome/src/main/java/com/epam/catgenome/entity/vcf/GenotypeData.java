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

package com.epam.catgenome.entity.vcf;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Contains genotype information from a VCF file
 */
@Getter
@Setter
public class GenotypeData {
    private OrganismType organismType;

    /**
     * Raw genotype field from VCF file, has the following format: {0, 0}, {0, 1}, {2, 1}, etc.
     */
    private int[] genotype = new int[2];

    /**
     * Parsed genotype field, has the following format: T|C, TT|A, etc.
     */
    private String genotypeString;

    /**
     * Extended genotype information
     */
    private Map<String, Object> info;

    private Map<String, Object> extendedAttributes;

    public GenotypeData() {
        // No-op
    }

    /**
     * @param organismType genotype of the sample
     * @param genotype raw genotype field from VCF file
     * @param genotypeString parsed genotype field
     */
    public GenotypeData(OrganismType organismType, int[] genotype, String genotypeString) {
        this.organismType = organismType;
        this.genotype = genotype;
        this.genotypeString = genotypeString;
    }
}
