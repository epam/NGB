package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import com.epam.catgenome.util.feature.reader.*;
import com.epam.catgenome.util.feature.reader.TabixFeatureReader;
import com.epam.catgenome.util.feature.reader.TribbleIndexedFeatureReader;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import htsjdk.tribble.*;
import htsjdk.tribble.bed.BEDCodec;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.tribble.readers.PositionalBufferedStream;
import htsjdk.tribble.util.TabixUtils;
import htsjdk.variant.bcf2.BCF2Codec;
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

import static org.junit.Assert.*;

/**
 * Copied from HTSJDK library and added tests for cases of load indexes and constructors.
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

    private static final String LOCAL_MIRROR_HTTP_INDEXED_VCF_PATH = "classpath:templates/ex2.vcf";
    private static final String HTTP_INDEXED_VCF_PATH = "https://personal.broadinstitute.org/picard/testdata/ex2.vcf";
    private static final String HTTP_INDEXED_VCF_PATH_WITH_QUESTIONS =
            "https://personal.broadinstitute.org/picard/testdata/ex2.vcf?PARAMETER=1?PARAMETER=2";
    private static final String HTTP_INDEXED_VCF_IDX_PATH =
            "https://personal.broadinstitute.org/picard/testdata/ex2.vcf.idx";
    private static final String FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final String CANTON_VCF = "classpath:templates/CantonS.vcf.gz";
    private String vcf;
    private String vcfGz;

    @Before
    public void setup() throws IOException {
        assertNotNull(context);
        assertNotNull(indexCache);
        vcf = context.getResource(FELIS_CATUS_VCF).getFile().getAbsolutePath();
        vcfGz = context.getResource(CANTON_VCF).getFile().getAbsolutePath();
    }

    /**
     * Asserts readability and correctness of VCF over HTTP.  The VCF is indexed and requires and index.
     */
    @Test
    public void testVcfOverHTTP() throws IOException {
        final VCFCodec codec = new VCFCodec();
        final AbstractFeatureReader<VariantContext, LineIterator> featureReaderHttp =
                AbstractFeatureReader.getFeatureReader(HTTP_INDEXED_VCF_PATH, HTTP_INDEXED_VCF_IDX_PATH,
                        codec, true, indexCache);

        String resource = context.getResource(LOCAL_MIRROR_HTTP_INDEXED_VCF_PATH).getFile().getAbsolutePath();
        final AbstractFeatureReader<VariantContext, LineIterator> featureReaderLocal =
                AbstractFeatureReader.getFeatureReader(resource, codec, false, indexCache);
        final CloseableTribbleIterator<VariantContext> localIterator = featureReaderLocal.iterator();
        for (final Feature feat : featureReaderHttp.iterator()) {
            assertEquals(feat.toString(), localIterator.next().toString());
        }
        assertFalse(localIterator.hasNext());

        Index index = IndexFactory.loadIndex(HTTP_INDEXED_VCF_IDX_PATH);
        final AbstractFeatureReader<VariantContext, LineIterator> featureReaderLocalIndex =
                AbstractFeatureReader.getFeatureReader(resource, codec, index, indexCache);
        assertNotNull(featureReaderLocalIndex);
    }

    @Test
    public void testVcfOverHTTPWithQuestionMarks() throws IOException {
        final VCFCodec codec = new VCFCodec();
        final AbstractFeatureReader<VariantContext, LineIterator> featureReaderHttp =
                AbstractFeatureReader.getFeatureReader(HTTP_INDEXED_VCF_PATH_WITH_QUESTIONS, HTTP_INDEXED_VCF_IDX_PATH,
                        codec, true, indexCache);
        assertNotNull(featureReaderHttp);
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

    @Test(expected = TribbleException.class)
    public void testLoadNonExistedBED() throws Exception {
        final String path = "testzip.bedx";
        final BEDCodec codec = new BEDCodec();
        final AbstractFeatureReader<BEDFeature, LineIterator> bfs =
                AbstractFeatureReader.getFeatureReader(path, codec, false, indexCache);
        assertNull(bfs);
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
        VCFCodec codec = new VCFCodec();
        try (TribbleIndexedFeatureReader<VariantContext, LineIterator> featureReader =
                     new TribbleIndexedFeatureReader<>(testPath, codec, false, indexCache)) {
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

    @Test(expected = TribbleException.class)
    public void testNotAsciiCodec() {
        final String vcf = "foo.vcf";
        final BCF2Codec codec = new BCF2Codec();
        final AbstractFeatureReader<VariantContext, PositionalBufferedStream> featureReader =
                AbstractFeatureReader.getFeatureReader(vcf, codec, false, indexCache);
        assertNull(featureReader);
    }

    @Test
    public void testAbstractFeatureConstructorWithoutIndex() {
        final VCFCodec codec = new VCFCodec();
        final AbstractFeatureReader featureReader = AbstractFeatureReader.getFeatureReader(vcfGz, codec, indexCache);
        assertNotNull(featureReader);
        assertTrue(featureReader.hasIndex());
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
        assertEquals(Tribble.tabixIndexFile(new File(vcf).getAbsolutePath()),
                new File(expectedIndex).getAbsolutePath());
    }

    @Test
    public void testTabixConstructors() throws IOException {
        TabixFeatureReader tabixFeatureReader = new TabixFeatureReader(vcfGz, new VCFCodec());
        assertNotNull(tabixFeatureReader);

        TabixReader tabixReader = new TabixReader(vcfGz, new SeekableFileStream(new File(vcfGz)), indexCache);
        assertNotNull(tabixReader);
        assertNotNull(tabixReader.getSource());
    }

    @Test
    public void testTribbleConstructors() throws IOException {
        TribbleIndexedFeatureReader tribbleFeatureReader = new TribbleIndexedFeatureReader(vcf, new VCFCodec(),
                true, indexCache);
        assertNotNull(tribbleFeatureReader);

        TribbleIndexedFeatureReader tribbleFeatureReaderNullIndex = new TribbleIndexedFeatureReader(vcf, null,
                        new VCFCodec(), true, indexCache);
        assertNotNull(tribbleFeatureReaderNullIndex);
        assertTrue(tribbleFeatureReaderNullIndex.hasIndex());
    }
}
