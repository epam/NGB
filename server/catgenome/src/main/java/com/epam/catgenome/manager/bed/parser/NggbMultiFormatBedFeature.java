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

import htsjdk.tribble.annotation.Strand;

import java.awt.*;
import java.util.Map;

/**
 * Implementation of {@code NggbBedFeature} according to Bed file format
 */
public class NggbMultiFormatBedFeature implements NggbBedFeature {
    protected String chr;
    protected int start = -1;
    protected int end = -1;
    protected Map<String, String> additional;

    public NggbMultiFormatBedFeature(int start, int end, String chr) {
        this.start = start;
        this.end = end;
        this.chr = chr;
    }

    @Override
    public Map<String, String> getAdditional() {
        return additional;
    }

    @Override
    public Strand getStrand() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public float getScore() {
        return 0;
    }

    @Override
    public String getLink() {
        return null;
    }

    @Override
    public Integer getThickStart() {
        return null;
    }

    @Override
    public Integer getThickEnd() {
        return null;
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
    }

    public void setColor(Color color) {
    }

    public void setScore(float score) {
    }

    public void setName(String name) {
    }

    public void setThickStart(int thickStart) {
    }

    public void setThickEnd(int thickEnd) {
    }

    @Override
    public int getBlockCount() {
        return 0;
    }

    public void setBlockCount(int blockCount) {
    }

    @Override
    public int[] getBlockSizes() {
        return null;
    }

    public void setBlockSizes(int[] blockSizes) {
    }

    @Override
    public int[] getBlockStarts() {
        return null;
    }

    public void setBlockStarts(int[] blockStarts) {
    }

    @Override
    public String getId() {
        return null;
    }

    public void setId(String id) {
    }

    public void setDescription(String description) {
    }
}
