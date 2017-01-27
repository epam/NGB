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

package com.epam.catgenome.entity.bam;

import java.io.Serializable;
import java.util.List;

import com.epam.catgenome.entity.track.Block;
import com.fasterxml.jackson.annotation.JsonIgnore;
import htsjdk.samtools.CigarElement;

/**
 * Source:      Read.java
 * Created:     12/2/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * NGB representation of a read, this class reflects the read object model from SAM file format
 */
public class Read extends Block implements Serializable {

    /**
     * {@code String} read's name.
     */
    private String name;
    /**
     * {@code Boolean} represents the reading direction.
     */
    private Boolean stand;
    private String cigarString;
    private Integer flagMask;
    private Integer mappingQuality;
    private Integer tLen;
    private String rNext;
    private Integer pNext;
    /**
     * {@code List<CigarElement>} A list of CigarElements, which describes how a read aligns with the reference.
     * E.g. the Cigar string 10M1D25M means
     */
    @JsonIgnore
    private List<CigarElement> cigar;

    private String qualities;

    private String pairedReadName;

    //in case, when CIGAR have S at start or at end
    private String headSequence;

    private String tailSequence;

    private String rName;

    // extended fields
    private transient List<NgbSamTagAndValue> tags;
    private String sequence;

    /**
     * {@code List<BasePosition>} represents the base in read, that different from reference.
     */
    private List<BasePosition> differentBase;

    public String getQualities() {
        return qualities;
    }

    public void setQualities(String qualities) {
        this.qualities = qualities;
    }


    public String getPairedReadName() {
        return pairedReadName;
    }

    public void setPairedReadName(String pairedReadName) {
        this.pairedReadName = pairedReadName;
    }

    public List<BasePosition> getDifferentBase() {
        return differentBase;
    }

    public void setDifferentBase(List<BasePosition> differentBase) {
        this.differentBase = differentBase;
    }

    public String getHeadSequence() {
        return headSequence;
    }

    public void setHeadSequence(String headSequence) {
        this.headSequence = headSequence;
    }

    public String getTailSequence() {
        return tailSequence;
    }

    public void setTailSequence(String tailSequence) {
        this.tailSequence = tailSequence;
    }

    public String getRName() {
        return rName;
    }

    public void setRName(String rName) {
        this.rName = rName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStand() {
        return stand;
    }

    public void setStand(Boolean stand) {
        this.stand = stand;
    }

    public Integer getMappingQuality() {
        return mappingQuality;
    }

    public void setMappingQuality(Integer mappingQuality) {
        this.mappingQuality = mappingQuality;
    }

    public Integer getTLen() {
        return tLen;
    }

    public void setTLen(Integer tLen) {
        this.tLen = tLen;
    }

    public String getRNext() {
        return rNext;
    }

    public void setRNext(String rNext) {
        this.rNext = rNext;
    }

    public List<CigarElement> getCigar() {
        return cigar;
    }

    public void setCigar(List<CigarElement> cigar) {
        this.cigar = cigar;
    }

    public Integer getFlagMask() {
        return flagMask;
    }

    public void setFlagMask(Integer flagMask) {
        this.flagMask = flagMask;
    }

    public String getCigarString() {
        return cigarString;
    }

    public void setCigarString(String cigarString) {
        this.cigarString = cigarString;
    }

    public Integer getPNext() {
        return pNext;
    }

    public void setPNext(Integer pNext) {
        this.pNext = pNext;
    }

    public List<NgbSamTagAndValue> getTags() {
        return tags;
    }

    public void setTags(List<NgbSamTagAndValue> tags) {
        this.tags = tags;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}

