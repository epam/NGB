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

package com.epam.catgenome.manager.tools;

import com.epam.catgenome.common.AbstractJUnitTest;
import com.epam.catgenome.controller.tools.FeatureFileSortRequest;
import com.epam.catgenome.manager.gene.parser.GffCodec;
import com.epam.catgenome.util.NgbFileUtils;
import com.epam.catgenome.util.feature.reader.AbstractEnhancedFeatureReader;
import com.epam.catgenome.util.feature.reader.AbstractFeatureReader;
import com.epam.catgenome.util.feature.reader.EhCacheBasedIndexCache;
import htsjdk.tribble.CloseableTribbleIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodec;
import htsjdk.tribble.bed.BEDCodec;
import htsjdk.variant.vcf.VCFCodec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.epam.catgenome.util.IndexUtils.checkSorted;
import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class SortTest extends AbstractJUnitTest {

    private static final int MAX_MEMORY = 500;

    private static final int UNSORTED_BED_EXPECTED_LINES = 9;
    private static final int GENE_SORTED_BED_EXPECTED_LINES = 141;
    private static final int BIG_BED_EXPECTED_LINES = 8178;

    @Autowired
    private ToolsManager toolsManager;

    @Autowired(required = false)
    private EhCacheBasedIndexCache indexCache;

    @Test
    public void testSortBed() throws Exception {
        testSort(getTemplate("invalid/unsorted.bed"), new BEDCodec(), UNSORTED_BED_EXPECTED_LINES);
    }

    @Test
    public void testSortBedCompressed() throws Exception {
        testSort(getTemplate("genes_sorted.bed.gz"), new BEDCodec(), GENE_SORTED_BED_EXPECTED_LINES);
    }

    @Test
    public void testSortBedCompressedOutOfMemory() throws Exception {
        testSort(getTemplate("big.bed.gz"), new BEDCodec(), BIG_BED_EXPECTED_LINES, 1);
    }

    @Test
    public void testSortVCF() throws Exception {
        testSort(getTemplate("invalid/unsorted.vcf"), new VCFCodec());
    }

    @Test
    public void testSortVCFCompressed() throws Exception {
        testSort(getTemplate("Felis_catus.vcf.gz"), new VCFCodec());
    }

    @Test
    public void testSortGFF() throws Exception {
        testSort(getTemplate("invalid/unsorted.gff"), new GffCodec(GffCodec.GffType.GFF));
    }

    @Test
    public void testSortGFF2() throws Exception {
        testSort(getTemplate("genes.gff"), new GffCodec(GffCodec.GffType.GTF));
    }

    @Test
    public void testSortGTFCompressed() throws Exception {
        testSort(getTemplate("genes_sorted.gtf.gz"), new GffCodec(GffCodec.GffType.COMPRESSED_GTF));
    }

    public void testSort(File infile, final FeatureCodec codec) throws IOException {
        testSort(infile, codec, 0);
    }

    public void testSort(File infile, final FeatureCodec codec, final int expectedLines) throws IOException {
        testSort(infile, codec, expectedLines, MAX_MEMORY);
    }

    public void testSort(File infile, final FeatureCodec codec, final int expectedLines, final int maxMemory)
            throws IOException {

        File ofile = new File(
                infile + (NgbFileUtils.isGzCompressed(infile.getName()) ? ".sorted.gz" : ".sorted")
        );
        ofile.deleteOnExit();

        FeatureFileSortRequest request = new FeatureFileSortRequest();
        request.setOriginalFilePath(infile.getAbsolutePath());
        request.setSortedFilePath(ofile.getAbsolutePath());
        request.setMaxMemory(maxMemory);

        toolsManager.sortFeatureFile(request);

        int outLines = checkFileSorted(ofile, codec);

        if(expectedLines != 0){
            assertEquals(expectedLines, outLines);
        }
    }

    public <F extends Feature, S> int checkFileSorted(File ofile, final FeatureCodec<F, S> codec)
            throws IOException {
        int numlines = 0;

        AbstractFeatureReader<F, S> reader =
                AbstractEnhancedFeatureReader.getFeatureReader(ofile.getAbsolutePath(), codec, false, indexCache);
        CloseableTribbleIterator<F> iterator = reader.iterator();

        final Map<String, Feature> visitedChromos = new HashMap<>(40);
        Feature lastFeature = null;
        while (iterator.hasNext()) {
            Feature currentFeature = iterator.next();
            numlines++;

            checkSorted(ofile, lastFeature, currentFeature, visitedChromos);

            lastFeature = currentFeature;
        }

        return numlines;
    }

}
