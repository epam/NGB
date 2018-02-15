package com.epam.catgenome.util;

import com.epam.catgenome.common.AbstractManagerTest;
import com.epam.catgenome.util.feature.reader.*;
import htsjdk.tribble.*;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;

/**
 * Test features of IndexCacheHeaderTest: check if get indexes and headers from cache.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class IndexHeaderCacheTest<T extends Feature, S> extends AbstractManagerTest {

    @Autowired
    private ApplicationContext context;

    @Spy
    @Autowired
    private EhCacheBasedIndexCache indexCache;

    private static final String FELIS_CATUS_VCF = "classpath:templates/Felis_catus.vcf";
    private static final String FELIS_CATUS_IDX = "classpath:templates/Felis_catus.idx";
    private static final String CANTON_VCF = "classpath:templates/CantonS.vcf.gz";
    private static final String CANTON_VCF_TBI = "classpath:templates/CantonS.vcf.gz.tbi";

    private String vcf;
    private String idx;
    private String vcfGz;
    private String tbiGz;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        assertNotNull(context);
        assertNotNull(indexCache);
        vcf = context.getResource(FELIS_CATUS_VCF).getFile().getAbsolutePath();
        idx = context.getResource(FELIS_CATUS_IDX).getFile().getAbsolutePath();
        vcfGz = context.getResource(CANTON_VCF).getFile().getAbsolutePath();
        tbiGz = context.getResource(CANTON_VCF_TBI).getFile().getAbsolutePath();

        indexCache.clearCache();
    }

    @Test
    public void  testReadTribbleIndexFromCache() throws IOException {
        // first creating, try to read index after header
        FeatureReader<VariantContext> reader = AbstractEnhancedFeatureReader.getFeatureReader(vcf,
                idx, new VCFCodec(), true, indexCache);
        assertNotNull(reader);
        verify(indexCache, times(1)).getFromCache(Tribble.indexFile(vcf));

        //read index and header from cache second time
        reader = AbstractEnhancedFeatureReader.getFeatureReader(vcf,
                idx, new VCFCodec(), true, indexCache);
        assertNotNull(reader);
        verify(indexCache, times(3)).getFromCache(Tribble.indexFile(vcf));
    }

    @Test
    public void testReadTabixIndexHeaderFromCache() throws IOException {
        //first creating, try to read header after index
        FeatureReader<VariantContext> reader = AbstractEnhancedFeatureReader.getFeatureReader(vcfGz,
                tbiGz, new VCFCodec(), true, indexCache);
        assertNotNull(reader);
        verify(indexCache, times(1)).getFromCache(tbiGz);

        reader = AbstractEnhancedFeatureReader.getFeatureReader(vcfGz,
                tbiGz, new VCFCodec(), true, indexCache);
        assertNotNull(reader);
        //read both index and header from cache
        verify(indexCache, times(3)).getFromCache(tbiGz);
    }
}
