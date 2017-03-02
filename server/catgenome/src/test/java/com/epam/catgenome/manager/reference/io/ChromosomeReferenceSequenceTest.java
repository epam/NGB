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

package com.epam.catgenome.manager.reference.io;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractJUnitTest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.manager.reference.ReferenceManager;

import htsjdk.samtools.reference.ReferenceSequence;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ChromosomeReferenceSequenceTest extends AbstractJUnitTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    ReferenceManager referenceManager;

    private Long referenceId;
    private List<Chromosome> chromosomes;
    private static final String REFERENCE_NAME = "hg38";

    private static final String CHR_1 = "chrA1";
    private static final String CHR_2 = "chrA2";
    private static final String CHR_3 = "chrA3";
    private static final String CHR_4 = "chrA4";
    private static final String CHR_5 = "chrA5";
    private static final int CHR_1_LEN = 231;
    private static final int CHR_2_LEN = 111;
    private static final int CHR_3_LEN = 171;
    private static final int CHR_4_LEN = 36;
    private static final int CHR_5_LEN = 44;

    private static final int INTERVAL_START = 50;
    private static final int INTERVAL_END = 151;

    private static final Map<String, Integer> EXPECTED_CHROMOSOMES = new HashMap<>();
    static {
        EXPECTED_CHROMOSOMES.put(CHR_1, CHR_1_LEN);
        EXPECTED_CHROMOSOMES.put(CHR_2, CHR_2_LEN);
        EXPECTED_CHROMOSOMES.put(CHR_3, CHR_3_LEN);
        EXPECTED_CHROMOSOMES.put(CHR_4, CHR_4_LEN);
        EXPECTED_CHROMOSOMES.put(CHR_5, CHR_5_LEN);
    }

    private static final Map<String, String> EXPECTED_SEQUENCES = new HashMap<>();
    static {
        EXPECTED_SEQUENCES.put(CHR_1, "NCCaGCAGAACCCAACCCCAACCCCAACCCCAACCCTAACCCTAACCCTAA"
                + "ACCCGggGCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA"
                + "ACCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCGGGGCCCTAACCCTAACCCT"
                + "ACCCTAACCCTAACCCTAAcccTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA");
        EXPECTED_SEQUENCES.put(CHR_2, "NCCAGCAGAACCCAACCCCAACCCCAACCCCAACCCTAACCCTAACCCTAA"
                + "ACCCTAGCCCTAACCCTAACCCTAACCCTAACGGTAACCccccCCCTAACCCTAACCCTA");
        EXPECTED_SEQUENCES.put(CHR_3, "NCCAGCAGAACCCAACCCCAACCCCAACCCCAACCCTAACCCTAACCCTAA"
                + "ACCCTAGGCCTAACCCTAACCCTAACCCtAACCCTAACCCTAACCCTAACCCTAACCCTA"
                + "ACCCCTAACCCTAACCCtAACCCGGACCCTAACCCTAACCCTAACCCTAGGGCTAACCCT");
        EXPECTED_SEQUENCES.put(CHR_4, "NCCtGCAGAACCCAACCCCaaCCCCAACCCCAACCC");
        EXPECTED_SEQUENCES.put(CHR_5, "NCCAGCAGAACCCaaCCCCAACCCCAACCCCAACCCTgCCCAAA");
    }


    @Before
    public void setUp() throws IOException {
        File fasta = getTemplate("reference/hp.genome.fa");
        FastaUtils.indexFasta(fasta);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(REFERENCE_NAME);
        request.setPath(fasta.getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        Reference reference = referenceManager.registerGenome(request);
        Assert.assertTrue(reference != null);
        referenceId = reference.getId();
        chromosomes = reference.getChromosomes();
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    public void getSequence() throws Exception {
        ChromosomeReferenceSequence referenceSequence = new ChromosomeReferenceSequence(chromosomes,
                referenceId, referenceManager);
        Assert.assertTrue(referenceSequence.getSequenceDictionary() != null);
        Assert.assertEquals(EXPECTED_CHROMOSOMES.size(), referenceSequence.getSequenceDictionary().size());
        for (Map.Entry<String, Integer> chrSize : EXPECTED_CHROMOSOMES.entrySet()) {
            ReferenceSequence sequence = referenceSequence.getSequence(chrSize.getKey());
            Assert.assertEquals(chrSize.getValue().intValue(), sequence.length());
            Assert.assertEquals(EXPECTED_SEQUENCES.get(chrSize.getKey()), sequence.getBaseString());
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    public void getSequenceInterval() throws Exception {
        ChromosomeReferenceSequence referenceSequence = new ChromosomeReferenceSequence(chromosomes,
                referenceId, referenceManager);
        ReferenceSequence sequence =
                referenceSequence.getSubsequenceAt(CHR_1, INTERVAL_START, INTERVAL_END);
        Assert.assertEquals(EXPECTED_SEQUENCES.get(CHR_1).substring(INTERVAL_START - 1, INTERVAL_END),
                sequence.getBaseString());
    }
}
