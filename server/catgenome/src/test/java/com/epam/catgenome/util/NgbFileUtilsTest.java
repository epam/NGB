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

    /**
     * MAF support was removed from NGB, so a MAF path resolves to no format and is not a supported
     * file, exactly like any other extension NGB does not know.
     */
    @Test
    public void mafExtensionsAreNotRecognized() {
        Assert.assertNull(NgbFileUtils.getFormatByExtension("mutations.maf"));
        Assert.assertNull(NgbFileUtils.getFormatByExtension("mutations.maf.gz"));

        Assert.assertFalse(NgbFileUtils.isFileSupported("mutations.maf"));
        Assert.assertFalse(NgbFileUtils.isFileSupported("mutations.maf.gz"));

        // a leftover MAF index is now just an index of an unknown format, as index.foo.tbi is above
        Assert.assertEquals(BiologicalDataItemFormat.INDEX, NgbFileUtils.getFormatByExtension("mutations.maf.tbi"));
    }
}