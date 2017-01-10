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

package com.epam.catgenome.controller.vo.ga4gh;

import java.util.List;
import java.util.Map;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;

/**
 * <p>
 * Class for data from GA4GH (setSearch)
 * </p>
 */
public class GenotypeGA4GH extends Genotype {
    private final List<Allele> alleles;
    private final boolean isPhasedFields;
    private final int gq;
    private final int dp;
    private final int[] ad;
    private final int[] pl;
    private final transient Map<String, Object> extendedAttributes;

    public GenotypeGA4GH(final String sampleName, final List<Allele> alleles, final boolean isPhased, final int gq,
                         final int dp, final int[] ad, final int[] pl, final String filters,
                         final Map<String, Object> extendedAttributes) {
        super(sampleName, filters);
        this.alleles = alleles;
        this.isPhasedFields = isPhased;
        this.gq = gq;
        this.dp = dp;
        this.ad = ad;
        this.pl = pl;
        this.extendedAttributes = extendedAttributes;
    }

    @Override
    public List<Allele> getAlleles() {
        return alleles;
    }

    @Override
    public Allele getAllele(int i) {
        return alleles.get(i);
    }

    @Override
    public boolean isPhased() {
        return isPhasedFields;
    }

    @Override
    public int getDP() {
        return dp;
    }

    @Override
    public int[] getAD() {
        return ad;
    }

    @Override
    public int getGQ() {
        return gq;
    }

    @Override
    public int[] getPL() {
        return pl;
    }

    @Override
    public Map<String, Object> getExtendedAttributes() {
        return extendedAttributes;
    }

}
