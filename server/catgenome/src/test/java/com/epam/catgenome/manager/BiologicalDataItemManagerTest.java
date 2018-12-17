package com.epam.catgenome.manager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.util.Utils;
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

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.project.Project;
import com.epam.catgenome.entity.project.ProjectItem;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.project.ProjectManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Source:      BiologicalDataItemManagerTest
 * Created:     31.01.17, 14:41
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BiologicalDataItemManagerTest extends AbstractManagerTest {
    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";

    @Autowired
    private BiologicalDataItemManager biologicalDataItemManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private ApplicationContext context;

    private Chromosome testChromosome;
    private Reference testReference;
    private Long referenceId;

    @Before
    public void setup() {
        testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference = EntityHelper.createNewReference(testChromosome, referenceGenomeManager.createReferenceId());

        referenceGenomeManager.create(testReference);
        referenceId = testReference.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testGenerateUrl() throws IOException {

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setName("testVcf1");
        request.setPath(resource.getFile().getAbsolutePath());

        VcfFile testVcf1 = vcfManager.registerVcfFile(request);

        Project testProject = new Project();
        testProject.setName("testProject1");
        testProject.setItems(Arrays.asList(new ProjectItem(testVcf1),
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        projectManager.create(testProject);
        request.setName("testVcf2");
        VcfFile testVcf2 = vcfManager.registerVcfFile(request);
        Project testProject2 = new Project();
        testProject2.setName("testProject2");
        testProject2.setItems(Arrays.asList(new ProjectItem(testVcf2),
                new ProjectItem(new BiologicalDataItem(testReference.getBioDataItemId()))));

        projectManager.create(testProject2);
        //first dataset with full location
        String url = biologicalDataItemManager.generateUrl(testProject.getName(),
                Collections.singletonList(testVcf1.getBioDataItemId().toString()),
                testChromosome.getName(), 1, TEST_CHROMOSOME_SIZE);
        Assert.assertNotNull(url);

        checkUrl(url, Collections.singletonList(testVcf1.getName()));

        Assert.assertEquals(url, biologicalDataItemManager.generateUrl(testProject.getId().toString(),
                Collections.singletonList(testVcf1.getName()),
                testChromosome.getName(), 1, TEST_CHROMOSOME_SIZE));


        //second dataset with full location
        url = biologicalDataItemManager.generateUrl(testProject2.getName(),
                Collections.singletonList(testVcf2.getBioDataItemId().toString()),
                testChromosome.getName(), 1, TEST_CHROMOSOME_SIZE);
        Assert.assertNotNull(url);

        checkUrl(url, Collections.singletonList(testVcf2.getName()));

        Assert.assertEquals(url, biologicalDataItemManager.generateUrl(testProject2.getId().toString(),
                Collections.singletonList(testVcf2.getName()),
                testChromosome.getName(), 1, TEST_CHROMOSOME_SIZE));

        String chrUrl = biologicalDataItemManager.generateUrl(testProject.getId().toString(),
                Collections.singletonList(testVcf1.getBioDataItemId().toString()),
                testChromosome.getName(), null, null);
        Assert.assertNotNull(chrUrl);
        checkChrUrl(chrUrl, Collections.singletonList(testVcf1.getName()));

        String shortUrl = biologicalDataItemManager.generateUrl(testProject2.getName(),
                Collections.singletonList(testVcf2.getBioDataItemId().toString()),
                null, null, null);
        Assert.assertNotNull(shortUrl);
        checkShortUrl(shortUrl, Arrays.asList(testVcf2.getName()));


        url = biologicalDataItemManager.generateUrl(testProject.getName(),
                Collections.singletonList(testVcf1.getBioDataItemId().toString()),
                Utils.changeChromosomeName(testChromosome.getName()), 1, TEST_CHROMOSOME_SIZE);
        Assert.assertNotNull(url);
    }

    private void checkUrl(String url, List<String> itemNames) throws IOException {
        String[] tokens = url.split("/");
        Assert.assertEquals(6, tokens.length);
        Assert.assertEquals(testReference.getName(), tokens[2]);
        Assert.assertEquals(testChromosome.getName(), tokens[3]);
        Assert.assertEquals(1, Integer.parseInt(tokens[4]));

        String[] subTokens = tokens[5].split("\\?");
        Assert.assertEquals(TEST_CHROMOSOME_SIZE, Integer.parseInt(subTokens[0]));

        checkTracks(subTokens[1], itemNames);
    }

    private void checkTracks(String tracksChunk, List<String> itemNames) throws IOException {
        Assert.assertTrue(tracksChunk.startsWith("tracks="));
        JsonMapper jsonMapper = new JsonMapper();
        List<BiologicalDataItemManager.TrackVO> vos = jsonMapper.readValue(tracksChunk.substring(7),
                                                               TypeFactory.defaultInstance().constructParametrizedType(
                                                                           List.class, List.class,
                                                                           BiologicalDataItemManager.TrackVO.class));
        Assert.assertEquals(itemNames.size(), vos.size());
        for (String id : itemNames) {
            Assert.assertTrue(vos.stream().anyMatch(v -> v.getB().equals(id) && v.getP() != null));
        }
    }

    private void checkChrUrl(String url, List<String> itemNames) throws IOException {
        String[] tokens = url.split("/");
        Assert.assertEquals(4, tokens.length);
        Assert.assertEquals(testReference.getName(), tokens[2]);

        String[] subTokens = tokens[3].split("\\?");
        Assert.assertEquals(testChromosome.getName(), subTokens[0]);

        checkTracks(subTokens[1], itemNames);
    }

    private void checkShortUrl(String url, List<String> itemNames) throws IOException {
        String[] tokens = url.split("/");
        Assert.assertEquals(3, tokens.length);

        String[] subTokens = tokens[2].split("\\?");
        Assert.assertEquals(testReference.getName(), subTokens[0]);

        checkTracks(subTokens[1], itemNames);
    }
}
