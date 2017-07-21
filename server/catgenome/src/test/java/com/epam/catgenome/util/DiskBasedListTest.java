package com.epam.catgenome.util;

import com.epam.catgenome.dao.index.FeatureIndexDao;
import com.epam.catgenome.entity.index.VcfIndexEntry;
import com.epam.catgenome.entity.vcf.VariationEffect;
import com.epam.catgenome.entity.vcf.VariationImpact;
import com.epam.catgenome.entity.vcf.VariationType;
import htsjdk.tribble.AbstractFeatureReader;
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

    @Test
    public void serialisationTest() throws IOException, ClassNotFoundException {
        Resource resource = context.getResource("classpath:templates/samples.vcf");
        List<VcfIndexEntry> writtenEntries = new ArrayList<>();
        List<VcfIndexEntry> diskBasedList = new DiskBasedList<VcfIndexEntry>(MAX_IN_MEMORY_ITEMS_COUNT).adaptToList();

        try (FeatureReader<VariantContext> reader = AbstractFeatureReader
                .getFeatureReader(resource.getFile().getAbsolutePath(), new VCFCodec(), false)
        ) {

            for (VariantContext variantContext : reader.iterator()) {

                VcfIndexEntry vcfIndexEntry = createTestEntry(variantContext);

                writtenEntries.add(vcfIndexEntry);
                diskBasedList.add(vcfIndexEntry);
            }
        }

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
