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

/**
 * <p>
 * Entity for control dim (pdb and uniprot start/end)
 * </p>
 */
public class DimEntity {
    private String compound;
    private String chainId;
    private Integer pdbStart;
    private Integer pdbEnd;
    private Integer unpStart;
    private Integer unpEnd;

    public String getCompound() {
        return compound;
    }

    public void setCompound(String compound) {
        this.compound = compound;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public Integer getPdbStart() {
        return pdbStart;
    }

    public void setPdbStart(Integer pdbStart) {
        this.pdbStart = pdbStart;
    }

    public Integer getPdbEnd() {
        return pdbEnd;
    }

    public void setPdbEnd(Integer pdbEnd) {
        this.pdbEnd = pdbEnd;
    }

    public Integer getUnpStart() {
        return unpStart;
    }

    public void setUnpStart(Integer unpStart) {
        this.unpStart = unpStart;
    }

    public Integer getUnpEnd() {
        return unpEnd;
    }

    public void setUnpEnd(Integer unpEnd) {
        this.unpEnd = unpEnd;
    }
}
