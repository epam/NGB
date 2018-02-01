package com.epam.catgenome.manager.gene;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.catgenome.util.feature.reader.AbstractEnhancedFeatureReader;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFileType;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.helper.EntityHelper;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.gene.parser.GeneFeature;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.manager.reference.ReferenceGenomeManager;
import com.epam.catgenome.util.CachedFeatureReader;
import htsjdk.samtools.util.Locatable;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import htsjdk.tribble.readers.LineIterator;

/**
 * Source:      GffManagerUnitTest
 * Created:     25.01.17, 13:12
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class GffManagerUnitTest {
    private static final long TEST_GENE_FILE_ID = 1;
    private static final long TEST_CHROMOSOME_ID = 1;
    //private static final int TEST_END_INDEX = 239107476;
    private static final int TEST_CHROMOSOME_SIZE = 239107476;

    @InjectMocks
    @Spy
    private GffManager gffManager;

    @Mock
    private GeneFileManager geneFileManager;

    @Mock
    private ReferenceGenomeManager referenceGenomeManager;

    @Mock
    private FileManager fileManager;

    @Autowired
    private ApplicationContext context;

    //@Autowired
    private EhCacheBasedIndexCache indexCache;

    private List<GeneFeature> featureList;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        Resource resource = context.getResource("classpath:templates/genes_sorted.gtf");
        try (AbstractFeatureReader<GeneFeature, LineIterator> reader = AbstractEnhancedFeatureReader
                .getFeatureReader(
            resource.getFile().getAbsolutePath(), new GffCodec(
            GffCodec.GffType.GTF), false, indexCache)) {
            featureList = reader.iterator().toList();
        }
    }

    @Test
    public void testGetNextFeature() throws IOException {
        GeneFile testGeneFile = new GeneFile();
        testGeneFile.setId(TEST_GENE_FILE_ID);
        testGeneFile.setCompressed(false);

        Chromosome testChromosome = EntityHelper.createNewChromosome();
        testChromosome.setSize(TEST_CHROMOSOME_SIZE);
        testChromosome.setId(TEST_CHROMOSOME_ID);

        CachedFeatureReader<GeneFeature, LineIterator> reader = new CachedFeatureReader<>(featureList, new GffCodec(
            GffCodec.GffType.GTF));

        Mockito.when(geneFileManager.loadGeneFile(TEST_GENE_FILE_ID)).thenReturn(testGeneFile);
        Mockito.when(fileManager.makeGeneReader(testGeneFile, GeneFileType.ORIGINAL)).thenReturn(reader);
        Mockito.when(referenceGenomeManager.loadChromosome(TEST_CHROMOSOME_ID)).thenReturn(testChromosome);

        List<GeneFeature> exons = featureList.stream().filter(s -> "exon".equals(s.getFeature())).collect(
            Collectors.toList());

        Collections.sort(exons, Comparator.comparingInt(Locatable::getStart));
        int middle = exons.size() / 2;
        GeneFeature firstExon = exons.get(middle);
        GeneFeature secondExon = exons.get(middle + 1);

        Gene loadedNextExon = gffManager.getNextOrPreviousFeature(firstExon.getEnd(), TEST_GENE_FILE_ID,
                                                                  TEST_CHROMOSOME_ID, true);
        Assert.assertNotNull(loadedNextExon);
        Assert.assertEquals(secondExon.getStart(), loadedNextExon.getStartIndex().intValue());
        Assert.assertEquals(secondExon.getEnd(), loadedNextExon.getEndIndex().intValue());

        reader.reOpen();

        Gene loadPrevExon = gffManager.getNextOrPreviousFeature(secondExon.getStart(), TEST_GENE_FILE_ID,
                                                                TEST_CHROMOSOME_ID, false);

        Assert.assertNotNull(loadPrevExon);
        Assert.assertEquals(firstExon.getStart(), loadPrevExon.getStartIndex().intValue());
        Assert.assertEquals(firstExon.getEnd(), loadPrevExon.getEndIndex().intValue());
    }
}
