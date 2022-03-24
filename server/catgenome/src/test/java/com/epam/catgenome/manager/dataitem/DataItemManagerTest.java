/*
 * MIT License
 *
 * Copyright (c) 2016-2022 EPAM Systems
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

package com.epam.catgenome.manager.dataitem;

import java.io.File;
import java.io.IOException;
import com.epam.catgenome.entity.BiologicalDataItem;
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

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.controller.vo.registration.FeatureIndexedFileRegistrationRequest;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.vcf.VcfFile;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.manager.vcf.VcfFileManager;
import com.epam.catgenome.manager.vcf.VcfManager;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public class DataItemManagerTest extends AbstractManagerTest {

    @Autowired
    private DataItemManager dataItemManager;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private VcfManager vcfManager;

    @Autowired
    private VcfFileManager vcfFileManager;

    @Autowired
    private ApplicationContext context;

    private static final int TEST_CHROMOSOME_SIZE = 239107476;

    private static final String CLASSPATH_TEMPLATES_FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final String TMP_FILE_NAME = "tmp";

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteReference() throws IOException {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());
        File tmp = File.createTempFile(TMP_FILE_NAME, TMP_FILE_NAME);
        testReference.setPath(tmp.getAbsolutePath());
        referenceGenomeManager.create(testReference);
        dataItemManager.deleteFileByBioItemId(testReference.getBioDataItemId());
    }

    @Test
    public void testDelete() throws IOException {
        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());
        File tmp = File.createTempFile(TMP_FILE_NAME, TMP_FILE_NAME);
        testReference.setPath(tmp.getAbsolutePath());
        referenceGenomeManager.create(testReference);

        Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);
        FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(testReference.getId());
        request.setPath(resource.getFile().getAbsolutePath());
        VcfFile vcfFile = vcfManager.registerVcfFile(request);
        dataItemManager.deleteFileByBioItemId(vcfFile.getBioDataItemId());
        VcfFile loadedVcfFile = vcfFileManager.load(vcfFile.getId());
        Assert.assertNull(loadedVcfFile);
    }

    @Test
    public void testRename() throws IOException {
        final Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        final Reference testReference = EntityHelper.createNewReference(testChromosome,
                referenceGenomeManager.createReferenceId());
        final File tmp = File.createTempFile(TMP_FILE_NAME, TMP_FILE_NAME);
        testReference.setPath(tmp.getAbsolutePath());
        referenceGenomeManager.create(testReference);

        final Resource resource = context.getResource(CLASSPATH_TEMPLATES_FELIS_CATUS_VCF);
        final FeatureIndexedFileRegistrationRequest request = new FeatureIndexedFileRegistrationRequest();
        request.setReferenceId(testReference.getId());
        request.setPath(resource.getFile().getAbsolutePath());
        final VcfFile vcfFile = vcfManager.registerVcfFile(request);

        dataItemManager.renameFile(testReference.getName(), "newRefName", "newRefPrettyName");
        final BiologicalDataItem refItem = dataItemManager.findFileByBioItemId(testReference.getBioDataItemId());
        Assert.assertEquals("newRefName", refItem.getName());
        Assert.assertEquals("newRefPrettyName", refItem.getPrettyName());

        dataItemManager.renameFile(vcfFile.getName(), "newVCFName", "newVCFPrettyName");
        final BiologicalDataItem vcfItem = dataItemManager.findFileByBioItemId(vcfFile.getBioDataItemId());
        Assert.assertEquals("newVCFName", vcfItem.getName());
        Assert.assertEquals("newVCFPrettyName", vcfItem.getPrettyName());
    }
}
