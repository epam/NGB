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

package com.epam.catgenome.controller.vo;

import com.epam.catgenome.controller.gene.ProteinSequenceVariationQuery;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.track.AbstractTrack;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.entity.track.SampledTrack;
import com.epam.catgenome.entity.track.Track;

/**
 * Source:      Query2TrackConverter
 * Created:     10/20/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * Converts TrackQuery value objects to Track objects
 * </p>
 */
public final class Query2TrackConverter {
    private Query2TrackConverter() {
        //No-op
    }

    /**
     * Converts TrackQuery to a regular Track of required type
     *
     * @param query a TrackQuery to convert
     * @param <T> required type of track
     * @return a Track of required type
     */
    public static <T extends Block> Track<T> convertToTrack(final TrackQuery query) {
        final Track<T> track = new Track<>();
        fillTrack(query, track);
        return track;
    }

    /**
     * Converts TrackQuery to a  SampledTrack of required type
     *
     * @param query a TrackQuery to convert
     * @param <T> required type of track
     * @return a SampledTrack of required type
     */
    public static <T extends Block> SampledTrack<T> convertToSampledTrack(final TrackQuery query) {
        final SampledTrack<T> track = new SampledTrack<>();
        fillTrack(query, track);
        return track;
    }

    /**
     * Converts ProteinSequenceVariationQuery to a regular Track of required type
     *
     * @param psVariationQuery a ProteinSequenceVariationQuery to convert
     * @param <T> required type of track
     * @return a Track of required type
     */
    public static <T extends Block> Track<T> convertToTrack(final ProteinSequenceVariationQuery psVariationQuery) {
        final Track<T> track = new Track<>();
        track.setId(psVariationQuery.getTrackQuery().getId());
        track.setEndIndex(psVariationQuery.getVariations().getEndIndex());
        track.setStartIndex(psVariationQuery.getVariations().getStartIndex());
        track.setScaleFactor(psVariationQuery.getVariations().getScaleFactor());
        track.setProjectId(psVariationQuery.getTrackQuery().getProjectId());
        track.setChromosome(new Chromosome(psVariationQuery.getVariations().getChromosome().getId()));
        return track;
    }


    private static void fillTrack(TrackQuery query, AbstractTrack track) {
        track.setId(query.getId());
        track.setEndIndex(query.getEndIndex());
        track.setStartIndex(query.getStartIndex());
        track.setScaleFactor(query.getScaleFactor());
        track.setProjectId(query.getProjectId());
        track.setChromosome(new Chromosome(query.getChromosomeId()));
    }
}
