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
import java.util.Random;
import java.util.stream.Collectors;

import static com.epam.catgenome.component.MessageHelper.getMessage;

@Service
@Slf4j
public class MotifSearchManager {

    @Autowired
    private ReferenceGenomeManager referenceGenomeManager;

    @Autowired
    private ReferenceManager referenceManager;

    private final int TRACK_LENGTH = 100;

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
        }
        throw new IllegalArgumentException("Search type is empty!");
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
            Assert.notNull(start, getMessage("Start position is empty!"));
            if (end != null) {
                Assert.isTrue(end - start > 0,
                        getMessage("Provided end and start are not valid: " + end + " < " + start));
            }
        } else if (searchType.equals(MotifSearchType.WHOLE_GENOME)) {
            Assert.notNull(motifSearchRequest.getReferenceId(), getMessage("Genome id is empty!"));
        }
    }

    private MotifSearchResult searchRegionMotifs(final MotifSearchRequest motifSearchRequest) {
        return MotifSearchResult.builder()
                .result(fillMotifList(motifSearchRequest.getStartPosition(),
                        motifSearchRequest.getEndPosition(),
                        0,
                        motifSearchRequest.getMotif(),
                        motifSearchRequest.getChromosomeId()))
                .chromosomeId(motifSearchRequest.getChromosomeId())
                .pageSize(motifSearchRequest.getPageSize())
                .position(motifSearchRequest.getEndPosition())
                .build();
    }

    private MotifSearchResult searchChromosomeMotifs(final MotifSearchRequest motifSearchRequest) {
        int start = motifSearchRequest.getStartPosition() == null
                ? 0
                : motifSearchRequest.getStartPosition();
        int end = motifSearchRequest.getEndPosition() == null
                ? getChrLength(motifSearchRequest.getChromosomeId())
                : motifSearchRequest.getEndPosition();
        return MotifSearchResult.builder()
                .result(fillMotifList(start,
                        end,
                        motifSearchRequest.getPageSize(),
                        motifSearchRequest.getMotif(),
                        motifSearchRequest.getChromosomeId()))
                .chromosomeId(motifSearchRequest.getChromosomeId())
                .pageSize(motifSearchRequest.getPageSize())
                .position(end)
                .build();
    }

    private int getChrLength(Long chromosomeId) {
        Chromosome chr;
        try {
            chr = referenceGenomeManager.loadChromosome(chromosomeId);
        } catch (Exception e) {
            chr = getNewChromosome(chromosomeId);
        }
        return chr.getSize();
    }

    private MotifSearchResult searchWholeGenomeMotifs(final MotifSearchRequest motifSearchRequest) {
        Chromosome chr = loadChrById(motifSearchRequest.getChromosomeId(), motifSearchRequest.getReferenceId());
        int start = motifSearchRequest.getStartPosition() == null
                ? 0
                : motifSearchRequest.getStartPosition();
        int end = motifSearchRequest.getEndPosition() == null
                ? chr.getSize()
                : motifSearchRequest.getEndPosition();
        return MotifSearchResult.builder()
                .result(fillMotifList(start,
                        end,
                        motifSearchRequest.getPageSize(),
                        motifSearchRequest.getMotif(),
                        chr.getId()))
                .chromosomeId(chr.getId())
                .pageSize(motifSearchRequest.getPageSize())
                .position(end)
                .build();
    }

    private Chromosome loadChrById(Long chromosomeId, Long referenceId) {
        Chromosome chr;
        if (chromosomeId == null) {
            chr = getFirstChromosomeFromGenome(referenceId);
        } else {
            try {
                chr = referenceGenomeManager.loadChromosome(chromosomeId);
            } catch (Exception e) {
                chr = getFirstChromosomeFromGenome(referenceId);
            }
        }
        return chr;
    }

    private Chromosome getFirstChromosomeFromGenome(Long referenceId) {
        Chromosome chr;
        try {
            chr = referenceGenomeManager.loadChromosomes(referenceId).get(0);
        } catch (Exception e) {
            chr = getNewChromosome(0L);
        }
        return chr;
    }

    private Chromosome getNewChromosome(Long chromosomeId) {
        Chromosome chr = new Chromosome(chromosomeId);
        chr.setSize(1_000_000);
        chr.setName("chr" + (chromosomeId + 1));
        return chr;
    }

    private List<Motif> fillMotifList(final Integer trackStart, final Integer trackEnd,
                                      final Integer pageSize, final String motif,
                                      final Long chromosomeId) {
        final List<Motif> motifs = new ArrayList<>();
        if (pageSize == null || pageSize == 0) {
            motifs.addAll(getStubMotifList(trackStart, trackEnd, chromosomeId));
        } else {
            List<Motif> motifList = getStubMotifList(trackStart, trackEnd, chromosomeId);
            if (motifList.size() > pageSize) {
                motifs.addAll(motifList.stream().limit(pageSize).collect(Collectors.toList()));
            } else {
                motifs.addAll(motifList);
            }
        }
        return motifs;
    }

    private List<Motif> getStubMotifList(final Integer trackStart, final Integer trackEnd,
                                         final Long chromosomeId) {
        String chrName;
        try {
            Chromosome chr = referenceGenomeManager.loadChromosome(chromosomeId);
            chrName = chr.getName();
        } catch (Exception e) {
            chrName = "chr" + (chromosomeId + 1);
        }
        final int motifStart = trackStart == null ? 0 : trackStart;
        final int motifEnd = trackEnd == null ? (motifStart + 1000) : trackEnd;
        int count = (motifEnd - motifStart) / TRACK_LENGTH;
        List<Motif> motifs = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            int start = (motifStart + TRACK_LENGTH * i);
            String value = generateString();
            int end = start + value.length();
            Motif curMotif = new Motif(chrName, start, end, StrandSerializable.POSITIVE, value);
            motifs.add(curMotif);
        }
        return motifs;
    }

    private String generateString() {
        String characters = "ATCG";
        Random rng = new java.util.Random();
        int length = rng.nextInt(TRACK_LENGTH);
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
}
