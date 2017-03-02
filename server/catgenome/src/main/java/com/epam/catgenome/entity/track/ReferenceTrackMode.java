package com.epam.catgenome.entity.track;

/**
 * Represents available reference track modes
 */
public enum ReferenceTrackMode {
    /**
     * Reference track contains nucleotide data
     */
    NUCLEOTIDES,
    /**
     * Reference track contains GC-content data
     */
    GC_CONTENT,
    /**
     * Reference track should contain GC-content data, but it cannot be provided (for example,
     * reference is remote)
     */
    NO_GC_DATA
}
