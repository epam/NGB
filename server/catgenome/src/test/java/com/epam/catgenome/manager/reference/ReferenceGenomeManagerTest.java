package com.epam.catgenome.manager.reference;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.bed.BedFile;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Species;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.exception.FeatureIndexException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.bed.BedManager;
import com.epam.catgenome.manager.bed.BedManagerTest;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.epam.catgenome.manager.vcf.VcfManagerTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ReferenceGenomeManagerTest extends AbstractManagerTest {
    private static final String CLASSPATH_TEMPLATES_GENES_SORTED = "classpath:templates/genes_sorted.gtf";
    private static final String GENES_SORTED_BED_PATH = "classpath:templates/genes_sorted.bed";
    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private static final String SPECIES_NAME = "human";
    private static final String SPECIES_VERSION = "hg19";
    public static final long HOMO_TAX_ID = 9606L;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private BedManager bedManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private ApplicationContext context;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadAllReferenceGenomes() throws IOException {
        registerTestReference();

        Reference g38 = EntityHelper.createG38Reference(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.create(g38);

        List<Reference> loaded = referenceGenomeManager.loadAllReferenceGenomes();
        Assert.assertFalse(loaded.isEmpty());
        Assert.assertEquals(1, loaded.stream().filter(r -> r.getGeneFile() != null &&
                r.getGeneFile().getId() != null &&
                r.getGeneFile().getName() != null).count());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadAllReferenceGenomeByTaxId() throws IOException {
        registerTestReference();
        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);
        testSpecies.setTaxId(HOMO_TAX_ID);

        referenceGenomeManager.registerSpecies(testSpecies);


        Reference g38 = EntityHelper.createG38Reference(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.create(g38);
        referenceGenomeManager.updateSpecies(g38.getId(), testSpecies.getVersion());

        List<Reference> loaded = referenceGenomeManager.loadAllReferenceGenomesByTaxId(HOMO_TAX_ID);
        Assert.assertFalse(loaded.isEmpty());
        Assert.assertEquals(1, loaded.stream()
                .filter(reference -> reference.getSpecies().getTaxId() == HOMO_TAX_ID).count());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testAddAnnotationFiles() throws IOException, FeatureIndexException {
        Reference testReference = registerTestReference();
        List<BiologicalDataItem> emptyAnnotations =
                referenceGenomeManager.getReferenceAnnotationFiles(testReference.getId());
        Assert.assertTrue(emptyAnnotations.isEmpty());

        Set<BiologicalDataItem> expectedAnnotations = new HashSet<>();

        //add bed
        Resource bedFileResource = context.getResource(GENES_SORTED_BED_PATH);
        BedFile bedFile =
                BedManagerTest.registerTestBed(bedFileResource, testReference.getId(), bedManager);
        expectedAnnotations.add(bedFile);
        referenceGenomeManager.updateReferenceAnnotationFile(testReference.getId(),
                bedFile.getBioDataItemId(), false);
        List<BiologicalDataItem> actualAnnotations =
                referenceGenomeManager.getReferenceAnnotationFiles(testReference.getId());
        checkAnnotationPresent(expectedAnnotations, actualAnnotations);

        //add vcf
        Resource vcfFileResource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);
        VcfFile vcfFile = VcfManagerTest.registerVcf(vcfFileResource, testReference.getId(), vcfManager,
                null);
        referenceGenomeManager.updateReferenceAnnotationFile(testReference.getId(),
                vcfFile.getBioDataItemId(), false);
        expectedAnnotations.add(vcfFile);
        actualAnnotations =
                referenceGenomeManager.getReferenceAnnotationFiles(testReference.getId());
        checkAnnotationPresent(expectedAnnotations, actualAnnotations);

        //remove vcf
        referenceGenomeManager.updateReferenceAnnotationFile(testReference.getId(),
                vcfFile.getBioDataItemId(), true);
        expectedAnnotations.remove(vcfFile);
        actualAnnotations =
                referenceGenomeManager.getReferenceAnnotationFiles(testReference.getId());
        checkAnnotationPresent(expectedAnnotations, actualAnnotations);
    }

    private void checkAnnotationPresent(Set<BiologicalDataItem> expectedAnnotations,
            List<BiologicalDataItem> actualAnnotations) {
        Assert.assertEquals(expectedAnnotations.size(), actualAnnotations.size());
        actualAnnotations.forEach(annFile -> {
            Assert.assertTrue(expectedAnnotations.contains(annFile));
        });
    }

    private Reference registerTestReference() throws IOException {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);
        Long referenceId = testReference.getId();

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile testGeneFile = gffManager.registerGeneFile(request);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), testGeneFile.getId());
        return testReference;
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterSpecies() {
        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);

        referenceGenomeManager.registerSpecies(testSpecies);
        Species loadedSpecies = referenceGenomeManager.loadSpeciesByVersion(testSpecies.getVersion());

        Assert.assertNotNull(loadedSpecies);
        Assert.assertEquals(testSpecies.getName(), loadedSpecies.getName());
        Assert.assertEquals(testSpecies.getVersion(), loadedSpecies.getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRegisterSpeciesExists() {
        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);

        referenceGenomeManager.registerSpecies(testSpecies);
        referenceGenomeManager.registerSpecies(testSpecies);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadAllSpecies() {
        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);

        Species testSpecies1 = new Species();
        testSpecies1.setName("human");
        testSpecies1.setVersion("hg38");

        referenceGenomeManager.registerSpecies(testSpecies);
        referenceGenomeManager.registerSpecies(testSpecies1);

        List<Species> speciesList = referenceGenomeManager.loadAllSpecies();
        Assert.assertNotNull(speciesList);
        Assert.assertEquals(2, speciesList.size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUpdateSpecies() {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);

        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);

        referenceGenomeManager.registerSpecies(testSpecies);

        referenceGenomeManager.updateSpecies(testReference.getId(), testSpecies.getVersion());

        Reference reference = referenceGenomeManager.load(testReference.getId());
        Assert.assertNotNull(reference);
        Assert.assertNotNull(reference.getSpecies());
        Assert.assertEquals(testSpecies.getName(), reference.getSpecies().getName());
        Assert.assertEquals(testSpecies.getVersion(), reference.getSpecies().getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUpdateSpeciesWrongReferenceId() {
        referenceGenomeManager.updateSpecies(1L, "testVersion");
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUpdateSpeciesWrongSpecies() {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);
        referenceGenomeManager.updateSpecies(testReference.getId(), "testSpecies");
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRemoveSpeciesFromReference() {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);

        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);

        referenceGenomeManager.registerSpecies(testSpecies);

        referenceGenomeManager.updateSpecies(testReference.getId(), testSpecies.getVersion());
        referenceGenomeManager.updateSpecies(testReference.getId(), null);

        Reference reference = referenceGenomeManager.load(testReference.getId());
        Assert.assertNotNull(reference);
        Assert.assertNull(reference.getSpecies());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUnregisterSpecies() {
        Species testSpecies = new Species();
        testSpecies.setName(SPECIES_NAME);
        testSpecies.setVersion(SPECIES_VERSION);

        referenceGenomeManager.registerSpecies(testSpecies);
        referenceGenomeManager.unregisterSpecies(testSpecies.getVersion());
        Assert.assertNull(referenceGenomeManager.loadSpeciesByVersion(SPECIES_VERSION));
    }


    @Test(expected = IllegalArgumentException.class)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testUnregisterWrongSpecies() {
        referenceGenomeManager.unregisterSpecies(SPECIES_VERSION);
    }
}
