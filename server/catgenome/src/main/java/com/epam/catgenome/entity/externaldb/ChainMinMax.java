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
 * Entity for save min/max value
 * </p>
 */
public class ChainMinMax {
    private String name;
    private int start = Integer.MAX_VALUE;
    private int end = Integer.MIN_VALUE;
    private int otherStart = Integer.MAX_VALUE;
    private int otherEnd = Integer.MIN_VALUE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (this.name == null) {
            this.name = name;
        }
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getOtherStart() {
        return otherStart;
    }

    public void setOtherStart(int otherStart) {
        this.otherStart = otherStart;
    }

    public int getOtherEnd() {
        return otherEnd;
    }

    public void setOtherEnd(int otherEnd) {
        this.otherEnd = otherEnd;
    }

    public void addEnd(int end) {
        if (this.end < end) {
            this.end = end;
        }
    }

    public void addOtherStart(int otherStart) {
        if (this.otherStart > otherStart) {
            this.otherStart = otherStart;
        }
    }

    public void addOtherEnd(int otherEnd) {
        if (this.otherEnd < otherEnd) {
            this.otherEnd = otherEnd;
        }
    }

    public void addStart(int start) {
        if (this.start > start) {
            this.start = start;
        }
    }

}
