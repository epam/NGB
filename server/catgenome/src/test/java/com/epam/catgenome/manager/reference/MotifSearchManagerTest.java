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

package com.epam.catgenome.manager.reference;

import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import org.junit.After;
import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class MotifSearchManagerTest {

    public static final int START_POSITION = 50;
    public static final int END_POSITION = 1000;
    public static final int PAGE_SIZE = 100;
    public static final int BIG_PAGE_SIZE = 10000;

    @Autowired
    ApplicationContext context;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private MotifSearchManager motifSearchManager;

    private static final String TEST_REF_NAME = "//dm606.X.fa";
    private static final String CHROMOSOME_NAME = "X";

    private static final String A3_FA_NAME = "//A3.fa";
    private static final String HP_GENOME_NAME = "//reference/hp.genome.fa";
    private static final String TEST_WG_NAME = "//Test_wg.fa";
    private static final String TEST_GENOME_MOTIF = "AAC";
    private static final String EXTENDED_TEST_GENOME_MOTIF = "A+C";

    private Reference a3TestReference;
    private Reference hpGenomeTestReference;
    private Reference testWgTestReference;

    private Reference testReference;
    private Chromosome testChromosome;
    private int motifSearchManagerBufferSize;

    @Before
    public void setup() throws IOException {
        motifSearchManagerBufferSize = (int)ReflectionTestUtils.getField(motifSearchManager, "bufferSize");
        Resource resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_REF_NAME);

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setPath(fastaFile.getPath());
        testReference = referenceManager.registerGenome(request);
        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(CHROMOSOME_NAME)) {
                testChromosome = chromosome;
                break;
            }
        }

        fastaFile = new File(resource.getFile().getAbsolutePath() + A3_FA_NAME);
        request = new ReferenceRegistrationRequest();
        request.setPath(fastaFile.getPath());
        a3TestReference = referenceManager.registerGenome(request);
        fastaFile = new File(resource.getFile().getAbsolutePath() + HP_GENOME_NAME);
        request = new ReferenceRegistrationRequest();
        request.setPath(fastaFile.getPath());
        hpGenomeTestReference = referenceManager.registerGenome(request);
        fastaFile = new File(resource.getFile().getAbsolutePath() + TEST_WG_NAME);
        request = new ReferenceRegistrationRequest();
        request.setPath(fastaFile.getPath());
        testWgTestReference = referenceManager.registerGenome(request);
    }

    @After
    public void restoreMotifSearchManagerBufferSize() {
        ReflectionTestUtils.setField(motifSearchManager, "bufferSize", motifSearchManagerBufferSize);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifsBounds() throws IOException {
        final int expectedStartPosition = 10000;
        final int expectedEndPosition = 11000;
        final int shift = 1000;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .endPosition(expectedEndPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedStartPosition, search.getResult().get(0).getStart());
        Assert.assertEquals(expectedEndPosition, search.getResult().get(0).getEnd());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifDividedWithBeginningBound() throws IOException {
        final int expectedStartPosition = 10000;
        final int expectedEndPosition = 10099;
        final int hidingShift = 100;
        final int nonHidingShift = 99;
        final int shift = 3000;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition + nonHidingShift)
                .endPosition(expectedEndPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedStartPosition, search.getResult().get(0).getStart());
        Assert.assertEquals(expectedEndPosition, search.getResult().get(0).getEnd());

        final MotifSearchRequest testUnsuccessfulRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition + hidingShift)
                .endPosition(expectedEndPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult unsuccessfulSearch = motifSearchManager.search(testUnsuccessfulRequest);
        Assert.assertEquals(0, unsuccessfulSearch.getResult().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifDividedWithEndBound() throws IOException {
        final int expectedStartPosition = 10000;
        final int expectedEndPosition = 10099;
        final int hidingShift = 100;
        final int nonHidingShift = 99;
        final int shift = 3000;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .endPosition(expectedEndPosition - nonHidingShift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedStartPosition, search.getResult().get(0).getStart());
        Assert.assertEquals(expectedEndPosition, search.getResult().get(0).getEnd());

        final MotifSearchRequest testUnsuccessfulRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .endPosition(expectedEndPosition - hidingShift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();
        final MotifSearchResult unsuccessfulSearch = motifSearchManager.search(testUnsuccessfulRequest);
        Assert.assertEquals(0, unsuccessfulSearch.getResult().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifAtEdges() throws IOException {
        final int expectedStartPosition = 1;
        final int expectedEndPosition = testChromosome.getSize();
        final int smallShift = 100;
        final int shift = 3000;

        final String testMotifAtBeginning = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedStartPosition + smallShift,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequestAtBeginning = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition)
                .endPosition(expectedStartPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotifAtBeginning)
                .build();
        final MotifSearchResult searchAtBeginning = motifSearchManager.search(testRequestAtBeginning);
        Assert.assertEquals(expectedStartPosition, searchAtBeginning.getResult().get(0).getStart());


        final String testMotifAtEnd = referenceManager.getSequenceString(
                expectedEndPosition - smallShift,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequestAtEnd = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedEndPosition - shift)
                .endPosition(expectedEndPosition)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotifAtEnd)
                .build();
        final MotifSearchResult searchAtEnd = motifSearchManager.search(testRequestAtEnd);
        Assert.assertEquals(expectedEndPosition - smallShift, searchAtEnd.getResult().get(0).getStart());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifsSequence() throws IOException {
        final int startPosition = 20000;
        final int endPosition = 21000;
        final int shift = 1000;

        final String expectedSequence = referenceManager.getSequenceString(
                startPosition,
                endPosition,
                testReference.getId(),
                testChromosome.getName());
        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(startPosition - shift)
                .endPosition(endPosition + shift)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(expectedSequence)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedSequence, search.getResult().get(0).getSequence());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchRegionMotifsReturnsEmptyValidRespondWhenMatchesNotFound() {

        final int testStart = END_POSITION;
        final int testEnd = 1050;
        final String testMotif = "acgttcgaacgttcga";

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(testStart)
                .endPosition(testEnd)
                .includeSequence(true)
                .searchType(MotifSearchType.REGION)
                .motif(testMotif)
                .build();

        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(0, search.getResult().size());
        Assert.assertEquals(0, search.getPageSize().longValue());
        Assert.assertEquals(testStart + 1, search.getPosition().longValue());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchChromosomeMotifsBounds() throws IOException {
        final int expectedEndPosition = testChromosome.getSize();
        final int expectedStartPosition = expectedEndPosition - END_POSITION;
        final int shift = END_POSITION;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedStartPosition, search.getResult().get(0).getStart());
        Assert.assertEquals(expectedEndPosition, search.getResult().get(0).getEnd());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchChromosomeMotifsSequence() throws IOException {
        final int expectedEndPosition = testChromosome.getSize();
        final int expectedStartPosition = expectedEndPosition - 10;
        final int shift = 10;

        final String testMotif = referenceManager.getSequenceString(
                expectedStartPosition,
                expectedEndPosition,
                testReference.getId(),
                testChromosome.getName());

        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(expectedStartPosition - shift)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(testMotif)
                .build();
        final MotifSearchResult search = motifSearchManager.search(testRequest);
        Assert.assertEquals(testMotif, search.getResult().get(0).getSequence());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchChromosomeMotifsShouldResultWithTheSameResultWhenBufferVaried() {

        final int testStart = 1;
        final int testEnd = 1000000;
        final int pageSize = 10000;
        final int slidingWindow = 15;
        final int expectedResultSize = 160;
        final int testBufferSizeStep = 123;
        final int numberOfBufferSteps = 5;
        final String testMotif = "acrywagt";


        final MotifSearchRequest testRequest = MotifSearchRequest.builder()
                .referenceId(testReference.getId())
                .chromosomeId(testChromosome.getId())
                .startPosition(testStart)
                .endPosition(testEnd)
                .includeSequence(true)
                .searchType(MotifSearchType.CHROMOSOME)
                .motif(testMotif)
                .pageSize(pageSize)
                .slidingWindow(slidingWindow)
                .build();

        final MotifSearchResult searchWithNormalBuffer = motifSearchManager.search(testRequest);
        Assert.assertEquals(expectedResultSize, searchWithNormalBuffer.getResult().size());

        IntStream.iterate(START_POSITION, i -> i + testBufferSizeStep).limit(numberOfBufferSteps).forEach(i -> {
            ReflectionTestUtils.setField(motifSearchManager, "bufferSize", i);
            final MotifSearchResult searchWithSmallBuffer = motifSearchManager.search(testRequest);
            Assert.assertEquals(searchWithNormalBuffer.getResult().size(), searchWithSmallBuffer.getResult().size());
        });
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchForSpecifiedPositionsShouldReturnNotEmptyResultsTest() {
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .startPosition(START_POSITION)
                .endPosition(END_POSITION)
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchForSpecifiedPageSizeShouldReturnResultsWithSizeEqualPageSizeTest() {
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .startPosition(1)
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        Assert.assertEquals(search.getResult().size(), (int) request.getPageSize());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchForLargeNumberOfResultsShouldReturnResultsFromSeveralChromosomesTest() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(hpGenomeTestReference.getId())
                .chromosomeId(hpGenomeTestReference.getChromosomes().get(1).getId())
                .motif(TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Set<String> chromosomes = search.getResult().stream().map(Motif::getContig)
                .collect(Collectors.toSet());
        Assert.assertTrue(chromosomes.size() > 1);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatPositionIsNullWhenWeSearchForLargeNumberOfResultsTest() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(hpGenomeTestReference.getId())
                .chromosomeId(hpGenomeTestReference.getChromosomes().get(1).getId())
                .startPosition(1)
                .motif(TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertNull(search.getPosition());
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatPositionIsNullWhenWeSearchFullGenomeButDataOnlyInFirstChrTest() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(testWgTestReference.getId())
                .startPosition(1)
                .chromosomeId(testWgTestReference.getChromosomes().get(0).getId())
                .motif("CCC")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertNull(search.getPosition());
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatResultsByRegexAndRevertedRegexIsEqualsTest() {
        final String motif = "AA[GGCCA(CAA)AT]";
        final String invertedMotif = "[AT(TTG)TGGCC]TT";
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(motif)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(BIG_PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        request = request.toBuilder()
                .motif(invertedMotif)
                .strand(StrandSerializable.NEGATIVE)
                .build();
        MotifSearchResult searchInverted = motifSearchManager.search(request);
        Assert.assertEquals(search.getResult().size(), searchInverted.getResult().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatResultsByIupacRegexAndRevertedRegexIsEqualsTest() {
        final String motif = "acryagt";
        final String invertedMotif = "actrygt";
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(motif)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(BIG_PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        request = request.toBuilder()
                .motif(invertedMotif)
                .strand(StrandSerializable.NEGATIVE)
                .build();
        MotifSearchResult searchInverted = motifSearchManager.search(request);
        Assert.assertEquals(search.getResult().size(), searchInverted.getResult().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatResultsIsEqualsWhenUsingIupacThatMustBeSameTest() {
        final String motif = "acwagt";
        final String invertedMotif = "actwgt";
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(motif)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(BIG_PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        request = request.toBuilder()
                .motif(invertedMotif)
                .strand(StrandSerializable.NEGATIVE)
                .build();
        MotifSearchResult searchInverted = motifSearchManager.search(request);
        Assert.assertEquals(search.getResult().size(), searchInverted.getResult().size());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchForSpecifiedPositionsByExtMotifShouldReturnNotEmptyResultsTest() {
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .startPosition(START_POSITION)
                .endPosition(END_POSITION)
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(EXTENDED_TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(Integer.MAX_VALUE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchForSpecifiedPageSizeByExtMotifShouldReturnResultsWithSizeEqualPageSizeTest() {
        MotifSearchRequest request = MotifSearchRequest.builder()
                .referenceId(a3TestReference.getId())
                .startPosition(1)
                .chromosomeId(a3TestReference.getChromosomes().get(0).getId())
                .motif(EXTENDED_TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(request);
        Assert.assertEquals(search.getResult().size(), (int) request.getPageSize());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void searchForLargeNumberOfResultsByExtMotifShouldReturnResultsFromSeveralChromosomesTest() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(hpGenomeTestReference.getId())
                .chromosomeId(hpGenomeTestReference.getChromosomes().get(1).getId())
                .motif(EXTENDED_TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Set<String> chromosomes = search.getResult().stream().map(Motif::getContig)
                .collect(Collectors.toSet());
        Assert.assertTrue(chromosomes.size() > 1);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatPositionIsNullWhenWeSearchByExtMotifForLargeNumberOfResultsTest() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(hpGenomeTestReference.getId())
                .chromosomeId(hpGenomeTestReference.getChromosomes().get(1).getId())
                .startPosition(1)
                .motif(EXTENDED_TEST_GENOME_MOTIF)
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(BIG_PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertNull(search.getPosition());
        Assert.assertFalse(search.getResult().isEmpty());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void checkThatPositionIsNullWhenWeSearchFullGenomeByExtMotifButDataOnlyInFirstChrTest() {
        MotifSearchRequest att = MotifSearchRequest.builder()
                .referenceId(testWgTestReference.getId())
                .startPosition(1)
                .chromosomeId(testWgTestReference.getChromosomes().get(0).getId())
                .motif("CC+")
                .searchType(MotifSearchType.WHOLE_GENOME)
                .pageSize(PAGE_SIZE)
                .strand(StrandSerializable.POSITIVE)
                .build();
        MotifSearchResult search = motifSearchManager.search(att);
        Assert.assertNull(search.getPosition());
        Assert.assertFalse(search.getResult().isEmpty());
    }
}
