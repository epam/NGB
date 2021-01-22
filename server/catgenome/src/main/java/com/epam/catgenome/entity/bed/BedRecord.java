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

package com.epam.catgenome.entity.bed;

import java.util.Map;
import java.util.Objects;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.bed.parser.NggbBedFeature;
import htsjdk.samtools.util.CollectionUtil;
import htsjdk.tribble.annotation.Strand;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

/**
 * Bed File record representation for the client. Class holds all the necessary data from the file.
 */
public class BedRecord extends Block {
    private String name;
    private Float score;
    private Strand strand;
    private Integer thickStart;
    private Integer thickEnd;
    private String rgb;
    private int blockCount;
    private int[] blockSizes;
    private int[] blockStarts;
    private String id;
    private String description;
    private Map<String, Object> additional;

    public BedRecord() {
        //default constructor
    }

    /**
     * Creates a {@code BedRecord} object from a {@code NggbBedFeature}, which represents a single
     * line in a Bed File
     * @param bedFeature to convert into a {@code BedRecord}
     */
    public BedRecord(NggbBedFeature bedFeature) {
        setStartIndex(bedFeature.getStart());
        setEndIndex(bedFeature.getEnd());
        name = bedFeature.getName();
        score = bedFeature.getScore();
        strand = bedFeature.getStrand();

        if (bedFeature.getThickStart() != null) {
            thickStart = bedFeature.getThickStart() + 1;
        }

        if (bedFeature.getThickEnd() != null) {
            thickEnd = bedFeature.getThickEnd() + 1;
        }

        if (bedFeature.getColor() != null) {
            int red = bedFeature.getColor().getRed();
            int blue = bedFeature.getColor().getBlue();
            int green = bedFeature.getColor().getGreen();
            rgb = String.valueOf(red) + ',' + green + ',' + blue;
        }

        blockCount = bedFeature.getBlockCount();
        blockSizes = bedFeature.getBlockSizes();
        blockStarts = bedFeature.getBlockStarts();
        id = bedFeature.getId();
        description = bedFeature.getDescription();
        if (!MapUtils.isEmpty(bedFeature.getAdditional())) {
            additional = bedFeature.getAdditional();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Strand getStrand() {
        return strand;
    }

    public void setStrand(Strand strand) {
        this.strand = strand;
    }

    public Integer getThickStart() {
        return thickStart;
    }

    public void setThickStart(Integer thickStart) {
        this.thickStart = thickStart;
    }

    public Integer getThickEnd() {
        return thickEnd;
    }

    public void setThickEnd(Integer thickEnd) {
        this.thickEnd = thickEnd;
    }

    public String getRgb() {
        return rgb;
    }

    public void setRgb(String rgb) {
        this.rgb = rgb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BedRecord bedRecord = (BedRecord) o;

        return Objects.equals(getStartIndex(), bedRecord.getStartIndex()) && Objects.equals(getEndIndex(),
                bedRecord.getEndIndex());
    }

    @Override
    public int hashCode() {
        return getStartIndex(); // if records have the same start, they are equals
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public int[] getBlockSizes() {
        return blockSizes;
    }

    public void setBlockSizes(int[] blockSizes) {
        this.blockSizes = blockSizes;
    }

    public int[] getBlockStarts() {
        return blockStarts;
    }

    public void setBlockStarts(int[] blockStarts) {
        this.blockStarts = blockStarts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getAdditional() {
        return additional;
    }

    public void setAdditional(final Map<String, Object> additional) {
        this.additional = additional;
    }
}
