package com.epam.catgenome.manager.reference;

import static com.epam.catgenome.controller.vo.Query2TrackConverter.convertToTrack;

import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.entity.reference.StrandedSequence;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MotifSearchManagerValidationTest {

    public static final int TEST_START_POSITION = 1;
    public static final int TEST_END_POSITION = 100;
    public static final int WRONG_START_POSITION = 1000;
    public static final String TEST_MOTIF = "ATTGC";

    @Autowired
    private MotifSearchManager motifSearchManager;

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoReferenceId() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .startPosition(TEST_START_POSITION)
                .endPosition(TEST_END_POSITION)
                .chromosomeId(0L)
                .motif(TEST_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertTrue(search.getResult().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoMotif() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .startPosition(TEST_START_POSITION)
                .endPosition(TEST_END_POSITION)
                .referenceId(1L)
                .chromosomeId(0L)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertTrue(search.getResult().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoSearchType() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .startPosition(TEST_START_POSITION)
                .endPosition(TEST_END_POSITION)
                .referenceId(1L)
                .chromosomeId(0L)
                .motif(TEST_MOTIF)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertTrue(search.getResult().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoStartPosition() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .endPosition(TEST_END_POSITION)
                .referenceId(1L)
                .chromosomeId(0L)
                .motif(TEST_MOTIF)
                .searchType(MotifSearchType.REGION)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertTrue(search.getResult().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoEndPosition() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .startPosition(TEST_END_POSITION)
                .referenceId(1L)
                .chromosomeId(0L)
                .motif(TEST_MOTIF)
                .searchType(MotifSearchType.REGION)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertTrue(search.getResult().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseStartPositionIsMoreThenEndPosition() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .startPosition(TEST_END_POSITION)
                .endPosition(TEST_START_POSITION)
                .referenceId(1L)
                .chromosomeId(0L)
                .motif(TEST_MOTIF)
                .searchType(MotifSearchType.CHROMOSOME)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertTrue(search.getResult().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoChromosomeIdInTrack() {
        TrackQuery query = new TrackQuery();
        query.setId(1L);
        query.setEndIndex(TEST_END_POSITION);
        query.setStartIndex(0);
        Track<StrandedSequence> track = motifSearchManager.fillTrackWithMotifSearch(convertToTrack(query),
                TEST_MOTIF, StrandSerializable.POSITIVE);
        Assert.assertTrue(track.getBlocks().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoEndIndexInTrack() {
        TrackQuery query = new TrackQuery();
        query.setId(1L);
        query.setChromosomeId(0L);
        query.setStartIndex(0);
        Track<StrandedSequence> track = motifSearchManager.fillTrackWithMotifSearch(convertToTrack(query),
                TEST_MOTIF, StrandSerializable.POSITIVE);
        Assert.assertTrue(track.getBlocks().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoStartIndexInTrack() {
        TrackQuery query = new TrackQuery();
        query.setId(1L);
        query.setChromosomeId(0L);
        query.setEndIndex(TEST_END_POSITION);
        Track<StrandedSequence> track = motifSearchManager.fillTrackWithMotifSearch(convertToTrack(query),
                TEST_MOTIF, StrandSerializable.POSITIVE);
        Assert.assertTrue(track.getBlocks().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseStartPositionIsMoreThenEndPositionInTrack() {
        TrackQuery query = new TrackQuery();
        query.setId(1L);
        query.setChromosomeId(0L);
        query.setEndIndex(TEST_END_POSITION);
        query.setStartIndex(WRONG_START_POSITION);
        Track<StrandedSequence> track = motifSearchManager.fillTrackWithMotifSearch(convertToTrack(query),
                TEST_MOTIF, StrandSerializable.POSITIVE);
        Assert.assertTrue(track.getBlocks().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionBecauseNoReferenceIdInTrack() {
        TrackQuery query = new TrackQuery();
        query.setChromosomeId(0L);
        query.setStartIndex(TEST_START_POSITION);
        query.setEndIndex(TEST_END_POSITION);
        Track<StrandedSequence> track = motifSearchManager.fillTrackWithMotifSearch(convertToTrack(query),
                TEST_MOTIF, StrandSerializable.POSITIVE);
        Assert.assertTrue(track.getBlocks().isEmpty());
    }

}
