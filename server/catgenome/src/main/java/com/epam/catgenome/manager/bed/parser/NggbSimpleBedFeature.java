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

package com.epam.catgenome.manager.bed.parser;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;

import htsjdk.tribble.annotation.Strand;

/**
 * Implementation of {@code NggbBedFeature} according to Bed file format
 */
public class NggbSimpleBedFeature implements NggbBedFeature {
    protected String chr;
    protected int start = -1;
    protected int end = -1;
    protected Integer thickStart;
    protected Integer thickEnd;
    protected Strand strand = Strand.NONE;
    private String name = "";
    private float score = Float.NaN;
    private String type = "";
    private Color color;
    private String description;
    private String id;
    private String link;
    private int blockCount;
    private int[] blockSizes;
    private int[] blockStarts;

    public NggbSimpleBedFeature(int start, int end, String chr) {
        this.start = start;
        this.end = end;
        this.chr = chr;
    }

    @Override
    public Map<String, Object> getAdditional() {
        return Collections.emptyMap();
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getScore() {
        return score;
    }

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public Integer getThickStart() {
        return thickStart;
    }

    @Override
    public Integer getThickEnd() {
        return thickEnd;
    }

    @Override
    public String getChr() {
        return chr;
    }

    @Override
    public String getContig() {
        return chr;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    public void setStrand(Strand strand) {
        this.strand = strand;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setThickStart(int thickStart) {
        this.thickStart = thickStart;
    }

    public void setThickEnd(int thickEnd) {
        this.thickEnd = thickEnd;
    }

    @Override
    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    @Override
    public int[] getBlockSizes() {
        return blockSizes;
    }

    public void setBlockSizes(int[] blockSizes) {
        this.blockSizes = blockSizes;
    }

    @Override
    public int[] getBlockStarts() {
        return blockStarts;
    }

    public void setBlockStarts(int[] blockStarts) {
        this.blockStarts = blockStarts;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
