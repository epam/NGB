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

package com.epam.catgenome.manager.gene.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import com.epam.catgenome.entity.gene.Gene;
import com.epam.catgenome.entity.gene.GeneFile;
import com.epam.catgenome.entity.track.Block;
import com.epam.catgenome.manager.FileManager;
import com.epam.catgenome.manager.gene.GeneUtils;
import com.epam.catgenome.manager.parallel.TreeListMultiset;
import htsjdk.samtools.util.IntervalTreeMap;

/**
 * Source:      GffReader
 * Created:     13.10.16, 15:15
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * An implementation of AbstractGeneReader that is designed to read GFF files
 * </p>
 */
public class GffReader extends AbstractGeneReader {

    /**
     * Constructs a GffReader for specified GeneFile
     * @param executorService is required for gene reading
     * @param fileManager is required for file management
     * @param geneFile a GeneFile to read
     */
    public GffReader(ExecutorService executorService, FileManager fileManager, GeneFile geneFile) {
        super(executorService, fileManager, geneFile);
    }

    @Override
    protected void mapFeature(Gene currGene, ReaderState readerState, Map<String, Gene> overlappedMrnas, int start,
                            int end) {
        if (GeneUtils.isTranscript(currGene)) {
            readerState.mRnaMap.putIfAbsent(currGene.getParentId(), new ConcurrentHashMap<>());

            String transcriptId = GeneUtils.getTranscriptId(currGene);
            if (transcriptId != null) {
                readerState.mRnaMap.get(currGene.getParentId()).put(transcriptId, currGene);

                if (currGene.getStartIndex() < start || currGene.getEndIndex() > end) {
                    overlappedMrnas.put(transcriptId, currGene);
                }
            }
        } else {
            readerState.mRnaStuffMap.putIfAbsent(currGene.getParentId(), new ConcurrentHashMap<>());

            String transcriptId = currGene.getParentId();

            readerState.mRnaStuffMap.get(currGene.getParentId()).putIfAbsent(transcriptId,
                                                                             new CopyOnWriteArrayList<>());
            readerState.mRnaStuffMap.get(currGene.getParentId()).get(transcriptId).add(currGene);
        }
    }

    @Override
    protected void collapseFeatures(final ConcurrentMap<String, ConcurrentMap<String, List<Gene>>> mRnaStuffMap,
                                    Gene gene, final ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                                    Double scaleFactor, List<Gene> passedGenes) {
        ConcurrentMap<String, Gene> mrnas = mRnaMap.remove(gene.getGffId());

        if (mrnas != null && scaleFactor > LARGE_SCALE_FACTOR_LIMIT) {
            IntervalTreeMap<Gene> stuffIntervalMap = new IntervalTreeMap<>();
            Gene canonicalTranscript = createCanonicalTranscript(gene);

            for (Map.Entry<String, Gene> mrnaEntry : mrnas.entrySet()) {
                Gene transcript = mrnaEntry.getValue();
                setCanonicalTranscriptIndexes(canonicalTranscript, transcript);

                if (mRnaStuffMap.containsKey(transcript.getGffId()) &&
                        mRnaStuffMap.get(transcript.getGffId()).containsKey(transcript.getGffId())) {

                    List<Gene> mRnaStuff = mRnaStuffMap.get(transcript.getGffId()).remove(transcript.getGffId());
                    removeIfEmpty(mRnaStuffMap, transcript.getGffId());

                    groupMrnaStuff(mRnaStuff, stuffIntervalMap);
                }
            }

            canonicalTranscript.setItems(new ArrayList<>(stuffIntervalMap.values()));
            gene.setItems(Collections.singletonList(canonicalTranscript));

            if (gene.getExonsCount() == null) {
                calculateExonsCountAndLength(gene, canonicalTranscript.getItems());
            }
        }

        if (passesScaleFactor(gene, scaleFactor)) {
            passedGenes.add(gene);
        }
    }

    @Override
    protected void assembleFeatures(final ConcurrentMap<String, ConcurrentMap<String, List<Gene>>> mRnaStuffMap,
                                    Gene gene, final ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                                    Double scaleFactor, List<Gene> passedGenes) {
        ConcurrentMap<String, Gene> mrnas = mRnaMap.remove(gene.getGffId());

        if (mrnas != null && scaleFactor > LARGE_SCALE_FACTOR_LIMIT) {
            for (Map.Entry<String, Gene> mrnaEntry : mrnas.entrySet()) {
                if (mRnaStuffMap.containsKey(mrnaEntry.getValue().getGffId())
                        && mRnaStuffMap.get(mrnaEntry.getValue().getGffId())
                            .containsKey(mrnaEntry.getValue().getGffId())) {
                    List<Gene> mRnaStuff = mRnaStuffMap.get(mrnaEntry.getValue().getGffId()).remove(
                            mrnaEntry.getValue().getGffId());
                    removeIfEmpty(mRnaStuffMap, mrnaEntry.getValue().getGffId());
                    mrnaEntry.getValue().setItems(mRnaStuff);

                    setExonsCountAndLength(mrnaEntry.getValue(), mRnaStuff);
                }
            }

            gene.setItems(new ArrayList<>(mrnas.values()));
        }

        if (passesScaleFactor(gene, scaleFactor)) {
            passedGenes.add(gene);
        }
    }

    @Override
    protected void addUnmappedFeatures(List<Gene> genes, ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                             ConcurrentMap<String, ConcurrentMap<String, List<Gene>>> mRnaStuffMap, int step,
                             Double scaleFactor) {
        TreeListMultiset<Integer, Gene> multiset = new TreeListMultiset<>(Block::getStartIndex);

        // A map, mapping geneIds to map of mrna and transcript ids
        mRnaMap.entrySet().stream().forEach(geneToMrnasMapEntry -> {
            for (Map.Entry<String, Gene> mrnaEntry : geneToMrnasMapEntry.getValue().entrySet()) {
                List<Gene> mRnaStuff = mRnaStuffMap.get(mrnaEntry.getValue().getGffId()).remove(
                        mrnaEntry.getValue().getGffId());
                if (mRnaStuffMap.get(mrnaEntry.getValue().getGffId()).isEmpty()) {
                    mRnaStuffMap.remove(mrnaEntry.getValue().getGffId());
                }
                mrnaEntry.getValue().setItems(mRnaStuff);

                makeStatisticUnmappedFeature(mrnaEntry.getValue(), scaleFactor, multiset, step);
            }
        });

        mRnaStuffMap.values().stream().forEach(
            geneToMrnaStuffEntry -> geneToMrnaStuffEntry.values().stream().forEach(
                l -> l.stream().forEach(g -> makeStatisticUnmappedFeature(g, scaleFactor, multiset, step))));

        genes.addAll(multiset);
    }
}
