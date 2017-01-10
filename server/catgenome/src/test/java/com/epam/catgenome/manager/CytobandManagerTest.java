/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager;

import static com.epam.catgenome.helper.FileTemplates.CAT_CYTOBANDS_GZ;
import static com.epam.catgenome.helper.FileTemplates.HP_CYTOBANDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.epam.catgenome.common.AbstractJUnitTest;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.cytoband.Cytoband;
import com.epam.catgenome.entity.reference.cytoband.GiemsaStain;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.reference.CytobandManager;
import com.epam.catgenome.manager.reference.io.cytoband.CytobandReader;
import com.epam.catgenome.manager.reference.io.cytoband.CytobandRecord;

/**
 * Source:      CytobandManagerTest.java
 * Created:     10/19/15, 12:24 PM
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 * <p>
 * {@code CytobandManagerTest} is used to test different calls to the service, which
 * is responsible for cytobands' management
 *
 * @author Denis Medvedev
 * @author Mikhail Miroliubov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class CytobandManagerTest extends AbstractJUnitTest {

    // describes attributes of the first band from HUMAN_CYTOBANDS_GZ test resource
    private static final int CAT_A1_BANDS_NUMBER = 4;
    private static final int CAT_BAND_START_INDEX = 0;
    private static final int CAT_BAND_END_INDEX = 85059546;
    private static final String CAT_BAND_NAME = "p";
    private static final String CAT_CHROMOSOME_A1 = "chrA1";
    private static final GiemsaStain CAT_BAND_GSTAIN = GiemsaStain.GNEG;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private CytobandManager cytobandManager;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testSaveAndLoadCytobands() throws Exception {
        // creates a corresponded test genome 'classpath:templates/reference/hp.genome.fa'
        final Reference reference = createGenome();
        // adds cytological bands to corresponded chromosomes
        cytobandManager.saveCytobands(reference.getId(), getTemplate(HP_CYTOBANDS.getPath()));
        // checks that cytobands were saved under appropriate location
        reference.getChromosomes().stream().forEach(
            chromosome -> {
                if (!CHR_A5.equals(chromosome.getName())) {
                    final File cytobandFile = fileManager.makeCytobandsFile(chromosome);
                    assertTrue(String.format("No cytobands are found for %s chromosome.", chromosome.getName()),
                        cytobandFile.exists());
                }
            }
        );
        // tries to load a track with cytobands for the first chromosome
        final Chromosome chromosome = reference.getChromosomes().get(0);
        final Track<Cytoband> track = cytobandManager.loadCytobands(chromosome.getId());
        assertNotNull(String.format("No cytobands are found for %s chromosome.", chromosome.getName()), track);
    }

    @Test
    public void testGZIPCatCytobandParsing() throws IOException {
        // parses cat cytobands, skipping validation
        final CytobandReader reader = CytobandReader.getInstance(getTemplate(CAT_CYTOBANDS_GZ.getPath()));
        // tests the first cytoband record
        final CytobandRecord record = reader.getRecord(CAT_CHROMOSOME_A1);
        assertNotNull(String.format("No cytobands are found for %s chromosome.", CAT_CHROMOSOME_A1), record);
        assertTrue(String.format("Unexpected number of bands for %s chromosome.", CAT_CHROMOSOME_A1),
            CAT_A1_BANDS_NUMBER == record.getBands().size());
        final Cytoband band = record.getBands().get(0);
        assertEquals("Unexpected band name.", CAT_BAND_NAME, band.getName());
        assertEquals("Unexpected Giemsa stain.", CAT_BAND_GSTAIN, band.getGiemsaStain());
        assertTrue("Unexpected end index value.", CAT_BAND_END_INDEX == band.getEndIndex());
        assertEquals("Unexpected chromosome name.", CAT_CHROMOSOME_A1, band.getChromosome());
        assertTrue("Unexpected start index value.", CAT_BAND_START_INDEX == band.getStartIndex());
    }

}
