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

package com.epam.catgenome.entity.track;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Source:      SampleTrack
 * Created:     14.06.16, 16:42
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A Track, that has multiple samples, that should be displayed as a whole
 * </p>
 */
public class SampledTrack<T extends Block> extends AbstractTrack {
    private Map<String, List<T>> tracks;

    /**
     * Basic empty constructor
     */
    public SampledTrack() {
        tracks = new HashMap<>();
    }

    /**
     * Create a SampledTrack, based on existing Track object
     * @param track
     */
    public SampledTrack(final Track track) {
        this.setId(track.getId());
        this.chromosome = track.getChromosome();
        this.endIndex = track.getEndIndex();
        this.scaleFactor = track.getScaleFactor();
        this.startIndex = track.getStartIndex();
        this.type = track.getType();
    }

    /**
     * Create a SampledTrack with desired type
     * @param type
     */
    public SampledTrack(final TrackType type) {
        this();
        this.type = type;
    }

    public Map<String, List<T>> getTracks() {
        return tracks;
    }

    public void setTracks(Map<String, List<T>> tracks) {
        this.tracks = tracks;
    }
}
