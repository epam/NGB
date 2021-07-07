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

package com.epam.catgenome.manager.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.manager.reference.io.FastaUtils;
import com.epam.catgenome.manager.genbank.GenbankUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.reference.ReferenceGenomeDao;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.entity.track.TrackType;
import com.epam.catgenome.exception.Ga4ghResourceUnavailableException;
import com.epam.catgenome.exception.ReferenceReadingException;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.util.TestUtils;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;

/**
 * Source:      ReferenceManagerTest.java
 * Created:     10/12/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"}) public class ReferenceManagerTest
        extends AbstractManagerTest {

    private static final double SCALE_FACTOR_4_BASE = 1D;
    private static final double SCALE_FACTOR_4_GC = 0.0001;
    private static final double SCALE_FACTOR_4_GC_NEW = 0.2;

    private static final int START_INDEX = 1;
    private static final int END_INDEX = 1000;
    private static final int LIST_INDEX = 4;
    private static final String NEW_NAME = "hiMom";
    private static final String A3_FA_PATH = "classpath:templates/A3.fa";
    private static final String GENBANK_PATH = "classpath:templates/KU131557.gbk";
    public static final String PRETTY_NAME = "pretty";

    @Value("${ga4gh.google.referenceSetId}") private String referenseSetID;

    @Value("#{catgenome['files.base.directory.path']}")
    private String baseDirPath;

    private long idRef;
    private long idChrom;
    private Resource resource;
    private Reference reference;

    @Autowired private ApplicationContext context;

    @Autowired private ReferenceManager referenceManager;

    @Autowired private ReferenceGenomeDao referenceGenomeDao;

    @Autowired private ReferenceGenomeManager referenceGenomeManager;

    @Autowired private GffManager gffManager;

    @Autowired private BiologicalDataItemDao biologicalDataItemDao;

    @Before public void fastaToNibFileTest() throws IOException {
        resource = context.getResource(A3_FA_PATH);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(NEW_NAME);
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        request.setPrettyName(PRETTY_NAME);

        reference = referenceManager.registerGenome(request);
        assertNotNull(reference);
        idRef = reference.getId();
        Chromosome chromosome = reference.getChromosomes().get(0);
        idChrom = chromosome.getId();
    }

    @Ignore @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void getReference() {

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setType(BiologicalDataItemResourceType.GA4GH);
        request.setPath(referenseSetID);
        request.setName(NEW_NAME);
        Track<Sequence> trackResult = null;
        Track<Sequence> track = null;
        try {
            Reference reference = referenceManager.registerGenome(request);
            track = new Track<>();
            track.setType(TrackType.REF);
            track.setId(reference.getId());
            track.setEndIndex(END_INDEX);
            track.setStartIndex(START_INDEX);
            track.setScaleFactor(SCALE_FACTOR_4_BASE);
            List<Chromosome> list =
                    referenceGenomeDao.loadAllChromosomesByReferenceId(reference.getId());
            track.setChromosome(list.get(LIST_INDEX));
            trackResult = referenceManager.getNucleotidesTrackFromNib(track);
        } catch (IOException | Ga4ghResourceUnavailableException e) {
            e.printStackTrace();
        }

        assertNotNull(reference);
        Reference referenceNew = referenceGenomeDao.loadReferenceGenome(reference.getId());
        assertEquals(PRETTY_NAME, referenceNew.getPrettyName());
        assertEquals("Unexpected id bioData.", reference.getBioDataItemId(),
                referenceNew.getBioDataItemId());
        assertEquals("Unexpected size reference.", reference.getSize(), referenceNew.getSize());
        assertEquals("Unexpected chromosome.", trackResult.getChromosome().getName(),
                track.getChromosome().getName());

        //2 scaleFactor < 0.5

        track.setScaleFactor(SCALE_FACTOR_4_GC_NEW);
        try {
            trackResult = referenceManager.getNucleotidesTrackFromNib(track);
            assertEquals("Unexpected id bioData.", reference.getBioDataItemId(),
                    referenceNew.getBioDataItemId());
            assertEquals("Unexpected size reference.", reference.getSize(), referenceNew.getSize());
            assertEquals("Unexpected chromosome.", trackResult.getChromosome().getName(),
                    track.getChromosome().getName());

        } catch (IOException | Ga4ghResourceUnavailableException e) {
            e.printStackTrace();
        }
    }

    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void nibFileToNucleotide() throws ReferenceReadingException {

        Track<Sequence> track = new Track<>();
        Chromosome chromosome = new Chromosome();
        chromosome.setId(idChrom);
        track.setId(idRef);
        track.setChromosome(chromosome);
        track.setScaleFactor(SCALE_FACTOR_4_BASE);
        track.setStartIndex(START_INDEX);
        track.setEndIndex(END_INDEX);
        track = referenceManager.getNucleotidesResultFromNib(track);
        assertNotNull(track);
    }

    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void getGCContentTest() throws ReferenceReadingException {
        Track<Sequence> track = new Track<>();
        Chromosome chromosome = new Chromosome();
        chromosome.setId(idChrom);
        track.setId(idRef);
        track.setChromosome(chromosome);
        track.setScaleFactor(SCALE_FACTOR_4_GC);
        track.setStartIndex(START_INDEX);
        track.setEndIndex(END_INDEX);
        track = referenceManager.getNucleotidesResultFromNib(track);
        assertNotNull(track);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void testUnregister() throws IOException {
        try {
            assertNotNull(reference);
            Path metaDataRefDir = Paths.get(baseDirPath + "/references/" + reference.getId());
            assertTrue(Files.exists(metaDataRefDir));
            Reference unregisterRef = referenceManager.unregisterGenome(reference.getId());
            assertNotNull(unregisterRef);
            assertFalse(Files.exists(metaDataRefDir));
            Reference refDeleted = referenceGenomeDao.loadReferenceGenome(reference.getId());
            assertNull("removed fail", refDeleted);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
            request.setName(NEW_NAME);
            request.setPath(resource.getFile().getPath());
            request.setType(BiologicalDataItemResourceType.FILE);
            request.setPrettyName(PRETTY_NAME);
            referenceManager.registerGenome(request);
        }
    }

    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void getOtherGCContentTest() throws ReferenceReadingException {
        Track<Sequence> track = new Track<>();
        Chromosome chromosome = new Chromosome();
        chromosome.setId(idChrom);
        track.setId(idRef);
        track.setChromosome(chromosome);
        track.setScaleFactor(SCALE_FACTOR_4_GC);
        track.setStartIndex(START_INDEX);
        track.setEndIndex(END_INDEX);
        track = referenceManager.getNucleotidesResultFromNib(track);
        assertNotNull(track);
        track.setScaleFactor(SCALE_FACTOR_4_GC_NEW);
        track = referenceManager.getNucleotidesResultFromNib(track);
        assertNotNull(track);
    }

    //test Identical fata from original Fasta-file and data from method getNucleotidesResultFromNib
    //To-do equals '-','?','n'
    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void nibFileAndFastaFileIdenticalTest() throws ReferenceReadingException, IOException {
        ReferenceSequenceFile referenceSequenceFile =
                ReferenceSequenceFileFactory.getReferenceSequenceFile(resource.getFile());
        //Go through all chromosomes if FASTA-file
        Track<Sequence> track = new Track<>();
        track.setType(TrackType.REF);
        track.setScaleFactor(SCALE_FACTOR_4_BASE);
        track.setId(reference.getId());

        byte[] arrayOfNucleicAcids;

        //Nucleicide frome FASTA-file
        arrayOfNucleicAcids = referenceSequenceFile.nextSequence().getBases();
        Chromosome chromosome = reference.getChromosomes().get(0);
        track.setChromosome(chromosome);
        track.setStartIndex(START_INDEX);
        track.setEndIndex(END_INDEX);
        //Nucleicide frome nib-file
        track = referenceManager.getNucleotidesResultFromNib(track);
        for (Sequence sequence : track.getBlocks()) {
            assertTrue(String.valueOf((char) (arrayOfNucleicAcids[sequence.getStartIndex() - 1]))
                    .equals(sequence.getText()));
        }
    }

    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterWithGeneFile() throws IOException {
        Resource resource = context.getResource(A3_FA_PATH);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("testReference1 " + this.getClass().getSimpleName());
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        FeatureIndexedFileRegistrationRequest geneRequest =
                new FeatureIndexedFileRegistrationRequest();
        resource = context.getResource("classpath:templates/genes_sorted.gtf");
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        request.setGeneFileRequest(geneRequest);

        Reference testRef = referenceManager.registerGenome(request);
        assertNotNull(testRef);
        assertNotNull(testRef.getGeneFile());

        Reference loadedReference = referenceGenomeManager.load(testRef.getId());
        assertNotNull(loadedReference);
        assertNotNull(loadedReference.getGeneFile());
        assertNotNull(loadedReference.getGeneFile().getId());
        assertNotNull(loadedReference.getGeneFile().getPath());
        assertNotNull(loadedReference.getGeneFile().getIndex());

        loadedReference = referenceGenomeManager.updateReferenceGeneFileId(testRef.getId(), null);
        assertNull(loadedReference.getGeneFile());

        loadedReference = referenceGenomeManager
                .updateReferenceGeneFileId(testRef.getId(), testRef.getGeneFile().getId());
        assertNotNull(loadedReference.getGeneFile());

        resource = context.getResource(A3_FA_PATH);
        request = new ReferenceRegistrationRequest();
        request.setName("testReference2 " + this.getClass().getSimpleName());
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);
        request.setGeneFileId(testRef.getGeneFile().getId());

        Reference testRef2 = referenceManager.registerGenome(request);
        loadedReference = referenceGenomeManager.load(testRef2.getId());
        assertNotNull(loadedReference);
        assertNotNull(loadedReference.getGeneFile());
        assertNotNull(loadedReference.getGeneFile().getId());
        assertNotNull(loadedReference.getGeneFile().getPath());
        assertNotNull(loadedReference.getGeneFile().getIndex());
        assertEquals(testRef.getGeneFile().getId(), loadedReference.getGeneFile().getId());
    }

    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testDeleteReferenceGeneFile() throws IOException {
        Resource resource = context.getResource(A3_FA_PATH);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("testReference " + this.getClass().getSimpleName());
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        FeatureIndexedFileRegistrationRequest geneRequest =
                new FeatureIndexedFileRegistrationRequest();
        resource = context.getResource("classpath:templates/genes_sorted.gtf");
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        request.setGeneFileRequest(geneRequest);

        Reference testRef = referenceManager.registerGenome(request);
        assertNotNull(testRef);
        assertNotNull(testRef.getGeneFile());

        referenceGenomeManager.updateReferenceGeneFileId(testRef.getId(), null);
        gffManager.unregisterGeneFile(testRef.getGeneFile().getId());
    }

    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testFailDeleteReferenceGeneFile() throws IOException {
        Resource resource = context.getResource(A3_FA_PATH);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("testReference3");
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        FeatureIndexedFileRegistrationRequest geneRequest =
                new FeatureIndexedFileRegistrationRequest();
        resource = context.getResource("classpath:templates/genes_sorted.gtf");
        geneRequest.setPath(resource.getFile().getAbsolutePath());

        request.setGeneFileRequest(geneRequest);

        Reference testRef = referenceManager.registerGenome(request);
        assertNotNull(testRef);
        assertNotNull(testRef.getGeneFile());

        TestUtils.assertFail(() -> gffManager.unregisterGeneFile(testRef.getGeneFile().getId()),
                Collections.singletonList(DataIntegrityViolationException.class));
    }


    @Test @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
    public void testRegisterInvalidReference() throws IOException {
        String invalidReference = "double_chr.fa";
        Resource resource = context.getResource("classpath:templates/invalid/" + invalidReference);
        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setPath(resource.getFile().getPath());
        request.setName(invalidReference);
        request.setType(BiologicalDataItemResourceType.FILE);
        String errorMessage = "";
        String expectedMessage = "Input file must have contiguous chromosomes";
        try {
            referenceManager.registerGenome(request);
        } catch (IllegalArgumentException | AssertionError e) {
            errorMessage = e.getMessage();
        }
        //check that we received an appropriate message
        assertTrue(errorMessage.contains(expectedMessage));
        assertTrue(biologicalDataItemDao.loadFilesByNameStrict(invalidReference).isEmpty());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterWithSpecies() throws IOException {
        Resource resource = context.getResource(A3_FA_PATH);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("testReference1 " + this.getClass().getSimpleName());
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        Species species = new Species();
        species.setName("human");
        species.setVersion("hg19");
        referenceGenomeManager.registerSpecies(species);


        Species testSpecies = new Species();
        testSpecies.setVersion("hg19");
        request.setSpecies(testSpecies);

        Reference reference = referenceManager.registerGenome(request);
        assertNotNull(reference);
        assertNotNull(reference.getSpecies());
        assertEquals(species.getName(), reference.getSpecies().getName());
        assertEquals(species.getVersion(), reference.getSpecies().getVersion());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testGenbankRegister() throws IOException {
        Resource resource = context.getResource(GENBANK_PATH);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName("testReference1 " + this.getClass().getSimpleName());
        request.setPath(resource.getFile().getPath());
        request.setType(BiologicalDataItemResourceType.FILE);

        Reference reference = referenceManager.registerGenome(request);
        assertNotNull(reference);
        assertTrue(reference.getPath().endsWith(FastaUtils.DEFAULT_FASTA_EXTENSION));
        assertTrue(reference.getSource().endsWith(GenbankUtils.GENBANK_DEFAULT_EXTENSION));
    }
}
