/*
 * MIT License
 *
 * Copyright (c) 2021-2022 EPAM Systems
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

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.BaseEntity;
import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.gene.GeneFilterForm;
import com.epam.catgenome.entity.index.FeatureIndexEntry;
import com.epam.catgenome.entity.index.FeatureType;
import com.epam.catgenome.entity.index.GeneIndexEntry;
import com.epam.catgenome.entity.index.IndexSearchResult;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.StrandedSequence;
import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.entity.reference.motif.MotifSearchRequest;
import com.epam.catgenome.entity.reference.motif.MotifSearchResult;
import com.epam.catgenome.entity.reference.motif.MotifSearchType;
import com.epam.catgenome.entity.reference.motif.Region;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.exception.GeneReadingException;
import com.epam.catgenome.manager.FeatureIndexManager;
import com.epam.catgenome.manager.gene.GeneFileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.gene.GffManager;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import com.epam.catgenome.util.NggbIntervalTreeMap;
import com.epam.catgenome.util.motif.MotifSearcher;
import htsjdk.samtools.util.Interval;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@Slf4j
public class MotifSearchManager {

    @Value("${motif.search.buffer.size:16000000}")
    private int bufferSize;

    @Value("${motif.search.sliding.window:1000}")
    private int defaultOverlap;

    @Value("${motif.search.page.size:100}")
    private int defaultPageSize;

    @Value("${motif.search.include.sequence:false}")
    private boolean defaultIncludeSequence;

    @Value("${motif.search.result.size.limit:131072}")
    private int searchResultSizeLimit;

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private FeatureIndexManager featureIndexManager;

    @Autowired
    private GeneFileManager geneFileManager;

    @Autowired
    private GffManager gffManager;

    public Track<StrandedSequence> fillTrackWithMotifSearch(final Track<StrandedSequence> track,
                                                            final String motif,
                                                            final StrandSerializable strand) {
        final List<StrandedSequence> result = search(
                MotifSearchRequest.builder()
                        .referenceId(track.getId())
                        .startPosition(track.getStartIndex())
                        .endPosition(track.getEndIndex())
                        .chromosomeId(track.getChromosome().getId())
                        .motif(motif)
                        .searchType(MotifSearchType.REGION)
                        .pageSize(Integer.MAX_VALUE)
                        .strand(strand)
                        .build()).getResult().stream()
                .map(m -> new StrandedSequence(m.getStart(), m.getEnd(), m.getSequence(), m.getStrand()))
                .collect(Collectors.toList());
        track.setBlocks(result);
        return track;
    }

    public MotifSearchResult search(final MotifSearchRequest request) {
        return search(request, false);
    }

    public MotifSearchResult search(final MotifSearchRequest request, final boolean loadGenes) {
        verifyMotifSearchRequest(request);
        final Reference reference = loadReferenceWithChromosomes(request);
        final List<GeneIndexEntry> filterGenes = fetchGenes(request, reference);
        if (CollectionUtils.isNotEmpty(filterGenes)) {
            return searchGeneMotifs(request, reference, filterGenes, loadGenes);
        }
        switch (request.getSearchType()) {
            case WHOLE_GENOME:
                return searchWholeGenomeMotifs(request, reference, loadGenes);
            case CHROMOSOME:
                return searchChromosomeMotifs(request, reference, loadGenes);
            case REGION:
                return searchRegionMotifs(request, reference, loadGenes);
            default:
                throw new IllegalStateException("Unexpected search type: " + request.getSearchType());
        }
    }

    public StrandedSequence getNextMotif(final MotifSearchRequest motifSearchRequest) {
        verifyNextOrPrevSearchRequest(motifSearchRequest);
        final Chromosome chromosome = fetchChromosomeById(
                loadReferenceWithChromosomes(motifSearchRequest),
                motifSearchRequest.getChromosomeId());
        // please see explanation for this calculation in getPreviousMotif() method
        final int shift = Math.max(1, searchResultSizeLimit - 2 * validateAndAdjustOverlap(motifSearchRequest) - 1);
        int from = motifSearchRequest.getStartPosition();
        int to = Math.min(chromosome.getSize(), from + shift);
        while (from < chromosome.getSize()) {
            final MotifSearchResult result = search(
                    motifSearchRequest.toBuilder()
                            .startPosition(from)
                            .endPosition(to)
                            .searchType(MotifSearchType.REGION)
                            .pageSize(searchResultSizeLimit).build()
            );
            final Optional<StrandedSequence> next = result.getResult()
                    .stream()
                    .filter(m -> m.getStart() > motifSearchRequest.getStartPosition())
                    .map(m -> new StrandedSequence(m.getStart(), m.getEnd(), m.getSequence(), m.getStrand()))
                    .findFirst();

            if (next.isPresent()) {
                return next.get();
            }

            from = to;
            to = Math.min(chromosome.getSize(), from + shift);
        }
        throw  new IllegalStateException("No next motif can be found!");
    }

    public StrandedSequence getPreviousMotif(final MotifSearchRequest motifSearchRequest) {
        verifyNextOrPrevSearchRequest(motifSearchRequest);
        // we calculate shift window in this way, because in the worst case
        // we can have searchResultSizeLimit number of result
        // (for example when each nucleotide is matched: pattern [ACGT])
        // and we need to find exactly last result to find a previous match.
        // to get maximum searchResultSizeLimit result we need to adjust shift window regarding overlapping mechanism
        // that will be used in search() method
        final int shift = Math.max(1, searchResultSizeLimit - 2 * validateAndAdjustOverlap(motifSearchRequest) - 1);
        int from = Math.max(0, motifSearchRequest.getStartPosition() - shift);
        int to = Math.max(0, motifSearchRequest.getStartPosition());
        while (from != 0 || to != 0) {
            Optional<Motif> result = search(
                    motifSearchRequest.toBuilder()
                            .startPosition(from).endPosition(to)
                            .searchType(MotifSearchType.REGION)
                            .pageSize(searchResultSizeLimit).build())
                    .getResult()
                    .stream()
                    .sorted((m1, m2) -> m2.getStart() - m1.getStart())
                    .filter(m -> m.getStart() < motifSearchRequest.getStartPosition())
                    .findFirst();
            if (result.isPresent()) {
                final Motif prevMotif = result.get();
                return new StrandedSequence(prevMotif.getStart(), prevMotif.getEnd(),
                        prevMotif.getSequence(), prevMotif.getStrand());
            } else {
                to = from;
                from = Math.max(0, from - shift);
            }

        }
        throw new IllegalStateException("No previous motif can be found!");
    }

    private void verifyMotifSearchRequest(final MotifSearchRequest request) {
        Assert.notNull(request.getSearchType(), getMessage("Search type is empty!"));
        Assert.notNull(request.getMotif(), getMessage("Motif is empty!"));
        Assert.notNull(request.getReferenceId(), getMessage("Genome id is empty!"));
        final Integer start = request.getStartPosition();
        final Integer end = request.getEndPosition();
        final MotifSearchType searchType = request.getSearchType();
        if (searchType.equals(MotifSearchType.WHOLE_GENOME)) {
            return;
        }
        Assert.notNull(request.getChromosomeId(), getMessage("Chromosome not provided!"));
        Assert.isTrue(CollectionUtils.isEmpty(request.getChromosomeFilter()),
                getMessage("Chromosome filter is supported only for whole genome search!"));
        if (end != null && start != null) {
            Assert.isTrue(end - start > 0,
                    getMessage("Provided end and start are not valid: " + end + " < " + start));
        }
        if (searchType.equals(MotifSearchType.CHROMOSOME)) {
            return;
        }
        Assert.isTrue(CollectionUtils.isEmpty(request.getGeneFilter()),
                getMessage("Gene filter is not supported for region queries!"));
        Assert.notNull(start, getMessage("Start position is empty!"));
        Assert.notNull(end, getMessage("End position is empty!"));
    }

    private void verifyNextOrPrevSearchRequest(final MotifSearchRequest request) {
        Assert.notNull(request.getMotif(), getMessage("Motif is empty!"));
        Assert.notNull(request.getReferenceId(), getMessage("Genome id is empty!"));
        Assert.notNull(request.getChromosomeId(), getMessage("Chromosome is not provided!"));
        Assert.notNull(request.getStrand(), getMessage("Strand is not provided!"));
        final Integer start = request.getStartPosition();
        final Integer end = request.getEndPosition();
        if (end != null && start != null) {
            Assert.isTrue(end - start > 0,
                    getMessage("Provided end and start are not valid: " + end + " < " + start));
        }
        Assert.notNull(start, getMessage("Start position is empty!"));
    }

    private MotifSearchResult searchGeneMotifs(final MotifSearchRequest request,
                                               final Reference reference,
                                               final List<GeneIndexEntry> filterGenes,
                                               final boolean loadGenes) {
        final int pageSize = Optional.ofNullable(request.getPageSize()).orElse(defaultPageSize);
        final List<Motif> motifs = new ArrayList<>();
        for (final Region region : getRequestIntervals(filterGenes, request, reference.getChromosomes())) {
            motifs.addAll(searchRegionMotifs(MotifSearchRequest.builder()
                    .motif(request.getMotif())
                    .referenceId(request.getReferenceId())
                    .chromosomeId(region.getChromosomeId())
                    .startPosition(region.getStart())
                    .endPosition(region.getEnd())
                    .pageSize(pageSize)
                    .includeSequence(request.getIncludeSequence())
                    .strand(request.getStrandFilter())
                    .slidingWindow(request.getSlidingWindow())
                    .build(), reference, loadGenes)
                    .getResult());

            if (motifs.size() >= pageSize) {
                break;
            }
        }
        final List<Motif> result = motifs.stream()
                .limit(Math.min(motifs.size(), pageSize))
                .collect(Collectors.toList());
        final Optional<Motif> lastMotif = CollectionUtils.isEmpty(result) ? Optional.empty() :
                Optional.of(result.get(result.size() - 1));

        return MotifSearchResult.builder()
                .result(result)
                .chromosomeId(lastMotif.flatMap(motif -> reference.getChromosomes()
                        .stream()
                        .filter(chr -> chr.getName().equals(motif.getContig()))
                        .findFirst()
                        .map(BaseEntity::getId))
                        .orElse(null))
                .pageSize(pageSize)
                .position(lastMotif.map(m -> m.getEnd() + 1).orElse(null))
                .build();
    }

    private MotifSearchResult searchRegionMotifs(final MotifSearchRequest request,
                                                 final Reference reference,
                                                 final boolean loadGenes) {
        final Chromosome chromosome = fetchChromosomeById(reference, request.getChromosomeId());
        Assert.isTrue(request.getEndPosition() <= chromosome.getSize(),
                getMessage(MessagesConstants.ERROR_POSITION_OUT_OF_RANGE, request.getEndPosition()));
        final boolean includeSequence = request.getIncludeSequence() == null
                        ? defaultIncludeSequence
                        : request.getIncludeSequence();
        final int pageSize = request.getPageSize() == null
                ? Integer.MAX_VALUE
                : request.getPageSize();
        final int overlap = validateAndAdjustOverlap(request);
        final int startPosition = Math.max(1, request.getStartPosition() - overlap);
        final int endPosition = Math.min(chromosome.getSize(), request.getEndPosition() + overlap);

        final Iterator<Motif> motifIterator = MotifSearcher.search(
                getSequence(startPosition, endPosition, reference, chromosome),
                        request.getMotif(), request.getStrandFilter(), chromosome.getName(),
                        startPosition, includeSequence, searchResultSizeLimit
                ).filter(motif -> motif.getEnd() >= request.getStartPosition()
                        && motif.getStart() <= request.getEndPosition())
                .iterator();

        final List<Motif> searchResult = new ArrayList<>();
        while (motifIterator.hasNext()) {
            Motif next = motifIterator.next();
            if (searchResult.size() >= pageSize) {
                break;
            }
            searchResult.add(next);
            checkSizeOfMotifSearchResult(searchResult);
        }
        if (loadGenes && CollectionUtils.isNotEmpty(searchResult) && reference.getGeneFile() != null) {
            attachGenesFromFile(searchResult, reference, chromosome);
        }
        final int lastStart = searchResult.isEmpty()
                ? request.getStartPosition()
                : searchResult.get(searchResult.size() - 1).getStart();
        return MotifSearchResult.builder()
                .result(searchResult)
                .chromosomeId(request.getChromosomeId())
                .pageSize(request.getPageSize())
                .position(lastStart < chromosome.getSize() ? lastStart + 1 : null)
                .build();
    }

    /**
     * Loads a reference sequence for a given request for a specified chromosome
     *
     * @param startPosition  start position in chromosome sequence
     *                       (the start position for the beginning of the chromosome: "1",
     *                       the end position of the whole chromosome: chromosome.size(),
     *                       start positions less than 1 (e.g. "0") will be turned into "1")
     *  @param endPosition   end position in chromosome sequence
     * @param chromosome     chromosome for search
     * @return a byte array representation of a reference sequence for the specified
     */
    private byte[] getSequence(final int startPosition, final int endPosition, final Reference reference,
                               final Chromosome chromosome) {
        final byte[] sequence;
        try {
            sequence= referenceManager.getSequenceByteArray(startPosition,
                            endPosition, reference, chromosome.getName());
        } catch (IOException e) {
            throw new IllegalStateException(getMessage(MessagesConstants.ERROR_REFERENCE_SEQUENCE_READING));
        }
        return sequence;
    }

    private MotifSearchResult searchChromosomeMotifs(final MotifSearchRequest request,
                                                     final Reference reference,
                                                     final boolean loadGenes) {
        final Chromosome chromosome = fetchChromosomeById(reference, request.getChromosomeId());
        Assert.isTrue(request.getEndPosition() == null || request.getEndPosition() <= chromosome.getSize(),
                getMessage(MessagesConstants.ERROR_POSITION_OUT_OF_RANGE, request.getEndPosition()));
        final int pageSize = request.getPageSize() == null || request.getPageSize() <= 0
                ? defaultPageSize
                : request.getPageSize();
        final int start = request.getStartPosition() == null ? 0 : request.getStartPosition();
        final int end = request.getEndPosition() == null ? chromosome.getSize() : request.getEndPosition();

        final Set<Motif> result = new LinkedHashSet<>();
        int currentStart = start;
        int currentEnd = Math.min(this.bufferSize, end - start) + start;

        while (result.size() < pageSize && currentStart < end) {
            result.addAll(searchRegionMotifs(MotifSearchRequest.builder()
                            .motif(request.getMotif())
                            .referenceId(request.getReferenceId())
                            .chromosomeId(request.getChromosomeId())
                            .startPosition(currentStart)
                            .endPosition(currentEnd)
                            .pageSize(pageSize)
                            .includeSequence(request.getIncludeSequence())
                            .strand(request.getStrandFilter())
                            .slidingWindow(request.getSlidingWindow())
                            .build(),
                    reference,
                    false
            ).getResult());
            checkSizeOfMotifSearchResult(result);
            currentStart = currentStart + bufferSize;
            currentEnd = Math.min(currentEnd + bufferSize, end);
        }
        final List<Motif> pageSizedResult = result.stream()
                .limit(Math.min(result.size(), pageSize))
                .collect(Collectors.toList());

        if (loadGenes && CollectionUtils.isNotEmpty(pageSizedResult) && reference.getGeneFile() != null) {
            attachGenesFromFile(pageSizedResult, reference, chromosome);
        }

        final Integer lastStartMotifPosition = pageSizedResult.isEmpty() ||
                (end == chromosome.getSize() && pageSizedResult.size() < pageSize)
                ? null
                : pageSizedResult.get(pageSizedResult.size() - 1).getStart();
        return MotifSearchResult.builder()
                .result(pageSizedResult)
                .chromosomeId(request.getChromosomeId())
                .pageSize(pageSize)
                .position(lastStartMotifPosition == null || lastStartMotifPosition.equals(chromosome.getSize())
                        ? null
                        : lastStartMotifPosition + 1)
                .build();
    }

    private void attachGenesFromFile(final List<Motif> motifs,
                                     final Reference reference,
                                     final Chromosome chromosome) {
        final int startQuery = motifs.get(0).getStart();
        final int endQuery = motifs.get(motifs.size() - 1).getEnd();
        final GeneFile geneFile = geneFileManager.load(reference.getGeneFile().getId());
        try {
            final NggbIntervalTreeMap<Gene> intervalMap = gffManager.loadGenesIntervalMap(
                    geneFile, startQuery, endQuery, chromosome);
            if (!intervalMap.isEmpty()) {
                motifs.forEach(motif -> {
                    motif.setGeneIds(new ArrayList<>());
                    motif.setGeneNames(new ArrayList<>());
                    final Collection<Gene> genes = intervalMap.getOverlapping(
                            new Interval(motif.getContig(), motif.getStart(), motif.getEnd()));
                    genes.stream()
                            .filter(GeneUtils::isGene)
                            .forEach(g -> {
                                motif.getGeneIds().add(g.getFeatureId());
                                motif.getGeneNames().add(g.getFeatureName());
                            });
                });
            }
        } catch (GeneReadingException e) {
            log.error(e.getMessage(), e);
        }
    }

    private int validateAndAdjustOverlap(final MotifSearchRequest request) {
        return request.getSlidingWindow() == null
                || request.getSlidingWindow() <= 0
                || request.getSlidingWindow() >= bufferSize
                ? defaultOverlap
                : request.getSlidingWindow();
    }

    private MotifSearchResult searchWholeGenomeMotifs(final MotifSearchRequest request,
                                                      final Reference reference,
                                                      final boolean loadGenes) {
        final int pageSize = request.getPageSize() == null ? defaultPageSize : request.getPageSize();
        int start = request.getStartPosition() == null ? 0 : request.getStartPosition();
        Chromosome chromosome = fetchChromosomeById(reference, request.getChromosomeId());
        long chrId = chromosome.getId();
        final List<Chromosome> chromosomes = reference.getChromosomes();
        final List<Motif> motifs = new ArrayList<>();
        while (chromosome != null && pageSize - motifs.size() > 0) {
            chrId = chromosome.getId();
            if (CollectionUtils.isNotEmpty(request.getChromosomeFilter()) &&
                    !request.getChromosomeFilter().contains(chrId)) {
                log.debug("Requested chromosome {} doesn't match filter {}", chromosome.getId(),
                        request.getChromosomeFilter());
                start = 0;
                chromosome = getNextChromosome(chromosomes, chromosome);
                continue;
            }
            motifs.addAll(searchChromosomeMotifs(
                    request.toBuilder()
                            .pageSize(pageSize - motifs.size())
                            .searchType(MotifSearchType.CHROMOSOME)
                            .chromosomeId(chrId)
                            .startPosition(start)
                            .endPosition(chromosome.getSize())
                            .slidingWindow(request.getSlidingWindow())
                            .build(),
                    reference, loadGenes)
                    .getResult()
                    .stream()
                    .limit(pageSize - motifs.size())
                    .collect(Collectors.toList()));
            start = 0;
            chromosome = getNextChromosome(chromosomes, chromosome);
            checkSizeOfMotifSearchResult(motifs);
        }
        return MotifSearchResult.builder()
                .result(motifs)
                .pageSize(pageSize)
                .chromosomeId(chrId)
                .position(chromosome == null && pageSize - motifs.size() > 0 ? null
                        : motifs.get(motifs.size() - 1).getStart())
                .build();
    }

    private Chromosome getNextChromosome(final List<Chromosome> chromosomes, final Chromosome chromosome) {
        for (int i = 0; i < chromosomes.size() - 1; i++) {
            if (chromosomes.get(i).getName().equals(chromosome.getName())) {
                return chromosomes.get(i + 1);
            }
        }
        return null;
    }

    private Reference loadReferenceWithChromosomes(final MotifSearchRequest request) {
        Reference reference = referenceGenomeManager.getOnlyReference(request.getReferenceId());
        if (reference == null) {
            throw new IllegalStateException(getMessage(
                    MessagesConstants.ERROR_REFERENCE_READING, request.getReferenceId()));
        }
        reference.setChromosomes(referenceGenomeManager.loadChromosomes(request.getReferenceId()));
        return reference;
    }

    private Chromosome fetchChromosomeById(final Reference reference, final Long chromosomeId) {
        return reference.getChromosomes().stream()
                .filter(chr -> chromosomeId == null || chromosomeId.equals(chr.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        getMessage(MessagesConstants.ERROR_WRONG_CHROMOSOME_ID, chromosomeId)));
    }

    private void checkSizeOfMotifSearchResult(final Collection<Motif> motifs) {
        Assert.isTrue(motifs.size() <= searchResultSizeLimit,
                "Too many result, specify more concrete query. Configured max result size: " + searchResultSizeLimit);

    }

    private List<Region> getRequestIntervals(final List<GeneIndexEntry> genes,
                                             final MotifSearchRequest request,
                                             final List<Chromosome> chromosomes) {
        final List<Region> regions = genes.stream()
                .map(gene -> Region
                        .builder()
                        .chromosomeId(gene.getChromosome().getId())
                        .start(gene.getStartIndex())
                        .end(gene.getEndIndex())
                        .strand(StrandSerializable.forValue(gene.getStrand()))
                        .build())
                .collect(Collectors.toList());
        if (Objects.isNull(request.getChromosomeId())) {
            return mergeRegions(regions);
        }

        final Set<Long> filteredChromosomes = chromosomes.stream()
                .skip(getChromosomeIndex(chromosomes, request.getChromosomeId()))
                .map(BaseEntity::getId)
                .collect(Collectors.toSet());

        final List<Region> filteredRegions = regions.stream()
                .filter(region -> filteredChromosomes.contains(region.getChromosomeId()))
                .map(region -> {
                    if (Objects.nonNull(request.getStartPosition())
                            && region.getChromosomeId().equals(request.getChromosomeId())) {
                        //shift gene start to requested interval
                        return region
                                .toBuilder()
                                .start(Math.max(request.getStartPosition(), region.getStart()))
                                .build();
                    }
                    return region;
                })
                .collect(Collectors.toList());
        return mergeRegions(filteredRegions);
    }

    private List<Region> mergeRegions(final List<Region> regions) {
        if (regions.size() <= 1) {
            return regions;
        }
        final List<Region> result = new ArrayList<>();
        Region current = regions.get(0);
        for (int i = 1; i < regions.size(); i++) {
            final Region next = regions.get(i);
            if (current.overlap(next)) {
                current = current.merge(next);
            } else {
                result.add(current);
                current = next;
            }
        }
        if (!result.contains(current)) {
            result.add(current);
        }
        return result;
    }

    private Integer getChromosomeIndex(final List<Chromosome> chromosomes,
                                       final Long chromosomeId) {
        for (int i = 0; i < chromosomes.size(); i++) {
            if (chromosomes.get(i).getId().equals(chromosomeId)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                getMessage("Chromosome {} was not found in reference", chromosomeId));
    }

    private List<GeneIndexEntry> fetchGenes(final MotifSearchRequest request,
                                            final Reference reference) {
        if (CollectionUtils.isEmpty(request.getGeneFilter())) {
            return Collections.emptyList();
        }
        final GeneFilterForm geneFilterForm = new GeneFilterForm();
        geneFilterForm.setFeatureNames(request.getGeneFilter());
        geneFilterForm.setFeatureTypes(Collections.singletonList(FeatureType.GENE.getFileValue()));
        geneFilterForm.setChromosomeIds(getChromosomeFilter(request));
        geneFilterForm.setPageSize(defaultPageSize);
        geneFilterForm.setPage(1);
        try {
            final IndexSearchResult<GeneIndexEntry> result = featureIndexManager
                    .searchGenesByReference(geneFilterForm, request.getReferenceId());

            final List<GeneIndexEntry> entries = result.getEntries();
            if (CollectionUtils.isEmpty(entries)) {
                throw new IllegalStateException("No genes match specified filter: " + request.getGeneFilter());
            }
            entries.sort(Comparator.comparing(FeatureIndexEntry::getStartIndex));

            if (request.getSearchType().equals(MotifSearchType.CHROMOSOME)) {
                return entries;
            }

            final Map<Long, List<GeneIndexEntry>> chromosomeToGenes = entries.stream()
                    .collect(Collectors.groupingBy(e -> e.getChromosome().getId()));
            final List<GeneIndexEntry> sortedGenes = new ArrayList<>();
            reference.getChromosomes()
                    .forEach(chr ->
                            sortedGenes.addAll(chromosomeToGenes.getOrDefault(chr.getId(), Collections.emptyList())));
            return sortedGenes;
        } catch (IOException e) {
            log.error("Failed to fetch gene for motif filter: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private List<Long> getChromosomeFilter(final MotifSearchRequest request) {
        if (CollectionUtils.isNotEmpty(request.getChromosomeFilter())) {
            return request.getChromosomeFilter();
        }
        if (request.getSearchType().equals(MotifSearchType.CHROMOSOME)) {
            return Collections.singletonList(request.getChromosomeId());
        }
        return Collections.emptyList();
    }
}
