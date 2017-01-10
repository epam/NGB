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

package com.epam.catgenome.entity.gene;

import java.util.List;

/**
 * Source:
 * Created:     5/26/2016
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.1, JDK 1.8
 *
 * <p>
 * Represents transcript data from external databases: ensemble, etc
 * </p>
 */
public class Transcript extends BaseGeneEntity {

    private List<BaseGeneEntity> exon;

    private List<BaseGeneEntity> utr;

    private List<BaseGeneEntity> domain;

    private List<BaseGeneEntity> secondaryStructure;

    private List<PBDGaneEntity> pdb;

    private String bioType;

    public List<BaseGeneEntity> getDomain() {
        return domain;
    }

    public void setDomain(List<BaseGeneEntity> domain) {
        this.domain = domain;
    }

    public List<BaseGeneEntity> getExon() {
        return exon;
    }

    public void setExon(List<BaseGeneEntity> exon) {
        this.exon = exon;
    }

    public List<BaseGeneEntity> getUtr() {
        return utr;
    }

    public void setUtr(List<BaseGeneEntity> utr) {
        this.utr = utr;
    }

    public List<PBDGaneEntity> getPdb() {
        return pdb;
    }

    public void setPdb(List<PBDGaneEntity> pdb) {
        this.pdb = pdb;
    }

    public List<BaseGeneEntity> getSecondaryStructure() {
        return secondaryStructure;
    }

    public void setSecondaryStructure(List<BaseGeneEntity> secondaryStructure) {
        this.secondaryStructure = secondaryStructure;
    }

    public String getBioType() {
        return bioType;
    }

    public void setBioType(String bioType) {
        this.bioType = bioType;
    }
}
