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

package com.epam.catgenome.manager.reference.io.cytoband;

import java.util.ArrayList;
import java.util.List;

import com.epam.catgenome.entity.reference.cytoband.Cytoband;

/**
 * Source:      CytobandRecord.java
 * Created:     11/24/15, 8:16 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code CytobandRecord} represents a plain data transfer object used to
 * encapsulates all cytobands belong to a chromosome with the given name.
 */
public class CytobandRecord {

    /**
     * {@code String} represents a chromosome's name which G-bands are
     * provided by this record.
     */
    private String chromosome;

    /**
     * {@code List} represents a list of bands for a particular chromosome.
     */
    private List<Cytoband> bands;

    public CytobandRecord(final String chromosome) {
        this.chromosome = chromosome;
        this.bands = new ArrayList<>();
    }

    public final String getChromosome() {
        return chromosome;
    }

    public final void setChromosome(final String chromosome) {
        this.chromosome = chromosome;
    }

    public final List<Cytoband> getBands() {
        return bands;
    }

    public final void setBands(final List<Cytoband> bands) {
        this.bands = bands;
    }

}
