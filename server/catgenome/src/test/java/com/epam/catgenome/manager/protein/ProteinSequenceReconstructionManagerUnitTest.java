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

package com.epam.catgenome.manager.protein;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.parser.GffFeature;
import com.epam.catgenome.manager.reference.ReferenceManager;

/**
 * Created: 2/15/2016
 * Project: CATGenome Browser
 *
 * @author Nina_Lukashina
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ProteinSequenceReconstructionManagerUnitTest {

    private static final int START_INDEX_1 = 3;
    private static final int END_INDEX_1 = 4;
    private static final int END_INDEX_2 = 13;
    private static final int START_INDEX_2 = 11;
    private static final int START_INDEX_3 = 8;
    private static final int END_INDEX_3 = 9;
    private static final int END_INDEX_4 = 23;
    private static final int START_INDEX_4 = 22;
    private static final List<Sequence> SEQUENCES_2_2 =
            Arrays.asList(new Sequence(8, 8, "T"), new Sequence(9, 9, "G"), new Sequence(10, 10, "G"),
                    new Sequence(11, 11, "A"), new Sequence(12, 12, "A"), new Sequence(13, 13, "A"),
                    new Sequence(14, 14, "T"), new Sequence(15, 15, "C"));
    private static final List<Sequence> SEQUENCES_1_2 =
            Arrays.asList(new Sequence(8, 8, "T"), new Sequence(9, 9, "G"), new Sequence(10, 10, "G"),
                    new Sequence(11, 11, "A"), new Sequence(12, 12, "A"), new Sequence(13, 13, "A"),
                    new Sequence(14, 14, "T"), new Sequence(15, 15, "C"));
    private static final List<Sequence> SEQUENCES_1_3 =
            Arrays.asList(new Sequence(20, 20, "G"), new Sequence(21, 21, "C"), new Sequence(22, 22, "G"),
                    new Sequence(23, 23, "C"), new Sequence(24, 24, "T"), new Sequence(25, 25, "A"),
                    new Sequence(26, 26, "T"), new Sequence(27, 27, "C"));
    private static final List<Sequence> SEQUENCES_2_1 =
            Arrays.asList(new Sequence(1, 1, "A"), new Sequence(2, 2, "T"), new Sequence(3, 3, "G"),
                    new Sequence(4, 4, "C"), new Sequence(5, 5, "T"), new Sequence(6, 6, "G"),
                    new Sequence(7, 7, "A"));
    private static final List<Sequence> SEQUENCES_1_1 =
            Arrays.asList(new Sequence(1, 1, "A"), new Sequence(2, 2, "T"), new Sequence(3, 3, "G"),
                    new Sequence(4, 4, "C"), new Sequence(5, 5, "T"), new Sequence(6, 6, "G"),
                    new Sequence(7, 7, "A"));

    @Mock
    private ReferenceManager referenceManager;

    @InjectMocks
    @Spy
    private ProteinSequenceReconstructionManager psReconstructionManager;

    private static final Gene CDS_1 = new Gene(new GffFeature(
            "2R\tFlyBase\tCDS\t1\t7\t3\t+\t0\tgene_id \"FBgn0265045\"; gene_symbol \"Strn-Mlck\"; "
                    + "transcript_id \"FBtr0336643\"; transcript_symbol \"Strn-Mlck-RG\";"));
    private static final Gene CDS_2 = new Gene(new GffFeature(
            "2R\tFlyBase\tCDS\t8\t15\t3\t-\t0\tgene_id \"FBgn0265045\"; gene_symbol \"Strn-Mlck\"; "
                    + "transcript_id \"FBtr0336643\"; transcript_symbol \"Strn-Mlck-RG\";"));
    private static final Gene CDS_3 = new Gene(new GffFeature(
            "2R\tFlyBase\tCDS\t20\t27\t3\t+\t2\tgene_id \"FBgn0265045\"; gene_symbol \"Strn-Mlck\"; "
                    + "transcript_id \"FBtr0336643\"; transcript_symbol \"Strn-Mlck-RG\";"));

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadNucleotidesForReferenceVariationCds() throws IOException, IllegalAccessException {
        Map<Variation, List<Gene>> variationToCds = new HashMap<>();
        Variation variation1 = new Variation(START_INDEX_1, END_INDEX_1, "GC", Collections.singletonList("G"));
        variationToCds.put(variation1, Collections.singletonList(CDS_1));
        Variation variation2 = new Variation(START_INDEX_2, END_INDEX_2, "AAA", Collections.singletonList("ATA"));
        variationToCds.put(variation2, Collections.singletonList(CDS_2));
        Variation variation3 = new Variation(START_INDEX_3, END_INDEX_3, "TG", Collections.singletonList("T"));
        variationToCds.put(variation3, Collections.singletonList(CDS_2));
        Variation variation4 = new Variation(START_INDEX_4, END_INDEX_4, "GC", Arrays.asList("GCA", "GCT"));
        variationToCds.put(variation4, Collections.singletonList(CDS_3));

        Chromosome chromosome = new Chromosome();
        chromosome.setName("Test chromosome");
        Long referenceId = 1L;
        List<Sequence> nucleotides1 = new ArrayList<>(SEQUENCES_1_1);
        Mockito.when(referenceManager
                .getNucleotidesFromNibFile(CDS_1.getStartIndex(), CDS_1.getEndIndex(), referenceId,
                        chromosome.getName()))
                .thenReturn(nucleotides1);
        List<Sequence> nucleotides2 = new ArrayList<>(SEQUENCES_1_2);
        Mockito.when(referenceManager
                .getNucleotidesFromNibFile(CDS_2.getStartIndex(), CDS_2.getEndIndex(), referenceId,
                        chromosome.getName()))
                .thenReturn(nucleotides2);
        List<Sequence> nucleotides3 = new ArrayList<>(SEQUENCES_1_3);
        Mockito.when(referenceManager
                .getNucleotidesFromNibFile(CDS_3.getStartIndex(), CDS_3.getEndIndex(), referenceId,
                        chromosome.getName()))
                .thenReturn(nucleotides3);

        Map<Gene, List<List<Sequence>>> nucleotides = psReconstructionManager
                .loadNucleotidesForReferenceVariationCds(chromosome, referenceId, variationToCds);
        Assert.assertNotNull(nucleotides);

        List<List<Sequence>> result1 = nucleotides.get(CDS_1);
        Assert.assertNotNull(result1);
        Assert.assertTrue(result1.size() == 1);
        String resultNucleotides1 = result1.get(0).stream().map(Sequence::getText).collect(Collectors.joining());
        Assert.assertEquals("ATGTGA", resultNucleotides1);

        List<List<Sequence>> result2 = nucleotides.get(CDS_2);
        Assert.assertNotNull(result2);
        Assert.assertTrue(result2.size() == 1);
        String resultNucleotides2 = result2.get(0).stream().map(Sequence::getText).collect(Collectors.joining());
        Assert.assertEquals("GAUAUCA", resultNucleotides2);

        List<List<Sequence>> result3 = nucleotides.get(CDS_3);
        Assert.assertNotNull(result3);
        Assert.assertTrue(result3.size() == 2);
        String resultNucleotides31 = result3.get(0).stream().map(Sequence::getText).collect(Collectors.joining());
        String resultNucleotides32 = result3.get(1).stream().map(Sequence::getText).collect(Collectors.joining());
        Assert.assertEquals("GCATATC", resultNucleotides31);
        Assert.assertEquals("GCTTATC", resultNucleotides32);
    }

    @Test
    public void testLoadNucleotidesForReferenceCds() throws IOException {
        Chromosome chromosome = EntityHelper.createNewChromosome();
        chromosome.setName("Test chromosome");
        Long referenceId = 1L;

        List<Sequence> nucleotides1 = new ArrayList<>(SEQUENCES_2_1);
        Mockito.when(referenceManager
                .getNucleotidesFromNibFile(CDS_1.getStartIndex(), CDS_1.getEndIndex(), referenceId,
                        chromosome.getName()))
                .thenReturn(nucleotides1);
        Mockito.when(referenceManager.getSequenceString(CDS_1.getStartIndex(), CDS_1.getEndIndex(), referenceId,
                chromosome.getName()))
                .thenReturn("ATGCTGA");
        List<Sequence> nucleotides2 = new ArrayList<>(SEQUENCES_2_2);
        Mockito.when(referenceManager
                .getNucleotidesFromNibFile(CDS_2.getStartIndex(), CDS_2.getEndIndex(), referenceId,
                        chromosome.getName()))
                .thenReturn(nucleotides2);
        Mockito.when(referenceManager.getSequenceString(CDS_2.getStartIndex(), CDS_2.getEndIndex(), referenceId,
                chromosome.getName()))
                .thenReturn("TGGAAATC");

        List<Gene> cdsSet = new ArrayList<>();
        cdsSet.add(CDS_1);
        List<List<Sequence>> nucleotideSequences =
                psReconstructionManager.loadNucleotidesForReferenceCds(chromosome, referenceId, cdsSet);
        Assert.assertNotNull(nucleotideSequences);
        Assert.assertTrue(nucleotideSequences.size() == 1);
        String sequence1 = nucleotideSequences.get(0).stream().map(Sequence::getText).collect(Collectors.joining());
        Assert.assertEquals("ATGCTGA", sequence1);
    }
}
