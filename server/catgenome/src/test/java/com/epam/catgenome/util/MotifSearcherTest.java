package com.epam.catgenome.util;

import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class MotifSearcherTest {

    @Test
    public void searchTest() {
        byte[] testSequence = "cgCGcattgcGcaaGGG".getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        Assert.assertEquals(3, MotifSearcher.search(testSequence, testRegex, "", 0, true).size());
    }

    @Test
    public void searchInPositiveStrandTest() {
        byte[] testSequence = "cgCGcattgcGcaaGGG".getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        Assert.assertEquals(2, MotifSearcher.search(testSequence, testRegex,
                StrandSerializable.POSITIVE, "", 0, true).size());
    }

    @Test
    public void searchInNegativeStrandTest() {
        byte[] testSequence = "cgCGcattgcGcaaGGG".getBytes(StandardCharsets.UTF_8);
        String testRegex = "ca";
        Assert.assertEquals(1, MotifSearcher.search(testSequence, testRegex,
                StrandSerializable.NEGATIVE, "", 0, true).size());
    }

    @Test(expected = IllegalStateException.class)
    public void searchThrowsExceptionOnInvalidSequence() {
        byte[] testSequence = "zxcontig".getBytes(StandardCharsets.UTF_8);
        String testRegex = "con";
        MotifSearcher.search(testSequence, testRegex, "", 0, true);
    }

    @Test
    public void convertIupacToRegexTest(){
        String testRegex = "atcgrYmKsWhBvDn[ac]+";
        String expectedResult = "atcg[rga][ytc][mac][kgt][sgc][wat][hact][bgtc][vgca][dgat].[ac]+";
        Assert.assertEquals(expectedResult, MotifSearcher.convertIupacToRegex(testRegex));
    }

    @Test
    public void searchInLargeBufferTest() throws IOException {

        final int expectedSize = 57534;
        final int bufferSize = 50_000_000;

        byte[] buf = new byte[bufferSize];
        final InputStream resourceAsStream = getClass().getResourceAsStream("/templates/dm606.X.fa");
        resourceAsStream.read(buf);
        resourceAsStream.close();
        String testSequence = new String(buf);
        final Pattern pattern = Pattern.compile("[^ATCGNatcgn]");
        testSequence = pattern.matcher(testSequence).replaceAll("n");
        buf = testSequence.getBytes(StandardCharsets.UTF_8);
        String testRegex = "tacyrw";
        Assert.assertEquals(expectedSize, MotifSearcher.search(buf, testRegex, "", 0, true).size());
    }

    @Test
    public void searchInLargeBufferInPositiveAndNegativeStrandTest() throws IOException {

        final int expectedSize = 57534;
        final int bufferSize = 50_000_000;

        byte[] buf = new byte[bufferSize];
        final InputStream resourceAsStream = getClass().getResourceAsStream("/templates/dm606.X.fa");
        resourceAsStream.read(buf);
        resourceAsStream.close();
        String testSequence = new String(buf);
        final Pattern pattern = Pattern.compile("[^ATCGNatcgn]");
        testSequence = pattern.matcher(testSequence).replaceAll("n");
        buf = testSequence.getBytes(StandardCharsets.UTF_8);
        String testRegex = "tacyrw";
        final int sumResult = MotifSearcher.search(buf, testRegex, StrandSerializable.POSITIVE, "", 0, true).size() +
                MotifSearcher.search(buf, testRegex, StrandSerializable.NEGATIVE, "", 0, true).size();
        Assert.assertEquals(expectedSize, sumResult);
    }
}
