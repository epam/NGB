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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.epam.catgenome.component.MessageHelper;

/**
 * Source:      Track.java
 * Created:     10/8/15, 5:35 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code Track} represents a general business entity used to manage data which is required
 * to present on UI a diverse collection of datasets, including reference sequence, variants,
 * genes etc.
 * <p>
 * The certain type of item in a dataset is defined through corresponded generic parameter
 * which should extend base {@code Block} entity.
 */
public class Track<T extends Block> extends AbstractTrack {
    private static final String HASH_DELIMITER = "_";

    /**
     * {@code List} specifies list of blocks that are defined by <tt>window</tt> coordinates
     * and should be displayed on visible area.
     */
    private List<T> blocks;

    public Track() {
        blocks = new ArrayList<>();
    }

    public Track(final Track track) {
        this.setId(track.getId());
        this.chromosome = track.getChromosome();
        this.endIndex = track.getEndIndex();
        this.scaleFactor = track.getScaleFactor();
        this.startIndex = track.getStartIndex();
        this.type = track.getType();
    }

    public Track(final TrackType type) {
        this();
        this.type = type;
    }

    public final List<T> getBlocks() {
        return blocks;
    }

    public final void setBlocks(final List<T> blocks) {
        this.blocks = blocks;
    }

    public String myCacheKey() {
        if (getChromosome() == null || getChromosome().getId() == null ||
                getStartIndex() == null || getEndIndex() == null || getId() == null) {
            throw new IllegalArgumentException(MessageHelper.getMessage("error.hash"));
        }

        StringBuilder sb = new StringBuilder();
        sb = sb.append(getId())
                .append(HASH_DELIMITER)
                .append(getChromosome().getId())
                .append(HASH_DELIMITER)
                .append(getStartIndex())
                .append(HASH_DELIMITER)
                .append(getEndIndex());
        return sb.toString();
    }

    public String proteinCacheKey(final Long referenceId) {
        String myCacheKey = myCacheKey();
        if (StringUtils.isEmpty(myCacheKey) || referenceId == null) {
            return null;
        }
        return myCacheKey + referenceId.toString();
    }
}
