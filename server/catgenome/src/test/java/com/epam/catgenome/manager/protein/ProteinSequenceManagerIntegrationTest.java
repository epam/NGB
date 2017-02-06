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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.gene.ProteinSequenceVariationQuery;
import com.epam.catgenome.controller.vo.TrackQuery;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.protein.MrnaProteinSequenceVariants;
import com.epam.catgenome.entity.protein.ProteinSequenceInfo;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.entity.vcf.Variation;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceManager;

/**
 * Created: 2/9/2016
 * Project: CATGenome Browser
 *
 * @author Nina_Lukashina
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ProteinSequenceManagerIntegrationTest extends AbstractManagerTest {

    private static final int VARIATION_START_INDEX_1 = 3;
    private static final int VARIATION_END_INDEX_1 = 4;
    private static final int VARIATION_START_INDEX_2 = 11;
    private static final int VARIATION_END_INDEX_2 = 13;
    private static final int TRACK_END_INDEX = 50;
    private static final int TRACK_START_INDEX = 1;
    private static final double TRACK_SCALE_FACTOR = 23.94927536183396;
    @Autowired
    private GffManager gffManager;

    @Autowired
    private ProteinSequenceManager proteinSequenceManager;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private ApplicationContext context;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadNewProteinSequence() throws IOException, InterruptedException, FeatureIndexException,
                                                    NoSuchAlgorithmException, GeneReadingException {
        // Add reference genome.
        Reference reference = prepareReference("classpath:templates/reference/hp.genome.fa");

        // Add gene file.
        GeneFile geneFile = prepareGene(reference, "classpath:templates/genes_sorted.gtf");

        // Load protein sequence.
        Track<Gene> track = new Track<>();
        track.setId(geneFile.getId());
        track.setStartIndex(1);
        track.setEndIndex(10);
        track.setChromosome(reference.getChromosomes().get(0));
        track.setScaleFactor(1D);

        Track<ProteinSequenceInfo> psTrack = proteinSequenceManager.loadProteinSequence(track, reference.getId());
        assertNotNull(psTrack);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadProteinSequenceWithVariations() throws IOException,
                                                               InterruptedException, FeatureIndexException,
                                                               NoSuchAlgorithmException, GeneReadingException {
        // Add reference genome.
        Reference reference = prepareReference("classpath:templates/protein/reference.fa");

        // Add gene file.
        GeneFile geneFile = prepareGene(reference, "classpath:templates/protein/genes.gff");

        Track<Variation> variationTrack = new Track<>(TrackType.VCF);
        variationTrack.setId(geneFile.getId());
        variationTrack.setChromosome(reference.getChromosomes().get(0));
        Variation variation1 =
                new Variation(VARIATION_START_INDEX_1, VARIATION_END_INDEX_1, "AG", Collections.singletonList("A"));
        Variation variation2 =
                new Variation(VARIATION_START_INDEX_2, VARIATION_END_INDEX_2, "CCC", Arrays.asList("CCT", "CCA"));
        variationTrack.setBlocks(Arrays.asList(variation1, variation2));
        TrackQuery trackQuery = new TrackQuery();
        trackQuery.setId(geneFile.getId());
        trackQuery.setStartIndex(TRACK_START_INDEX);
        trackQuery.setEndIndex(TRACK_END_INDEX);
        trackQuery.setScaleFactor(TRACK_SCALE_FACTOR);
        trackQuery.setChromosomeId(reference.getChromosomes().get(0).getId());
        ProteinSequenceVariationQuery psQuery = new ProteinSequenceVariationQuery(variationTrack, trackQuery);


        Track<MrnaProteinSequenceVariants> psList =
                proteinSequenceManager.loadProteinSequenceWithVariations(psQuery, reference.getId());
        assertNotNull(psList);
        /*assertTrue(psList.size() == 2);
        for (Track<ProteinSequence> psTrack : psList) {
            assertNotNull(psTrack);
            List<ProteinSequence> blocks = psTrack.getBlocks();
            assertNotNull(blocks);
            blocks.sort((o1, o2) -> o1.getStartIndex().compareTo(o2.getStartIndex()));
            String ps = blocks.stream().map(ProteinSequence::getText).collect(Collectors.joining());
            assertEquals("QQGS", ps);
        }*/
    }

    private GeneFile prepareGene(final Reference reference, final String fileName) throws IOException,
            FeatureIndexException, InterruptedException, NoSuchAlgorithmException {
        Resource resource;
        resource = context.getResource(fileName);
        FeatureIndexedFileRegistrationRequest registerGeneRequest = new FeatureIndexedFileRegistrationRequest();
        registerGeneRequest.setReferenceId(reference.getId());
        registerGeneRequest.setPath(resource.getFile().getAbsolutePath());

        return gffManager.registerGeneFile(registerGeneRequest);
    }

    private Reference prepareReference(final String fileName) throws IOException {
        Resource resource = context.getResource(fileName);
        ReferenceRegistrationRequest registerReferenceRequest = new ReferenceRegistrationRequest();
        registerReferenceRequest.setName("A1");
        registerReferenceRequest.setPath(resource.getFile().getPath());

        return referenceManager.registerGenome(registerReferenceRequest);
    }
}
