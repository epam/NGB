package com.epam.catgenome.util.feature.reader.index;

import htsjdk.tribble.Feature;

/**
 *
 * An interface for creating indexes
 *
 * @author jrobinso
 */
public interface IndexCreator {
    /**
     * Add a feature to the index
     * @param feature the feature, of which start, end, and contig must be filled in
     * @param filePosition the current file position, at the beginning of the specified feature
     */
    public void addFeature(Feature feature, long filePosition);

    /**
     * Create the index, given the stream of features passed in to this point
     * @param finalFilePosition the final file position, for indexes that have to close out with the final position
     * @return an index object
     */
    public Index finalizeIndex(long finalFilePosition);
}
