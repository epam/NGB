/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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


package com.epam.catgenome.util;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationEffect;
import com.epam.catgenome.entity.vcf.VariationImpact;
import com.epam.catgenome.entity.vcf.VariationType;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import htsjdk.tribble.FeatureReader;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.testng.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration()
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
public class DiskBasedListTest {

    private static final int MAX_IN_MEMORY_ITEMS_COUNT = 10;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EhCacheBasedIndexCache indexCache;

    @Test
    public void serialisationTest() throws IOException, ClassNotFoundException {
        Resource resource = context.getResource("classpath:templates/samples.vcf");
        List<VcfIndexEntry> writtenEntries = new ArrayList<>();
        List<VcfIndexEntry> diskBasedList = new DiskBasedList<VcfIndexEntry>(MAX_IN_MEMORY_ITEMS_COUNT).adaptToList();

        try (FeatureReader<VariantContext> reader = AbstractFeatureReader
                .getFeatureReader(resource.getFile().getAbsolutePath(), new VCFCodec(), false, indexCache)
        ) {

            for (VariantContext variantContext : reader.iterator()) {

                VcfIndexEntry vcfIndexEntry = createTestEntry(variantContext);

                writtenEntries.add(vcfIndexEntry);
                diskBasedList.add(vcfIndexEntry);
            }
        }

        for (int i = 0; i < 3; i++) {
            assertLists(writtenEntries, diskBasedList);
        }

    }

    private void assertLists(List<VcfIndexEntry> writtenEntries, List<VcfIndexEntry> diskBasedList) {
        Iterator<VcfIndexEntry> writtenEntriesIterator = writtenEntries.iterator();
        for (VcfIndexEntry fromDiskBasedList : diskBasedList) {
            VcfIndexEntry writtenEntry = writtenEntriesIterator.next();

            Assert.assertEquals(fromDiskBasedList.getVariationType(), writtenEntry.getVariationType());
            Assert.assertEquals(fromDiskBasedList.getGene(), writtenEntry.getGene());
            Assert.assertEquals(fromDiskBasedList.getGeneIds(), writtenEntry.getGeneIds());
            Assert.assertEquals(fromDiskBasedList.getGeneName(), writtenEntry.getGeneName());
            Assert.assertEquals(fromDiskBasedList.getImpact(), writtenEntry.getImpact());
            Assert.assertEquals(fromDiskBasedList.getEffect(), writtenEntry.getEffect());

            Assert.assertEquals(
                    fromDiskBasedList.getInfo().get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName()),
                    writtenEntry.getInfo().get(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName()));

            VariantContext deserializedVariantContext = fromDiskBasedList.getVariantContext();
            VariantContext writtenVariantContext = writtenEntry.getVariantContext();

            Assert.assertEquals(deserializedVariantContext.getContig(), writtenVariantContext.getContig());
            Assert.assertEquals(deserializedVariantContext.getAlleles(), writtenVariantContext.getAlleles());
            Assert.assertEquals(deserializedVariantContext.getAlleles(), writtenVariantContext.getAlleles());
        }
    }

    @NotNull
    private VcfIndexEntry createTestEntry(VariantContext variantContext) {
        VcfIndexEntry vcfIndexEntry = new VcfIndexEntry();

        vcfIndexEntry.setVariationType(VariationType.DEL);
        vcfIndexEntry.setGene("TestGene");
        vcfIndexEntry.setGeneIds("TestGeneIsd");
        vcfIndexEntry.setGeneName("TestGeneName");
        vcfIndexEntry.setImpact(VariationImpact.HIGH);
        vcfIndexEntry.setEffect(VariationEffect.INTRON);

        vcfIndexEntry.setInfo(new HashMap<>());
        vcfIndexEntry.getInfo().put(FeatureIndexDao.FeatureIndexFields.IS_EXON.getFieldName(), true);

        vcfIndexEntry.setVariantContext(variantContext);
        return vcfIndexEntry;
    }
}
