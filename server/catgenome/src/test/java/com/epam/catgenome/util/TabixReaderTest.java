package com.epam.catgenome.util;

import com.epam.catgenome.util.feature.reader.TabixIteratorLineReader;
import com.epam.catgenome.util.feature.reader.TabixReader;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Copied from HTSJDK library.
 */
public class TabixReaderTest {

    private static final String TABIX_FILE_PATH = "templates/trioDup.vcf.gz";
    private String tabixFile;
    private TabixReader tabixReader;
    private List<String> sequenceNames;

    @Before
    public void setup() throws IOException {
        tabixFile = getClass().getClassLoader().getResource(TABIX_FILE_PATH).getFile();
        tabixReader = new TabixReader(tabixFile);
        sequenceNames = new ArrayList<>(tabixReader.getChromosomes());
    }

    @After
    public void teardown() throws Exception {
        tabixReader.close();
    }

    @Test
    public void testSequenceNames() {
        String[] expectedSeqNames = new String[24];
        for (int i = 1; i < 24; i++) {
            expectedSeqNames[i - 1] = String.valueOf(i);
        }
        expectedSeqNames[22] = "X";
        expectedSeqNames[23] = "Y";
        assertEquals(expectedSeqNames.length, sequenceNames.size());

        for (String s : expectedSeqNames) {
            assertTrue(sequenceNames.contains(s));
        }
    }

    @Test
    public void testSequenceSet() {
        Set<String> chroms = tabixReader.getChromosomes();
        assertFalse(chroms.isEmpty());
        assertTrue(chroms.contains("1"));
        assertFalse(chroms.contains("MT"));
    }

    @Test
    public void testIterators() throws IOException {
        TabixReader.Iterator iter = tabixReader.query("1", 1, 400);
        assertNotNull(iter);
        assertNotNull(iter.next());
        assertNull(iter.next());

        iter = tabixReader.query("UN", 1, 100);
        assertNotNull(iter);
        assertNull(iter.next());

        iter = tabixReader.query("UN:1-100");
        assertNotNull(iter);
        assertNull(iter.next());


        iter = tabixReader.query("1:10-1");
        assertNotNull(iter);
        assertNull(iter.next());

        iter = tabixReader.query(999999, 9, 9);
        assertNotNull(iter);
        assertNull(iter.next());

        iter = tabixReader.query("1", Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertNotNull(iter);
        assertNull(iter.next());

        final int pos_snp_in_vcf_chr1 = 327;

        iter = tabixReader.query("1", pos_snp_in_vcf_chr1, pos_snp_in_vcf_chr1);
        assertNotNull(iter);
        assertNotNull(iter);
        assertNull(iter.next());

        iter = tabixReader.query("1", pos_snp_in_vcf_chr1 - 1, pos_snp_in_vcf_chr1 - 1);
        assertNotNull(iter);
        assertNull(iter.next());

        iter = tabixReader.query("1", pos_snp_in_vcf_chr1 + 1, pos_snp_in_vcf_chr1 + 1);
        assertNotNull(iter);
        assertNull(iter.next());
    }

    /**
     * Test reading a local tabix file
     *
     * @throws java.io.IOException
     */
    @Test
    public void testLocalQuery() throws IOException {
        TabixIteratorLineReader lineReader = new TabixIteratorLineReader(
                tabixReader.query(tabixReader.chr2tid("4"), 320, 330));

        int nRecords = 0;
        String nextLine;
        while ((nextLine = lineReader.readLine()) != null) {
            assertTrue(nextLine.startsWith("4"));
            nRecords++;
        }
        assertTrue(nRecords > 0);
    }

    /**
     * Test reading a tabix file over http
     *
     * @throws java.io.IOException
     */
    @Test
    public void testRemoteQuery() throws IOException {
        String tabixFile = "https://personal.broadinstitute.org/picard/testdata/igvdata/tabix/trioDup.vcf.gz";

        TabixReader tabixReader = new TabixReader(tabixFile);
        TabixIteratorLineReader lineReader = new TabixIteratorLineReader(
                tabixReader.query(tabixReader.chr2tid("4"), 320, 330));

        int nRecords = 0;
        String nextLine;
        while ((nextLine = lineReader.readLine()) != null) {
            assertTrue(nextLine.startsWith("4"));
            nRecords++;
        }
        assertTrue(nRecords > 0);
    }

    /**
     * Test TabixReader.readLine
     *
     * @throws java.io.IOException
     */
    @Test
    public void testTabixReaderReadLine() throws IOException {
        TabixReader tabixReader = new TabixReader(tabixFile);
        assertNotNull(tabixReader.readLine());
        tabixReader.close();
    }
}