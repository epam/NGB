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

import java.util.List;

import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.FeatureFile;

/**
 * {@code SegFile} represents a SEG file metadata and {@code List} of assosiated woth it samples.
 * A SEG file (segmented data; .seg) is a tab-delimited text file that lists loci and associated
 * numeric values.
 */
public class SegFile extends FeatureFile {
    private List<SegSample> samples;

    /**
     * Creates an empty {@code SegFile} instance with a specified {@code BiologicalDataItemFormat.SEG}
     * format
     */
    public SegFile() {
        setFormat(BiologicalDataItemFormat.SEG);
    }

    public List<SegSample> getSamples() {
        return samples;
    }

    public void setSamples(List<SegSample> samples) {
        this.samples = samples;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SegFile segFile = (SegFile) o;

        return (samples != null) ? samples.equals(segFile.samples) : (segFile.samples == null);

    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (samples != null ? samples.hashCode() : 0);
        return result;
    }
}
