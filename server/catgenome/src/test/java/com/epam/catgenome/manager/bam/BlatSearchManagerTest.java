package com.epam.catgenome.manager.bam;

import com.epam.catgenome.entity.bam.PSLRecord;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BlatSearchManagerTest {

    private static final String TEST_SEQUENSE = "CAGTATCGTCCTTACTATTACATAGTGTGGTAGCGATGCAGTCCCAGTGAAAAAAAAAAAAAAAAAAAC";
    private static final Species TEST_SPECIES = new Species(){{
        setName("human");
        setVersion("hg19");
    }};
    private static final List<PSLRecord> EXPECTED = mockPSLRecord();

    @Autowired
    private BlatSearchManager blatSearchManager;

    @Test
    public void testFind() throws IOException, ExternalDbUnavailableException {
        List<PSLRecord> actual = blatSearchManager.find(TEST_SEQUENSE, TEST_SPECIES);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(EXPECTED.get(0), actual.get(0));
    }

    private static List<PSLRecord> mockPSLRecord() {
        PSLRecord record = new PSLRecord();
        record.setName("YourSeq");
        record.setChr("chr5");
        record.setStartIndex(118149214);
        record.setEndIndex(118149241);
        record.setStrand(StrandSerializable.POSITIVE);
        record.setMatch(27);
        record.setMisMatch(0);
        record.setRepMatch(0);
        record.setNs(0);
        record.setqGapCount(0);
        record.setqGapBases(0);
        record.settGapCount(0);
        record.setqGapBases(0);
        record.setqSize(69);
        record.setScore(27000f / 69);
        return Collections.singletonList(record);
    }

}