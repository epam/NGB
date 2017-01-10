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

package com.epam.catgenome.entity.reference;

import java.io.Serializable;
import java.util.Objects;

import com.epam.catgenome.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@code Chromosome} represents a business entity designed to handle general information
 * about a packaged and organized structure containing most of the DNA of a living organism.
 */
public class Chromosome extends BaseEntity implements Serializable {

    /**
     * {@code String} represents a key used to retrieve corresponded resource (e.g. file) that
     * describes sequence of a chromosome and is stored in an internal resources' storage.
     */
    @JsonIgnore
    private String path;

    /**
     * {@code String} represents an optional header of a chromosome that can be extracted from
     * an original chromosome's description provided via FASTA format.
     */
    private String header;

    /**
     * {@code Integer} represents the length of a chromosome in bases.
     */
    private Integer size;

    /**
     * {@code Long} represents ID of corresponded reference genome entity.
     */
    private Long referenceId;

    public Chromosome() {
        // no operations by default
    }

    public Chromosome(final Long id) {
        super(id, null);
    }

    public Chromosome(final String name, final Integer size) {
        setName(name);
        this.size = size;
    }

    public final String getPath() {
        return path;
    }

    public final void setPath(final String path) {
        this.path = path;
    }

    public final String getHeader() {
        return header;
    }

    public final void setHeader(final String header) {
        this.header = header;
    }

    public final Integer getSize() {
        return size;
    }

    public final void setSize(final Integer size) {
        this.size = size;
    }

    public final Long getReferenceId() {
        return referenceId;
    }

    public final void setReferenceId(final Long referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Chromosome that = (Chromosome) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }
}
