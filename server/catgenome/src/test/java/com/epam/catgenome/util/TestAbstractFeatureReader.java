package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import com.epam.catgenome.util.feature.reader.*;
import htsjdk.tribble.*;
import htsjdk.tribble.TribbleIndexedFeatureReader;
import htsjdk.tribble.bed.BEDCodec;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.util.TabixUtils;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author jacob
 * @date 2013-Apr-10
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class TestAbstractFeatureReader  {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EhCacheBasedIndexCache indexCache;

    final static String LOCAL_MIRROR_HTTP_INDEXED_VCF_PATH = "classpath:templates/ex2.vcf";
    final static String HTTP_INDEXED_VCF_PATH = "https://personal.broadinstitute.org/picard/testdata/ex2.vcf";
    final static String HTTP_INDEXED_VCF_IDX_PATH = "https://personal.broadinstitute.org/picard/testdata/ex2.vcf.idx";

    @Before
    public void setup() throws IOException {
        assertNotNull(context);
        assertNotNull(indexCache);
    }

    /**
     * Asserts readability and correctness of VCF over HTTP.  The VCF is indexed and requires and index.
     */
    @Test
    public void testVcfOverHTTP() throws IOException {
        final VCFCodec codec = new VCFCodec();
        final AbstractFeatureReader<VariantContext, LineIterator> featureReaderHttp =
                AbstractFeatureReader.getFeatureReader(HTTP_INDEXED_VCF_PATH, HTTP_INDEXED_VCF_IDX_PATH, codec, true, indexCache);

        String resource = context.getResource(LOCAL_MIRROR_HTTP_INDEXED_VCF_PATH).getFile().getAbsolutePath();
        final AbstractFeatureReader<VariantContext, LineIterator> featureReaderLocal =
                AbstractFeatureReader.getFeatureReader(resource, codec, false, indexCache);
        final CloseableTribbleIterator<VariantContext> localIterator = featureReaderLocal.iterator();
        for (final Feature feat : featureReaderHttp.iterator()) {
            assertEquals(feat.toString(), localIterator.next().toString());
        }
        assertFalse(localIterator.hasNext());
    }

    @Test
    public void testLoadBEDFTP() throws Exception {
        final String path = "ftp://ftp.broadinstitute.org/distribution/igv/TEST/cpgIslands with spaces.hg18.bed";
        final BEDCodec codec = new BEDCodec();
        final AbstractFeatureReader<BEDFeature, LineIterator> bfs =
                AbstractFeatureReader.getFeatureReader(path, codec, false, indexCache);
        for (final Feature feat : bfs.iterator()) {
            assertNotNull(feat);
        }
    }

    @Test
    public void testBlockCompressionExtensionString() {
        String testString = "testzip.gz";
        assertEquals(AbstractFeatureReader.hasBlockCompressedExtension(testString), true);
    }

    @Test
    public void testBlockCompressionExtensionFile() {
        String testString = "testzip.gz";
        assertEquals(AbstractFeatureReader.hasBlockCompressedExtension(new File(testString)), true);
    }

    @Test
    public void testBlockCompressionExtension() throws URISyntaxException {
        String testURIString = "https://www.googleapis.com/download/storage/v1/b/deflaux-public-test/o/NA12877.vcf.gz";
        URI testURI = URI.create(testURIString);
        assertEquals(AbstractFeatureReader.hasBlockCompressedExtension(testURI), true);
    }

    @Test
    public void testIndexedGZIPVCF() throws IOException {
        String vcfPath = "classpath:templates/test.vcf";
        String testPath = context.getResource(vcfPath).getFile().getAbsolutePath();
        final VCFCodec codec = new VCFCodec();
        try (final TribbleIndexedFeatureReader<VariantContext, LineIterator> featureReader =
                     new TribbleIndexedFeatureReader<>(testPath, codec, false)) {
            final CloseableTribbleIterator<VariantContext> localIterator = featureReader.iterator();
            int count = 0;
            for (final Feature feature : featureReader.iterator()) {
                localIterator.next();
                assertNotNull(feature);
                count++;
            }
            assertEquals(count, 5);
        }
    }

    @Test
    public void testStandardIndex() {
        final String vcf = "foo.vcf";
        final String expectedIndex = vcf + Tribble.STANDARD_INDEX_EXTENSION;

        assertEquals(Tribble.indexFile(vcf), expectedIndex);
        assertEquals(Tribble.indexFile(new File(vcf).getAbsolutePath()), new File(expectedIndex).getAbsolutePath());
    }

    @Test
    public void testTabixIndex() {
        final String vcf = "foo.vcf.gz";
        final String expectedIndex = vcf + TabixUtils.STANDARD_INDEX_EXTENSION;

        assertEquals(Tribble.tabixIndexFile(vcf), expectedIndex);
        assertEquals(Tribble.tabixIndexFile(new File(vcf).getAbsolutePath()), new File(expectedIndex).getAbsolutePath());
    }
}
