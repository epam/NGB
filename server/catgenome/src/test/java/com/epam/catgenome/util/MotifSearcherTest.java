/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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

package com.epam.catgenome.util;

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.util.motif.IupacRegexConverter;
import com.epam.catgenome.util.motif.MotifSearchIterator;
import com.epam.catgenome.util.motif.MotifSearcher;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class MotifSearcherTest {

    public static final byte[] TEST_SEQUENCE = "cgCGcattgcGcaaGGG".getBytes(StandardCharsets.UTF_8);
    public static final String SIMPLE_TEST_MOTIF = "ca";
    public static final String SIMPLE_REVERSIBLE_TEST_REGEX = "(c[a])";
    public static final String TEST_REGEX = "ca+?";
    public static final String EXTENDED_TEST_REGEX = "tacyrw+?";
    public static final String TEST_REFERENCE_SOURCE = "/templates/dm606.X.fa";
    public static final int MAX_SIZE_SEARCH_RESULT_LIMIT = 2000000;
    public static final int SIZE_SEARCH_RESULT_LOW_LIMIT = 20000;

    @Test
    public void searchTest() {
        Assert.assertEquals(3,
                MotifSearcher.search(TEST_SEQUENCE, SIMPLE_TEST_MOTIF, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
        Assert.assertEquals(3,
                MotifSearcher.search(TEST_SEQUENCE, SIMPLE_REVERSIBLE_TEST_REGEX, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
        Assert.assertEquals(3,
                MotifSearcher.search(TEST_SEQUENCE, TEST_REGEX, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchPalindromeTest() {
        final String simplePalindromeMotif = "gc";
        final String simpleReversiblePalindromeRegex = "(g[c])";
        final String palindromeRegex = "gc+?";
        Assert.assertEquals(8,
                MotifSearcher.search(TEST_SEQUENCE, simplePalindromeMotif, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
        Assert.assertEquals(8,
                MotifSearcher.search(TEST_SEQUENCE, simpleReversiblePalindromeRegex, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
        Assert.assertEquals(8,
                MotifSearcher.search(TEST_SEQUENCE, palindromeRegex, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchStartAndEndEdgesWhenGivenSimpleTestMotif() {
        final int[] actualResults = MotifSearcher.search(TEST_SEQUENCE, SIMPLE_TEST_MOTIF, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT)
                .flatMapToInt(motif -> IntStream.of(motif.getStart(), motif.getEnd()))
                .toArray();
        final int[] expectedResults = {4, 5, 7, 8, 11, 12};
        Assert.assertArrayEquals(expectedResults, actualResults);
    }

    @Test
    public void searchStartAndEndEdgesWhenGivenSimpleReversibleTestRegex() {
        final int[] actualResults = MotifSearcher.search(TEST_SEQUENCE, SIMPLE_REVERSIBLE_TEST_REGEX, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT)
                .flatMapToInt(motif -> IntStream.of(motif.getStart(), motif.getEnd()))
                .toArray();
        final int[] expectedResults = {4, 5, 7, 8, 11, 12};
        Assert.assertArrayEquals(expectedResults, actualResults);
    }

    @Test
    public void searchStartAndEndEdgesWhenGivenTestRegex() {
        final String testRegex = "ca+?";
        final int[] actualResults = MotifSearcher.search(TEST_SEQUENCE, testRegex, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT)
                .flatMapToInt(motif -> IntStream.of(motif.getStart(), motif.getEnd()))
                .toArray();
        final int[] expectedResults = {4, 5, 7, 8, 11, 12};
        Assert.assertArrayEquals(expectedResults, actualResults);
    }

    @Test
    public void comparingSearchedSequenceWhenGivenSimpleTestMotif() {
        final String[] actualResults = MotifSearcher.search(TEST_SEQUENCE, SIMPLE_TEST_MOTIF, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT)
                .map(Motif::getSequence)
                .toArray(String[]::new);
        final String[] expectedResults = {"ca", "tg", "ca"};
        Assert.assertArrayEquals(expectedResults, actualResults);

    }

    @Test
    public void comparingSearchedSequenceWhenGivenSimpleReversibleTestRegex() {
        final String[] actualResults = MotifSearcher.search(TEST_SEQUENCE, SIMPLE_REVERSIBLE_TEST_REGEX, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT)
                .map(Motif::getSequence)
                .toArray(String[]::new);
        final String[] expectedResults = {"ca", "tg", "ca"};
        Assert.assertArrayEquals(expectedResults, actualResults);
    }

    @Test
    public void comparingSearchedSequenceWhenGivenTestRegex() {
        final String[] actualResults = MotifSearcher.search(TEST_SEQUENCE, TEST_REGEX, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT)
                .map(Motif::getSequence)
                .toArray(String[]::new);
        final String[] expectedResults = {"ca", "tg", "ca"};
        Assert.assertArrayEquals(expectedResults, actualResults);
    }

    @Test
    public void searchInPositiveStrandWhenGivenSimpleTestMotif() {
        Assert.assertEquals(2, MotifSearcher.search(TEST_SEQUENCE, SIMPLE_TEST_MOTIF,
                StrandSerializable.POSITIVE, "", 0,
                true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInPositiveStrandWhenGivenSimpleReversibleTestRegex() {
        Assert.assertEquals(2, MotifSearcher.search(TEST_SEQUENCE, SIMPLE_REVERSIBLE_TEST_REGEX,
                StrandSerializable.POSITIVE, "", 0,
                true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInPositiveStrandWhenGivenTestRegex() {
        Assert.assertEquals(2,
                MotifSearcher.search(TEST_SEQUENCE, TEST_REGEX, StrandSerializable.POSITIVE, "", 0,
                true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInNegativeStrandWhenGivenSimpleTestMotif() {
        Assert.assertEquals(1,
                MotifSearcher.search(TEST_SEQUENCE, SIMPLE_TEST_MOTIF, StrandSerializable.NEGATIVE, "", 0,
                true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInNegativeStrandWhenGivenSimpleReversibleTestRegex() {
        Assert.assertEquals(1,
                MotifSearcher.search(TEST_SEQUENCE, SIMPLE_REVERSIBLE_TEST_REGEX, StrandSerializable.NEGATIVE, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInNegativeStrandWhenGivenTestRegex() {
        Assert.assertEquals(1, MotifSearcher.search(TEST_SEQUENCE, TEST_REGEX, StrandSerializable.NEGATIVE, "", 0,
                true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test(expected = IllegalStateException.class)
    public void searchThrowsExceptionOnInvalidSequenceWhenGivenTestRegex() {
        byte[] testSequence = "zxcontig".getBytes(StandardCharsets.UTF_8);
        String testRegex = "con+?";
        MotifSearcher.search(testSequence, testRegex, "", 0, true, MAX_SIZE_SEARCH_RESULT_LIMIT);
    }

    @Test
    public void convertIupacToRegexTest(){
        String testRegex = "atcgrYmKsWhBvDn[ac]+";
        String expectedResult = "atcg[rga][ytc][mac][kgt][sgc][wat][hact][bgtc][vgca][dgat].[ac]+";
        Assert.assertEquals(expectedResult, IupacRegexConverter.convertIupacToRegex(testRegex));
    }

    @Test
    public void convertIupactoComplementReversedRegexTest(){
        String testRegex1 = "a[^a[tcg]rY]mK{23}sn[ac]+";
        String expectedResult1 = "[gt]+.[cgs][acm]{23}[gtk][^[gar][tcy][cga]t]t";
        Assert.assertEquals(expectedResult1, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex1));

        String testRegex2 = "a+?c[gt]*?yr{2,}a{7,}";
        String expectedResult2 = "t{7,}[tcy]{2,}[gar][ac]*?gt+?";
        Assert.assertEquals(expectedResult2, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex2));

        String testRegex3 = "(ac)|(tc)|[tcc]{12}";
        String expectedResult3 = "[gga]{12}|(ga)|(gt)";
        Assert.assertEquals(expectedResult3, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex3));

        String testRegex4 = "((ac)|(tc))|([tcc]{12,23})";
        String expectedResult4 = "([gga]{12,23})|((ga)|(gt))";
        Assert.assertEquals(expectedResult4, IupacRegexConverter.convertIupacToComplementReversedRegex(testRegex4));
    }

    @Test
    public void searchInLargeBufferWhenGivenSimpleTestMotif() throws IOException {
        final int expectedSize = 57534;
        final byte[] largeTestSequence = getTestSequenceFromResource(TEST_REFERENCE_SOURCE);
        final String[] testMotifsAsRegexSubstitution =
            {"taccat", "taccaa", "taccgt", "taccga", "tactat", "tactaa", "tactgt", "tactga"};
        final int actualSize = IntStream.range(0,  testMotifsAsRegexSubstitution.length)
                .map(i -> (int) MotifSearcher.search(largeTestSequence, testMotifsAsRegexSubstitution[i], "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count())
                .sum();
        Assert.assertEquals(expectedSize, actualSize);
    }

    @Test
    public void searchInLargeBufferWhenGivenSimpleReversibleTestRegex() throws IOException {
        final int expectedSize = 57534;
        final byte[] largeTestSequence = getTestSequenceFromResource(TEST_REFERENCE_SOURCE);
        final String simpleReversibleTestRegex = "(tacyrw)";
        Assert.assertEquals(expectedSize,
                MotifSearcher.search(largeTestSequence, simpleReversibleTestRegex, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInLargeBufferWhenGivenTestRegex() throws IOException {
        final int expectedSize = 57534;
        final byte[] largeTestSequence = getTestSequenceFromResource(TEST_REFERENCE_SOURCE);
        final String testRegex = "tacyrw+?";
        Assert.assertEquals(expectedSize, MotifSearcher.search(largeTestSequence, testRegex, "", 0,
                true, MAX_SIZE_SEARCH_RESULT_LIMIT).count());
    }

    @Test
    public void searchInLargeBufferInPositiveAndNegativeStrandWhenGivenSimpleTestMotif() throws IOException {
        final int expectedSize = 57534;
        final byte[] largeTestSequence = getTestSequenceFromResource(TEST_REFERENCE_SOURCE);
        final String[] testMotifsAsRegexSubstitution =
            {"taccat", "taccaa", "taccgt", "taccga", "tactat", "tactaa", "tactgt", "tactga"};
        final int actualSize = Arrays.stream(testMotifsAsRegexSubstitution)
                .mapToInt(s ->
                        (int) MotifSearcher.search(largeTestSequence, s,
                        StrandSerializable.POSITIVE, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count()
                        + (int) MotifSearcher.search(largeTestSequence, s,
                        StrandSerializable.NEGATIVE, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count())
                .sum();
        Assert.assertEquals(expectedSize, actualSize);
    }

    @Test
    public void searchInLargeBufferInPositiveAndNegativeStrandWhenGivenTestRegex() throws IOException {
        final int expectedSize = 57534;
        final byte[] largeTestSequence = getTestSequenceFromResource(TEST_REFERENCE_SOURCE);
        final String testRegex = "tacyrw+?";
        final long sumResult =
                MotifSearcher.search(largeTestSequence, testRegex, StrandSerializable.POSITIVE, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count() +
                MotifSearcher.search(largeTestSequence, testRegex, StrandSerializable.NEGATIVE, "", 0,
                        true, MAX_SIZE_SEARCH_RESULT_LIMIT).count();
        Assert.assertEquals(expectedSize, sumResult);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchInLargeBufferOnPositiveStrandWhenGivenShortestTestRegexAndSetModestResultSizeLimitShouldFail()
            throws IOException {
        new MotifSearchIterator(getTestSequenceFromResource(TEST_REFERENCE_SOURCE), EXTENDED_TEST_REGEX,
                StrandSerializable.NEGATIVE, "", 0, true, SIZE_SEARCH_RESULT_LOW_LIMIT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void searchInLargeBufferOnNegativeStrandWhenGivenShortestTestRegexAndSetModestResultSizeLimitShouldFail()
            throws IOException {
        new MotifSearchIterator(getTestSequenceFromResource(TEST_REFERENCE_SOURCE), EXTENDED_TEST_REGEX,
               StrandSerializable.NEGATIVE, "", 0, true, SIZE_SEARCH_RESULT_LOW_LIMIT);
    }

    private byte[] getTestSequenceFromResource(final String path) throws IOException {
        final InputStream resourceAsStream = getClass().getResourceAsStream(path);
        byte[] buf = new byte[resourceAsStream.available()];
        resourceAsStream.read(buf);
        resourceAsStream.close();
        return Pattern.compile("[^ATCGNatcgn]").matcher(new String(buf))
                .replaceAll("n").getBytes(StandardCharsets.UTF_8);
    }
}
