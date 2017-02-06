package com.epam.catgenome.util;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class InfoFieldParserTest {

    private static final String PATTERN = "'|'";
    private static final String PATTERN2 = "{/}";
    private static final String EXTEDENDED_INFO_HEADER =
            "'Gene_Name | Gene_ID | Number_of_transcripts_in_gene | Percent_of_transcripts_affected'";
    private static final String EXTENDED_INFO_VALUE = "KRAS | 123||";
    private static final String EXTENDED_INFO_WRONG_DELIMITER = "KRAS / 123//";
    private static final String EXTENDED_INFO_VALUE_BRACKETS = "(KRAS | 123||)";
    private static final String NON_EXTENDED_VALUE = "Gene_Name |'";

    @Test
    public void testIsExtendedInfoField() throws Exception {
        InfoFieldParser parser = new InfoFieldParser(PATTERN);
        Assert.assertTrue(parser.isExtendedInfoField(EXTEDENDED_INFO_HEADER));
        Assert.assertFalse(parser.isExtendedInfoField(NON_EXTENDED_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPattern() throws Exception {
        new InfoFieldParser(PATTERN.substring(1));
        System.err.println("");
    }


    @Test
    public void testExtractDataFromLineTwoPatterns() throws Exception {
        InfoFieldParser parser = new InfoFieldParser(PATTERN2 + "," + PATTERN);
        List<String> result = parser.extractDataFromLine(EXTENDED_INFO_VALUE);
        Assert.assertEquals(4, result.size());
    }

    @Test
    public void testExtractDataFromLine() throws Exception {
        InfoFieldParser parser = new InfoFieldParser(PATTERN);
        List<String> result = parser.extractDataFromLine(EXTENDED_INFO_VALUE);
        Assert.assertEquals(4, result.size());
    }

    @Test
    public void testExtractDataFromLineWrongDelimiter() throws Exception {
        InfoFieldParser parser = new InfoFieldParser(PATTERN);
        List<String> result = parser.extractDataFromLine(EXTENDED_INFO_WRONG_DELIMITER);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testExtractDataFromLineWithBrackets() throws Exception {
        InfoFieldParser parser = new InfoFieldParser(PATTERN);
        List<String> resultRemoveBrackets = parser.extractDataFromLine(EXTENDED_INFO_VALUE_BRACKETS);
        Assert.assertEquals(4, resultRemoveBrackets.size());
        Assert.assertTrue(resultRemoveBrackets.get(0).startsWith("K"));
        Assert.assertTrue(resultRemoveBrackets.get(3).isEmpty());

        List<String> resultNoBrackets = parser.extractDataFromLine(EXTENDED_INFO_VALUE_BRACKETS.substring(0,
                EXTENDED_INFO_VALUE_BRACKETS.length() - 1));
        Assert.assertEquals(4, resultNoBrackets.size());
        Assert.assertTrue(resultNoBrackets.get(0).startsWith("("));
        Assert.assertTrue(resultNoBrackets.get(3).isEmpty());
    }

    @Test
    public void testExtractHeaderFromLine() throws Exception {
        InfoFieldParser parser = new InfoFieldParser(PATTERN);
        List<String> result = parser.extractHeaderFromLine(EXTEDENDED_INFO_HEADER);
        Assert.assertEquals(4, result.size());
    }
}
