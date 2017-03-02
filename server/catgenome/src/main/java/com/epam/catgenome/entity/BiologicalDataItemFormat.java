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

package com.epam.catgenome.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Source:      BiologicalItemFormat
 * Created:     17.12.15, 12:52
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Represents a format of a biological data item instance.
 * </p>
 */
public enum BiologicalDataItemFormat {
    /**
     * A reference genome item
     */
    REFERENCE(1),

    /**
     * A VCF item
     */
    VCF(2),

    /**
     * A BAM item
     */
    BAM(3),

    /**
     * A GFF/GTF item
     */
    GENE(4),

    /**
     * A BIGWIG item
     */
    WIG(5),

    /**
     * An index item for VCF file
     */
    VCF_INDEX(6, true),

    /**
     * An index item for GFF/GTF file
     */
    GENE_INDEX(7, true),

    /**
     * An index item for BAM file
     */
    BAM_INDEX(8, true),

    /**
     * An index item for BED file
     */
    BED_INDEX(9, true),

    /**
     * A BED item
     */
    BED(10),

    /**
     * A SEG item
     */
    SEG(11),

    /**
     * An index item for SEG file
     */
    SEG_INDEX(12, true),

    /**
     * A MAF item
     */
    MAF(13),

    /**
     * An index item for MAF file
     */
    MAF_INDEX(14, true),

    /**
     * A VG item
     */
    VG(15),

    /**
     * A reference index item
     */
    REFERENCE_INDEX(16, true),


    /**
     * A some unknown index format. Used for dummy index reference.
     */
    INDEX(-1, true);

    private long id;
    private boolean index = false;
    private static Map<Long, BiologicalDataItemFormat> idMap = new HashMap<>((int) VG.getId());

    static {
        idMap.put(REFERENCE.id, REFERENCE);
        idMap.put(VCF.id, VCF);
        idMap.put(BAM.id, BAM);
        idMap.put(GENE.id, GENE);
        idMap.put(WIG.id, WIG);
        idMap.put(VCF_INDEX.id, VCF_INDEX);
        idMap.put(GENE_INDEX.id, GENE_INDEX);
        idMap.put(BAM_INDEX.id, BAM_INDEX);
        idMap.put(BED_INDEX.id, BED_INDEX);
        idMap.put(BED.id, BED);
        idMap.put(SEG.id, SEG);
        idMap.put(SEG_INDEX.id, SEG_INDEX);
        idMap.put(MAF.id, MAF);
        idMap.put(MAF_INDEX.id, MAF_INDEX);
        idMap.put(VG.id, VG);
        idMap.put(REFERENCE_INDEX.id, REFERENCE_INDEX);
        idMap.put(INDEX.id, INDEX);
    }

    BiologicalDataItemFormat(long id) {
        this.id = id;
    }

    BiologicalDataItemFormat(long id, boolean index) {
        this.id = id;
        this.index = index;
    }

    public long getId() {
        return id;
    }

    public boolean isIndex() {
        return index;
    }

    /**
     * Returns instance of BiologicalDataItemFormat by it's ID from the database
     * @param id ID of BiologicalDataItemFormat from the database
     * @return BiologicalDataItemFormat instance
     */
    public static BiologicalDataItemFormat getById(Long id) {
        if (id == null) {
            return null;
        }

        return idMap.get(id);
    }
}
