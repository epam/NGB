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

package com.epam.catgenome.entity.seg;

import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.seg.parser.SegFeature;

/**
 * {@code SegRecord} represents a block of SEG track
 */
public class SegRecord extends Block {
    private String id;
    private Integer numMark;
    private Float segMean;

    /**
     * Creates a {@code SegRecord} from a {@code SegFeature}, parsed form a Seg file
     * @param segFeature
     */
    public SegRecord(SegFeature segFeature) {
        setStartIndex(segFeature.getStart());
        setEndIndex(segFeature.getEnd());
        id = segFeature.getId();
        numMark = segFeature.getNumMark();
        segMean = segFeature.getSegMean();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getNumMark() {
        return numMark;
    }

    public void setNumMark(Integer numMark) {
        this.numMark = numMark;
    }

    public Float getSegMean() {
        return segMean;
    }

    public void setSegMean(Float segMean) {
        this.segMean = segMean;
    }
}
