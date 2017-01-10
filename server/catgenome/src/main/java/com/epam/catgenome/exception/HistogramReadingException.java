package com.epam.catgenome.exception;

import com.epam.catgenome.entity.track.Track;

/**
 * Source:      HistogramWritingExceprion
 * Created:     23.11.16, 19:21
 * Project:     CATGenome Browser
 * Make: IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * <p>
 * An Exception, indicating that something wrong happened while reading histogram
 * </p>
 */
public class HistogramReadingException extends Exception {
    public HistogramReadingException(Track track, Throwable cause) {
        super(String.format("Exception while reading histogram for track %d, from chromosome '%d'",
                            track.getId(), track.getChromosome().getId()), cause);
    }
}
