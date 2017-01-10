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

package com.epam.catgenome.entity.reference.cytoband;

import com.epam.catgenome.entity.track.Block;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@code Cytoband} represents a business entity designed to handle information that
 * describes a single cytological band on a chromosome.
 */
public class Cytoband extends Block {

    /**
     * {@code String} represents a name for a cytological band. Optional as in some cases
     * it can be unspecified.
     */
    private String name;

    /**
     * {@code String} represents a name for a chromosome which is describing by this band.
     */
    @JsonIgnore
    private String chromosome;

    /**
     * {@code GiemsaStain} represents Giemsa stain value that describes a certain type of
     * this band.
     */
    private GiemsaStain giemsaStain;

    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public final String getChromosome() {
        return chromosome;
    }

    public final void setChromosome(final String chromosome) {
        this.chromosome = chromosome;
    }

    public final GiemsaStain getGiemsaStain() {
        return giemsaStain;
    }

    public final void setGiemsaStain(final GiemsaStain giemsaStain) {
        this.giemsaStain = giemsaStain;
    }

}
