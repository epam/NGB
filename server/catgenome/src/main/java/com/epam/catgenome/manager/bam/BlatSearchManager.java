package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BlatSearchManager {

    public List<PSLRecord> find(String readSequence, Species species) throws ExternalDbUnavailableException {
        //TODO we put mock here for possibilities to do work related with blat on the client side
        List<PSLRecord> mockedResult = new ArrayList<>();
        mockedResult.add(
                getMockedPslRecord(
                        "mockedBlatSearchResult0", "chr3", 111572177, 111572278,
                        101, 1000, 0, 0, 0, 0,
                        0, 0, 0, StrandSerializable.NEGATIVE)
        );
        mockedResult.add(
                getMockedPslRecord(
                        "mockedBlatSearchResult1", "chr8", 89711262, 89711286,
                        24, 227, 0, 0, 0, 1,
                        2, 0, 0, StrandSerializable.NEGATIVE)
        );
        mockedResult.add(
                getMockedPslRecord("mockedBlatSearchResult2", "chr12", 118712938, 118712962,
                        24, 227, 0, 0, 0, 1,
                        3, 0, 0, StrandSerializable.NEGATIVE)
        );
        mockedResult.add(
                getMockedPslRecord("mockedBlatSearchResult3", "chr7", 84750564, 84750593,
                        24, 217, 0, 0, 0, 1,
                        1, 1, 5, StrandSerializable.POSITIVE)
        );
        return mockedResult;
    }

    @NotNull
    private PSLRecord getMockedPslRecord(String name, String chr, int start, int end, int match, int score,
                                         int misMatch, int repMatch, int ns, int qGapCount, int qGapBases,
                                         int tGapCount, int tGapBases, StrandSerializable strand) {
        PSLRecord mock = new PSLRecord();
        mock.setName(name);
        mock.setChr(chr);
        mock.setStart(start);
        mock.setEnd(end);
        mock.setStrand(strand);
        mock.setMatch(match);
        mock.setScore(score);
        mock.setMisMatch(misMatch);
        mock.setRepMatch(repMatch);
        mock.setNs(ns);
        mock.setqGapCount(qGapCount);
        mock.setqGapBases(qGapBases);
        mock.settGapCount(tGapCount);
        mock.settGapBases(tGapBases);
        return mock;
    }

}
