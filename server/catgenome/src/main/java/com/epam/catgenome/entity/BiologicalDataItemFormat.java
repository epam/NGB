/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

    /*
     * Ids 13 (MAF) and 14 (MAF_INDEX) were used by a format NGB no longer supports. The gap is
     * deliberate: the ids below are persisted in BIO_DATA_ITEM.FORMAT and must not be reassigned.
     */

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
    INDEX(-1, true),

    BED_GRAPH_INDEX(18, true),

    FEATURE_COUNTS(19),

    HEATMAP(20),

    LINEAGE_TREE(21),

    PATHWAY(22),

    PDB_FILE(23);

    /**
     * The reserved ids of the removed MAF formats, named so they cannot be reassigned by accident.
     */
    private static final long MAF_ID = 13L;
    private static final long MAF_INDEX_ID = 14L;

    /**
     * Formats removed from NGB, by the id they used to be persisted under. Kept so a database
     * written by an older NGB reports what it holds instead of failing obscurely.
     */
    private static final Map<Long, String> REMOVED_FORMAT_NAMES = removedFormatNames();

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
        idMap.put(VG.id, VG);
        idMap.put(REFERENCE_INDEX.id, REFERENCE_INDEX);
        idMap.put(INDEX.id, INDEX);
        idMap.put(BED_GRAPH_INDEX.id, BED_GRAPH_INDEX);
        idMap.put(FEATURE_COUNTS.id, FEATURE_COUNTS);
        idMap.put(HEATMAP.id, HEATMAP);
        idMap.put(LINEAGE_TREE.id, LINEAGE_TREE);
        idMap.put(PATHWAY.id, PATHWAY);
        idMap.put(PDB_FILE.id, PDB_FILE);
    }

    private static Map<Long, String> removedFormatNames() {
        final Map<Long, String> names = new HashMap<>();
        names.put(MAF_ID, "MAF");
        names.put(MAF_INDEX_ID, "MAF_INDEX");
        return names;
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
     * @return BiologicalDataItemFormat instance, or {@code null} if {@code id} is {@code null} or is
     *         not a known format id. {@code null} for an unknown id is deliberate: this method is
     *         also called for INDEX_FORMAT of items that have no index.
     * @throws IllegalArgumentException if the id belongs to a format NGB has removed. Those ids are
     *         named in the message, because a database written by an older NGB can still hold them.
     */
    public static BiologicalDataItemFormat getById(Long id) {
        if (id == null) {
            return null;
        }
        if (REMOVED_FORMAT_NAMES.containsKey(id)) {
            throw new IllegalArgumentException(unsupportedFormatMessage(id));
        }

        return idMap.get(id);
    }

    /**
     * @param id a format id NGB has removed
     * @return a message naming the removed format the id belonged to
     */
    public static String unsupportedFormatMessage(final Long id) {
        final String removed = REMOVED_FORMAT_NAMES.get(id);
        return removed == null
               ? String.format("Unknown biological data item format id: %s.", id)
               : String.format("Biological data item format %s (%s) is no longer supported. MAF "
                               + "registration was removed from NGB and the database upgrade deletes "
                               + "the rows that held this format, so there is nothing to re-register.",
                               id, removed);
    }
}
