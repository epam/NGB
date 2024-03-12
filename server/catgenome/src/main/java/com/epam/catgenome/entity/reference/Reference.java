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

import java.util.ArrayList;
import java.util.List;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.IndexedDataItem;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.security.AclClass;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code Reference} represents a business entity designed to handle information that
 * describes a reference.
 */
@Getter
@Setter
public class Reference extends IndexedDataItem {

    private Long bioDataItemId;
    /**
     * {@code Long} represents the length of a reference in bases.
     */
    private Long size;

    /**
     * {@code List} represents collection of chromosomes related to a reference.
     */
    private List<Chromosome> chromosomes;

    /**
     * Represents GeneFile, containing gene annotations, associated with the reference genome
     */
    private GeneFile geneFile;

    /**
     * Represents annotation files, containing associated with the reference genome
     */
    private List<BiologicalDataItem> annotationFiles;

    /**
     * Represents a species associated with the reference genome
     */
    private Species species;
    private String proteinSequenceFile;

    public Reference() {
        chromosomes = new ArrayList<>();
        setFormat(BiologicalDataItemFormat.REFERENCE);
    }

    public Reference(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
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
        Reference reference = (Reference) o;
        if ((bioDataItemId != null) ?
                !bioDataItemId.equals(reference.bioDataItemId) :
                (reference.bioDataItemId != null)) {
            return false;
        }
        if ((size != null) ? !size.equals(reference.size) : (reference.size != null)) {
            return false;
        }
        return (chromosomes != null) ?
                chromosomes.equals(reference.chromosomes) :
                (reference.chromosomes == null);

    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (bioDataItemId != null ? bioDataItemId.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (chromosomes != null ? chromosomes.hashCode() : 0);
        return result;
    }

    @Override
    public AclClass getAclClass() {
        return AclClass.REFERENCE;
    }
}
