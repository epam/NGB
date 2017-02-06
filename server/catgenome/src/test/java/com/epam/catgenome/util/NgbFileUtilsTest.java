package com.epam.catgenome.util;

import org.junit.Assert;
import org.junit.Test;

import com.epam.catgenome.entity.BiologicalDataItemFormat;

/**
 * Source:      FileUtilsTest
 * Created:     19.01.17, 18:29
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
public class NgbFileUtilsTest {
    @Test
    public void testGetFileExtension() {
        Assert.assertEquals(".vcf", NgbFileUtils.getFileExtension("vcf_file.vcf"));
        Assert.assertEquals(".vcf.gz", NgbFileUtils.getFileExtension("vcf_file.vcf.gz"));
        Assert.assertEquals(".vcf.gz.tbi", NgbFileUtils.getFileExtension("vcf_file.vcf.gz.tbi"));
        Assert.assertEquals(".vcf.idx", NgbFileUtils.getFileExtension("vcf_file.vcf.idx"));

        Assert.assertEquals(".tbi", NgbFileUtils.getFileExtension("index.tbi"));
        Assert.assertEquals(".tbi", NgbFileUtils.getFileExtension("index.foo.tbi"));
    }

    @Test
    public void getFormatByExtension() {
        Assert.assertEquals(BiologicalDataItemFormat.VCF, NgbFileUtils.getFormatByExtension("vcf_file.vcf"));
        Assert.assertEquals(BiologicalDataItemFormat.VCF, NgbFileUtils.getFormatByExtension("vcf_file.vcf.gz"));
        Assert.assertEquals(BiologicalDataItemFormat.VCF_INDEX, NgbFileUtils.getFormatByExtension(
            "vcf_file.vcf.gz.tbi"));
        Assert.assertEquals(BiologicalDataItemFormat.VCF_INDEX, NgbFileUtils.getFormatByExtension("vcf_file.vcf.idx"));

        Assert.assertEquals(BiologicalDataItemFormat.INDEX, NgbFileUtils.getFormatByExtension("index.tbi"));
        Assert.assertEquals(BiologicalDataItemFormat.INDEX, NgbFileUtils.getFormatByExtension("index.foo.tbi"));
    }
}