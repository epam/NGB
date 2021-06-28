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

package com.epam.catgenome.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.epam.catgenome.dao.reference.SpeciesDao;
import com.epam.catgenome.entity.reference.Species;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.unitils.reflectionassert.ReflectionAssert;

import com.epam.catgenome.dao.gene.GeneFileDao;
import com.epam.catgenome.dao.reference.ReferenceGenomeDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemFormat;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.helper.EntityHelper;

/**
 * Source:      ReferenceGenomeDaoTest.java
 * Created:     11/2/15, 1:40 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code ReferenceGenomeDaoTest} is designed to test different calls to support metadata
 * management for reference genomes, e.g. save new entities, retrieves reference genomes
 * and corresponded chromosomes matched different conditions.
 *
 * @author Denis Medvedev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ReferenceGenomeDaoTest extends AbstractDaoTest {
    @Autowired
    private ReferenceGenomeDao referenceGenomeDao;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private GeneFileDao geneFileDao;

    @Autowired
    private SpeciesDao speciesDao;

    @Before
    @Override
    public void setup() throws Exception {
        assertNotNull("ReferenceGenomeDao isn't provided.", referenceGenomeDao);
        super.setup();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testReferenceGenomeManagement() {
        // clear the list of chromosomes for test purposes
        reference.setChromosomes(Collections.emptyList());
        // tests that a reference genome created in 'setup' can be retrieved by the known ID
        final Reference foundById = referenceGenomeDao.loadReferenceGenome(reference.getId());
        assertNotNull("Reference genome isn't found by the given ID.", foundById);
        ReflectionAssert.assertReflectionEquals("Unexpected reference genome is loaded by ID.", reference, foundById);
        // tests that a reference genome created in 'setup' can be retrieved in the list which contains
        // all chromosomes registered in the system
        final List<Reference> allReferenceGenomes = referenceGenomeDao.loadAllReferenceGenomes();
        assertTrue("Collection of available reference genomes is empty, but should contain at least one value.",
                CollectionUtils.isNotEmpty(allReferenceGenomes));
        final Optional<Reference> foundInList = allReferenceGenomes
                .stream()
                .filter(e -> reference.getId().equals(e.getId()))
                .findFirst();
        assertTrue("Reference genome isn't found by ID in the retrieved list.", foundInList.isPresent());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testChromosomeManagement() {
        final List<Chromosome> chromosomes = reference.getChromosomes();
        // tests that batch of chromosomes are saved and IDs are assigned
        final int count = chromosomes.size();
        final int[] result = referenceGenomeDao.saveChromosomes(reference.getId(), chromosomes);
        assertEquals("Unexpected batch size.", count, result.length);
        for (int i = 0; i < count; i++) {
            assertEquals("Unexpected number of affected rows after chromosome has been saved successfully.",
                    1, result[i]);
            Chromosome entity = chromosomes.get(i);
            assertNotNull("Chromosome ID has been assigned.", entity.getId());
            assertNotNull("Reference ID has been assigned.", entity.getReferenceId());
        }

        final Chromosome chromosome = chromosomes.get(0);

        // tests that a chromosome can be retrieved by the given ID
        final Chromosome foundById = referenceGenomeDao.loadChromosome(chromosome.getId());
        assertNotNull("Chromosome isn't found by the given ID.", foundById);
        ReflectionAssert.assertReflectionEquals("Unexpected chromosome is loaded by ID.", chromosome, foundById);
        // tests that all chromosomes related to a reference genome with the given ID can be retrieved
        final List<Chromosome> allChromosomes = referenceGenomeDao.loadAllChromosomesByReferenceId(reference.getId());
        assertTrue("Collection of chromosomes is empty, but it should contain more than 1 value.",
                CollectionUtils.isNotEmpty(allChromosomes));
        assertEquals("Unexpected number of chromosomes for the given reference is detected.",
                count, allChromosomes.size());
        final Optional<Chromosome> foundInList = allChromosomes
                .stream()
                .filter(e -> chromosome.getId().equals(e.getId()))
                .findFirst();
        assertTrue("Chromosome isn't found by ID in the retrieved list.", foundInList.isPresent());
        ReflectionAssert.assertReflectionEquals("Unexpected chromosome is found in the list.",
                chromosome, foundInList.get());
    }


    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testNewFileRegister() {
        Reference newReference = getTestReference();

        final Long referenceId = newReference.getId();
        newReference.setName(newReference.getName() + referenceId);
        biologicalDataItemDao.createBiologicalDataItem(newReference);
        referenceGenomeDao.createReferenceGenome(newReference, referenceId);

        final Reference newReferenceGenomeFile = referenceGenomeDao
                .loadReferenceGenome(newReference.getId());
        assertNotNull("Reference isn't found by the given ID.", newReferenceGenomeFile);
        assertEquals("not equals param", newReference.getName(), newReferenceGenomeFile.getName());
        assertEquals("not equals param", newReference.getPath(), newReferenceGenomeFile.getPath());
        assertEquals("not equals param", newReference.getCreatedDate(), newReferenceGenomeFile.getCreatedDate());
        assertEquals(newReference.getOwner(), newReferenceGenomeFile.getOwner());

        final List<Reference> newList = referenceGenomeDao.loadAllReferenceGenomes();
        assertNotNull(newList);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadReferenceBioItemID() {
        Reference testReference = getTestReference();

        final Long referenceId = testReference.getId();
        testReference.setName(testReference.getName() + referenceId);
        biologicalDataItemDao.createBiologicalDataItem(testReference);
        referenceGenomeDao.createReferenceGenome(testReference, referenceId);

        final Reference newReferenceGenomeFile = referenceGenomeDao
                .loadReferenceGenomeByBioItemId(testReference.getBioDataItemId());
        assertNotNull("Reference isn't found by the given ID.", newReferenceGenomeFile);
        assertEquals(testReference.getName(), newReferenceGenomeFile.getName());
        assertEquals(testReference.getPath(), newReferenceGenomeFile.getPath());
        assertEquals(testReference.getCreatedDate(), newReferenceGenomeFile.getCreatedDate());

    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void testSaveLoadGeneFile() {
        Reference testReference = getTestReference();

        final Long referenceId = testReference.getId();
        testReference.setName(testReference.getName() + referenceId);
        biologicalDataItemDao.createBiologicalDataItem(testReference);
        referenceGenomeDao.createReferenceGenome(testReference, referenceId);

        GeneFile geneFile = new GeneFile();

        geneFile.setId(geneFileDao.createGeneFileId());
        geneFile.setName("testFile");
        geneFile.setCreatedDate(new Date());
        geneFile.setReferenceId(testReference.getId());
        geneFile.setType(BiologicalDataItemResourceType.FILE);
        geneFile.setFormat(BiologicalDataItemFormat.GENE);
        geneFile.setPath("///");
        geneFile.setSource("///");
        geneFile.setOwner(EntityHelper.TEST_OWNER);

        BiologicalDataItem index = EntityHelper.createIndex(BiologicalDataItemFormat.GENE_INDEX,
                                                            BiologicalDataItemResourceType.FILE, "////");
        geneFile.setIndex(index);

        biologicalDataItemDao.createBiologicalDataItem(index);
        final Long realId = geneFile.getId();
        biologicalDataItemDao.createBiologicalDataItem(geneFile);
        geneFileDao.createGeneFile(geneFile, realId);

        referenceGenomeDao.updateReferenceGeneFileId(referenceId, geneFile.getId());

        Reference loadedReference = referenceGenomeDao.loadReferenceGenome(testReference.getId());
        assertNotNull(loadedReference.getGeneFile());
        assertEquals(geneFile.getId(), loadedReference.getGeneFile().getId());

        loadedReference = referenceGenomeDao.loadReferenceGenomeByBioItemId(testReference.getBioDataItemId());
        assertNotNull(loadedReference.getGeneFile());
        assertEquals(geneFile.getId(), loadedReference.getGeneFile().getId());

        Reference testReference2 = getTestReference();
        testReference2.setGeneFile(geneFile);

        final Long referenceId2 = testReference2.getId();
        testReference2.setName(testReference2.getName() + referenceId2);
        biologicalDataItemDao.createBiologicalDataItem(testReference2);
        referenceGenomeDao.createReferenceGenome(testReference2, referenceId2);

        loadedReference = referenceGenomeDao.loadReferenceGenome(testReference2.getId());
        assertNotNull(loadedReference.getGeneFile());
        assertEquals(geneFile.getId(), loadedReference.getGeneFile().getId());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUpdateSpecies() {
        Reference testReference = getTestReference();

        final Long referenceId = testReference.getId();
        testReference.setName(testReference.getName() + referenceId);
        biologicalDataItemDao.createBiologicalDataItem(testReference);
        referenceGenomeDao.createReferenceGenome(testReference, referenceId);

        Species testSpecies = new Species();
        testSpecies.setName("human");
        testSpecies.setVersion("hg19");

        speciesDao.saveSpecies(testSpecies);
        referenceGenomeDao.updateSpecies(testReference.getId(), testSpecies.getVersion());

        Reference loadedReference = referenceGenomeDao.loadReferenceGenome(testReference.getId());
        assertNotNull(loadedReference.getSpecies());
        assertEquals(testSpecies.getName(), loadedReference.getSpecies().getName());
        assertEquals(testSpecies.getVersion(), loadedReference.getSpecies().getVersion());
    }

    @NotNull private Reference getTestReference() {
        Reference newReference = new Reference();

        newReference.setSize(reference.getSize());
        newReference.setName(reference.getName());
        newReference.setPath(reference.getPath());
        newReference.setSource(reference.getPath());
        newReference.setType(BiologicalDataItemResourceType.FILE);
        newReference.setId(referenceGenomeDao.createReferenceGenomeId());

        newReference.setCreatedDate(reference.getCreatedDate());
        newReference.setIndex(createReferenceIndex());
        newReference.setOwner(EntityHelper.TEST_OWNER);
        return newReference;
    }
}
