package com.epam.catgenome.entity.bam;

/**
 * Defines BAM browsing mode
 */
public enum BamTrackMode {
    /**
     * On a largest scale, only regions, possibly containing variations should be displayed
     */
    REGIONS,

    /**
     * On a medium scale, BAM coverage can be calculated and displayed
     */
    COVERAGE,

    /**
     * On a small scale, BAM reads can be displayed along with coverage
     */
    FULL
}
