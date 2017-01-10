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

package com.epam.catgenome.vo;

import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToSampledTrack;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import com.epam.catgenome.controller.gene.ProteinSequenceVariationQuery;
import com.epam.catgenome.controller.vo.Query2TrackConverter;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.entity.protein.MrnaProteinSequenceVariants;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.seg.SegRecord;
import com.epam.catgenome.entity.track.SampledTrack;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.entity.vcf.Variation;

/**
 * Source:      Query2TrackConverterTest.java
 * Created:     10/26/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class Query2TrackConverterTest {

    private TrackQuery query;
    private final Long chromosomeId = 777L;
    private final long referenceId = 0L;
    private final int endIndex = 20;
    private final int startIndex = 1;
    private final double scaleFactor = 11;
    private final Chromosome chromosome = new Chromosome(chromosomeId);

    @Before
    public void creationTrackQuery() {
        query = new TrackQuery();
        query.setChromosomeId(chromosomeId);
        query.setId(referenceId);
        query.setEndIndex(endIndex);
        query.setStartIndex(startIndex);
        query.setScaleFactor(scaleFactor);
    }


    @Test
    public void convert2ReferenceTest() {
        Track<Sequence> track = Query2TrackConverter.convertToTrack(query);
        Assert.notNull(track);
        Assert.isTrue(track.getChromosome().getId().equals(chromosomeId), "name");
        Assert.isTrue(track.getId().equals(referenceId), "id");
        Assert.isTrue(track.getEndIndex().equals(endIndex), "endIndex");
        Assert.isTrue(track.getStartIndex().equals(startIndex), "start.index");
        Assert.isTrue(track.getScaleFactor().equals(scaleFactor), "scale.factor");
    }

    @Test
    public void convertToSampledTrackTest() {
        final SampledTrack<SegRecord> track = convertToSampledTrack(query);
        Assert.notNull(track);
        Assert.isTrue(track.getChromosome().getId().equals(chromosomeId), "name");
        Assert.isTrue(track.getId().equals(referenceId), "id");
        Assert.isTrue(track.getEndIndex().equals(endIndex), "endIndex");
        Assert.isTrue(track.getStartIndex().equals(startIndex), "start.index");
        Assert.isTrue(track.getScaleFactor().equals(scaleFactor), "scale.factor");
    }

    @Test
    public void convertToTrackTest() {
        Track<Variation> variationTrack = new Track<>(TrackType.VCF);
        variationTrack.setId(referenceId);
        variationTrack.setChromosome(chromosome);
        variationTrack.setEndIndex(endIndex);
        variationTrack.setStartIndex(startIndex);
        variationTrack.setScaleFactor(scaleFactor);

        TrackQuery trackQuery = new TrackQuery();
        trackQuery.setId(referenceId);
        trackQuery.setChromosomeId(chromosomeId);
        trackQuery.setEndIndex(endIndex);
        trackQuery.setStartIndex(startIndex);
        trackQuery.setScaleFactor(scaleFactor);


        ProteinSequenceVariationQuery psQuery = new ProteinSequenceVariationQuery(variationTrack, trackQuery);
        Track<MrnaProteinSequenceVariants> track = Query2TrackConverter.convertToTrack(psQuery);

        Assert.notNull(track);
        Assert.isTrue(track.getChromosome().getId().equals(chromosomeId), "name");
        Assert.isTrue(track.getId().equals(referenceId), "id");
        Assert.isTrue(track.getEndIndex().equals(endIndex), "endIndex");
        Assert.isTrue(track.getStartIndex().equals(startIndex), "start.index");
        Assert.isTrue(track.getScaleFactor().equals(scaleFactor), "scale.factor");
    }
}