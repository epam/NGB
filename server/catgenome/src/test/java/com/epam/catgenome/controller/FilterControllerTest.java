package com.epam.catgenome.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractControllerTest;
import com.epam.catgenome.common.ResponseResult;
import com.epam.catgenome.controller.vo.GeneSearchQuery;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.Group;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.entity.vcf.VcfFilterForm;
import com.epam.catgenome.entity.vcf.VcfFilterInfo;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfManager;

/**
 * Source:      FilterControllerTest
 * Created:     23.01.17, 13:02
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class FilterControllerTest extends AbstractControllerTest {
    private static final int TEST_CHROMOSOME_SIZE = 239107476;
    private Reference testReference;
    private Long referenceId;

    private GeneFile geneFile;
    private VcfFile vcfFile;

    private static final String URL_FILTER_SEARCH_GENES = "/filter/searchGenes";
    private static final String URL_FILTER = "/filter";
    private static final String URL_FILTER_INFO = "/filter/info";
    private static final String URL_FILTER_GROUP = "/filter/group";

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private GffManager gffManager;

    @Autowired
    private VcfManager vcfManager;

    @Before
    public void setup() throws Exception {
        super.setup();

        referenceId = referenceGenomeManager.createReferenceId();
        testReference = new Reference();
        testReference.setId(referenceId);
        testReference.setName("testReference " + this.getClass().getSimpleName());
        testReference.setSize(0L);
        testReference.setPath("");
        testReference.setCreatedDate(new Date());

        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testReference.setChromosomes(Collections.singletonList(testChromosome));

        referenceGenomeManager.register(testReference);

        Resource resource = wac.getResource(CLASSPATH_TEMPLATES_GENES_SORTED);

        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        geneFile = gffManager.registerGeneFile(request);
        Assert.assertNotNull(geneFile);
        Assert.assertNotNull(geneFile.getId());

        referenceGenomeManager.updateReferenceGeneFileId(testReference.getId(), geneFile.getId());

        resource = wac.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);

        request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(referenceId);
        request.setPath(resource.getFile().getAbsolutePath());

        vcfFile = vcfManager.registerVcfFile(request);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    public void testSearchGenesInProject() throws Exception {
        GeneSearchQuery geneSearchQuery = new GeneSearchQuery();
        geneSearchQuery.setSearch("ENS");
        geneSearchQuery.setVcfIds(Collections.singletonList(vcfFile.getId()));

        ResultActions actions = mvc()
            .perform(post(URL_FILTER_SEARCH_GENES).content(
                getObjectMapper().writeValueAsString(geneSearchQuery)).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
            .andDo(MockMvcResultHandlers.print());

        ResponseResult<Set<String>> geneNamesAvailable = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  getTypeFactory().constructParametrizedType(Set.class,
                                                                                             Set.class, String.class)));

        Assert.assertFalse(geneNamesAvailable.getPayload().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    public void testGetFieldInfo() throws Exception {
        ResultActions actions = mvc()
            .perform(post(URL_FILTER_INFO).content(getObjectMapper().writeValueAsBytes(
                Collections.singletonList(vcfFile.getId()))).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<VcfFilterInfo> infoRes = getObjectMapper().readValue(
            actions.andReturn().getResponse().getContentAsByteArray(),
            getTypeFactory().constructParametrizedType(ResponseResult.class,
                                                       ResponseResult.class, VcfFilterInfo.class));

        Assert.assertNotNull(infoRes.getPayload());
        Assert.assertFalse(infoRes.getPayload().getAvailableFilters().isEmpty());
        Assert.assertNull(infoRes.getPayload().getInfoItemMap());
        Assert.assertFalse(infoRes.getPayload().getInfoItems().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    public void testFilterVcf() throws Exception {
        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));
        vcfFilterForm.setGenes(new VcfFilterForm.FilterSection<>(Collections.singletonList("ENS"), false));
        vcfFilterForm.setVariationTypes(new VcfFilterForm.FilterSection<>(Arrays.asList(VariationType.MNP,
                                                                                VariationType.INS), false));

        ResultActions actions = mvc()
            .perform(post(URL_FILTER).content(
                getObjectMapper().writeValueAsString(vcfFilterForm)).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
            .andDo(MockMvcResultHandlers.print());

        ResponseResult<List<FeatureIndexEntry>> filterRes = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  getTypeFactory().constructParametrizedType(List.class,
                                                                                                             List.class,
                                                                                             VcfIndexEntry.class)));

        Assert.assertFalse(filterRes.getPayload().isEmpty());

        // filter by additional fields
        Map<String, Object> additionalFilters = new HashMap<>();
        additionalFilters.put("SVTYPE", "BND");
        vcfFilterForm.setAdditionalFilters(additionalFilters);
        vcfFilterForm.setGenes(null);
        vcfFilterForm.setVariationTypes(null);

        actions = mvc()
            .perform(post(URL_FILTER).content(
                getObjectMapper().writeValueAsString(vcfFilterForm)).contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()))
            .andDo(MockMvcResultHandlers.print());

        filterRes = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                                                  getTypeFactory().constructParametrizedType(List.class,
                                                                                                             List.class,
                                                                                                 VcfIndexEntry.class)));

        Assert.assertFalse(filterRes.getPayload().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    public void testGroupVariations() throws Exception {
        VcfFilterForm vcfFilterForm = new VcfFilterForm();
        vcfFilterForm.setVcfFileIds(Collections.singletonList(vcfFile.getId()));

        ResultActions actions = mvc()
            .perform(post(URL_FILTER_GROUP).content(getObjectMapper().writeValueAsString(vcfFilterForm))
                         .param("groupBy", "VARIATION_TYPE")
                         .contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(EXPECTED_CONTENT_TYPE))
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_PAYLOAD).exists())
            .andExpect(MockMvcResultMatchers.jsonPath(JPATH_STATUS).value(ResultStatus.OK.name()));
        actions.andDo(MockMvcResultHandlers.print());

        ResponseResult<List<Group>> groupRes = getObjectMapper()
            .readValue(actions.andReturn().getResponse().getContentAsByteArray(),
                       getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                                          getTypeFactory().constructParametrizedType(List.class, List.class,
                                                 Group.class)));

        Assert.assertFalse(groupRes.getPayload().isEmpty());
    }

}