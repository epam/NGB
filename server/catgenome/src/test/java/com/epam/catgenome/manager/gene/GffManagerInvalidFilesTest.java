package com.epam.catgenome.manager.gene;

import java.io.IOException;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.exception.FeatureFileReadingException;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import htsjdk.tribble.TribbleException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class GffManagerInvalidFilesTest  extends AbstractManagerTest {

    @Autowired
    private GffManager gffManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private ApplicationContext context;

    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private long referenceId;
    private Reference testReference;
    private Chromosome testChromosome;

    @Before
    public void setup() throws Exception {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.register(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRegisterUnsorted()
            throws IOException, FeatureFileReadingException {
        String invalidGff = "unsorted.gff";
        testRegisterInvalidFile("classpath:templates/invalid/" + invalidGff,
                MessageHelper.getMessage(MessagesConstants.ERROR_UNSORTED_FILE));
        //check that name is not reserved
        Assert.assertTrue(biologicalDataItemDao
                .loadFilesByNameStrict(invalidGff).isEmpty());
    }

    private void testRegisterInvalidFile(String path, String expectedMessage) throws IOException {
        String errorMessage = "";
        try {
            Resource resource = context.getResource(path);
            FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
            request.setPath(resource.getFile().getAbsolutePath());
            request.setReferenceId(referenceId);
            gffManager.registerGeneFile(request);
        } catch (TribbleException | IllegalArgumentException | AssertionError e) {
            errorMessage = e.getMessage();
        }
        //check that we received an appropriate message
        Assert.assertTrue(errorMessage.contains(expectedMessage));
    }

}
