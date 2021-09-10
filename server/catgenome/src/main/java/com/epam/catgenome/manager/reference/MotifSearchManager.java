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

package com.epam.catgenome.manager.reference;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.StrandedSequence;
import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@Slf4j
public class MotifSearchManager {

    private static final int TRACK_LENGTH = 100;
    private static final int DEFAULT_PAGE_SIZE = 100;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    public Track<StrandedSequence> fillTrackWithMotifSearch(final Track<StrandedSequence> track,
                                                            final String motif,
                                                            final StrandSerializable strand) {
        final List<StrandedSequence> result = search(
                MotifSearchRequest.builder()
                        .startPosition(track.getStartIndex())
                        .endPosition(track.getEndIndex())
                        .chromosomeId(track.getChromosome().getId())
                        .motif(motif)
                        .searchType(MotifSearchType.REGION)
                        .pageSize(0)
                        .strand(strand)
                        .build()).getResult().stream()
                .map(m -> new StrandedSequence(m.getStart(), m.getEnd(), m.getValue(), m.getStrand()))
                .collect(Collectors.toList());
        track.setBlocks(result);
        return track;
    }

    public MotifSearchResult search(final MotifSearchRequest request) {
        verifyMotifSearchRequest(request);
        switch (request.getSearchType()) {
            case WHOLE_GENOME:
                return searchWholeGenomeMotifs(request);
            case CHROMOSOME:
                return searchChromosomeMotifs(request);
            case REGION:
                return searchRegionMotifs(request);
            default:
                throw new IllegalStateException("Unexpected search type: " + request.getSearchType());
        }
    }

    private void verifyMotifSearchRequest(final MotifSearchRequest motifSearchRequest) {
        Assert.notNull(motifSearchRequest.getMotif(), getMessage("Motif is empty!"));
        final Integer start = motifSearchRequest.getStartPosition();
        final Integer end = motifSearchRequest.getEndPosition();
        final MotifSearchType searchType = motifSearchRequest.getSearchType();
        if (searchType.equals(MotifSearchType.REGION)) {
            Assert.notNull(motifSearchRequest.getChromosomeId(), getMessage("Chromosome not provided!"));
            Assert.notNull(start, getMessage("Start position is empty!"));
            Assert.notNull(end, getMessage("End position is empty!"));
            Assert.isTrue(end - start > 0,
                    getMessage("Provided end and start are not valid: " + end + " < " + start));
        } else if (searchType.equals(MotifSearchType.CHROMOSOME)) {
            Assert.notNull(motifSearchRequest.getChromosomeId(), getMessage("Chromosome not provided!"));
            if (end != null && start != null) {
                Assert.isTrue(end - start > 0,
                        getMessage("Provided end and start are not valid: " + end + " < " + start));
            }
        } else if (searchType.equals(MotifSearchType.WHOLE_GENOME)) {
            Assert.notNull(motifSearchRequest.getReferenceId(), getMessage("Genome id is empty!"));
        }
    }

    private MotifSearchResult searchRegionMotifs(final MotifSearchRequest request) {
        final Chromosome chromosome = loadChromosomeById(request.getReferenceId(), request.getChromosomeId());
        return MotifSearchResult.builder()
                .result(
                    fillMotifList(
                        chromosome, request.getStartPosition(),
                        request.getEndPosition(), 0,
                            request.getStrand()))
                .chromosomeId(request.getChromosomeId())
                .pageSize(request.getPageSize())
                .position(request.getEndPosition())
                .build();
    }

    private MotifSearchResult searchChromosomeMotifs(final MotifSearchRequest request) {
        final Chromosome chromosome = loadChromosomeById(request.getReferenceId(), request.getChromosomeId());
        final int start = request.getStartPosition() == null ? 0 : request.getStartPosition();
        final int end = request.getEndPosition() == null ? chromosome.getSize() : request.getEndPosition();
        final int pageSize = request.getPageSize() != null ? request.getPageSize() : DEFAULT_PAGE_SIZE;
        return MotifSearchResult.builder()
                .result(
                    fillMotifList(chromosome, start, end, pageSize,
                            request.getStrand()))
                .chromosomeId(request.getChromosomeId())
                .pageSize(request.getPageSize())
                .position(end)
                .build();
    }

    private MotifSearchResult searchWholeGenomeMotifs(final MotifSearchRequest request) {
        final List<Chromosome> chromosomes = referenceGenomeManager.loadChromosomes(request.getReferenceId());
        final Chromosome chr = loadChromosomeById(request.getReferenceId(), request.getChromosomeId());
        int start = request.getStartPosition() == null ? 0 : request.getStartPosition();
        int end = request.getEndPosition() == null ? chr.getSize() : request.getEndPosition();

        final List<Motif> result = fillMotifList(chr, start, end,
                request.getPageSize() != null ? request.getPageSize() : DEFAULT_PAGE_SIZE, request.getStrand());

        final Chromosome next = nextChromosome(chromosomes, chr.getId());
        final MotifSearchResult.MotifSearchResultBuilder resultBuilder = MotifSearchResult.builder()
                .result(result).pageSize(request.getPageSize());
        if (next != null) {
            resultBuilder.chromosomeId(next.getId()).position(0);
        }
        return resultBuilder.build();
    }

    private Chromosome nextChromosome(final List<Chromosome> chromosomes, final Long chromosomeId) {
        for (int i = 0; i < chromosomes.size() - 1; i++) {
            if (Objects.equals(chromosomes.get(i).getId(), chromosomeId)) {
                return chromosomes.get(i + 1);
            }
        }
        return null;
    }

    private Chromosome loadChromosomeById(final Long referenceId, final Long chromosomeId) {
        if (chromosomeId == null) {
            return getFirstChromosomeFromGenome(referenceId);
        } else {
            return referenceGenomeManager.loadChromosome(chromosomeId);
        }
    }

    private Chromosome getFirstChromosomeFromGenome(final Long referenceId) {
        return referenceGenomeManager.loadChromosomes(referenceId).get(0);
    }

    private List<Motif> fillMotifList(final Chromosome chromosome,
                                      final Integer trackStart, final Integer trackEnd,
                                      final Integer pageSize, final StrandSerializable strand) {
        final List<Motif> motifs = new ArrayList<>();
        if (pageSize == null || pageSize == 0) {
            motifs.addAll(getStubMotifList(chromosome, trackStart, trackEnd, strand, pageSize));
        } else {
            List<Motif> motifList = getStubMotifList(chromosome, trackStart, trackEnd, strand, pageSize);
            if (motifList.size() > pageSize) {
                motifs.addAll(motifList.stream().limit(pageSize).collect(Collectors.toList()));
            } else {
                motifs.addAll(motifList);
            }
        }
        return motifs;
    }

    private List<Motif> getStubMotifList(final Chromosome chromosome, final int trackStart, final int trackEnd,
                                         final StrandSerializable strand, final Integer pageSize) {
        int motifStart = trackStart;
        int count = pageSize != null && pageSize > 0 ? pageSize : Math.max(1, (trackEnd - trackStart) / TRACK_LENGTH);
        int distance = (trackEnd - trackStart) / count;

        final List<Motif> motifs = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            String value = generateString();
            int end = motifStart + value.length();
            motifs.add(
                Motif.builder().contig(chromosome.getName()).start(motifStart).end(end)
                        .strand(strand != null ? strand : StrandSerializable.POSITIVE)
                        .value(value).build());
            motifStart = motifStart + distance;
        }
        return motifs;
    }

    private String generateString() {
        String characters = "ATCG";
        Random rng = new Random();
        int length = rng.nextInt(TRACK_LENGTH);
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
}
