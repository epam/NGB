package com.epam.catgenome.manager.reference;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
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
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class ReferenceGenomeManagerTest extends AbstractManagerTest {
    private static final String CLASSPATH_TEMPLATES_GENES_SORTED = "classpath:templates/genes_sorted.gtf";
    private static final int TEST_CHROMOSOME_SIZE = 239107476;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private ApplicationContext context;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testLoadAllReferenceGenomes() throws IOException {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        Long referenceId = testReference.getId();

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        GeneFile testGeneFile = gffManager.registerGeneFile(request);

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), testGeneFile.getId());

        Reference g38 = EntityHelper.createG38Reference(referenceGenomeManager.createReferenceId());
        referenceGenomeManager.register(g38);

        List<Reference> loaded = referenceGenomeManager.loadAllReferenceGenomes();
        Assert.assertFalse(loaded.isEmpty());
        Assert.assertEquals(1, loaded.stream().filter(r -> r.getGeneFile() != null &&
                r.getGeneFile().getId() != null &&
                r.getGeneFile().getName() != null).count());
    }
}
