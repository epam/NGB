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
import com.epam.catgenome.parallel.TreeListMultiset;
import htsjdk.samtools.util.IntervalTreeMap;

/**
 * Source:      GeneReader
 * Created:     13.10.16, 12:58
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * <p>
 * A class for reading GTF files
 * </p>
 */
public class GtfReader extends AbstractGeneReader {
    public GtfReader(ExecutorService executorService, FileManager fileManager, GeneFile geneFile) {
        super(executorService, fileManager, geneFile);
    }

    @Override
    protected void mapFeature(Gene currGene, ReaderState readerState, Map<String, Gene> overlappedMrnas,
                            int start, int end) {
        String transcriptId = GeneUtils.getTranscriptId(currGene);

        if (GeneUtils.isTranscript(currGene)) {
            readerState.mRnaMap.putIfAbsent(currGene.getParentId(), new ConcurrentHashMap<>());

            if (transcriptId != null) {
                readerState.mRnaMap.get(currGene.getParentId()).put(transcriptId, currGene);

                if (currGene.getStartIndex() < start || currGene.getEndIndex() > end) {
                    overlappedMrnas.put(transcriptId, currGene);
                }
            }
        } else {
            readerState.mRnaStuffMap.putIfAbsent(currGene.getParentId(), new ConcurrentHashMap<>());

            if (transcriptId != null) {
                readerState.mRnaStuffMap.get(currGene.getParentId()).putIfAbsent(transcriptId,
                                                                                 new CopyOnWriteArrayList<>());
                readerState.mRnaStuffMap.get(currGene.getParentId()).get(transcriptId).add(currGene);
            }
        }
    }

    @Override
    protected void collapseFeatures(final ConcurrentMap<String, ConcurrentMap<String, List<Gene>>> mRnaStuffMap,
                                    Gene gene, final ConcurrentMap<String, ConcurrentMap<String, Gene>> mRnaMap,
                                    Double scaleFactor, List<Gene> passedGenes) {
        final ConcurrentMap<String, Gene> mrnas = mRnaMap.remove(gene.getGroupId());

        if (mrnas != null && scaleFactor > LARGE_SCALE_FACTOR_LIMIT) {
            IntervalTreeMap<Gene> stuffIntervalMap = new IntervalTreeMap<>();
            Gene canonicalTranscript = createCanonicalTranscript(gene);

            for (Map.Entry<String, Gene> mrnaEntry : mrnas.entrySet()) {
                setCanonicalTranscriptIndexes(canonicalTranscript, mrnaEntry.getValue());

                if (mRnaStuffMap.containsKey(gene.getGroupId())
                        && mRnaStuffMap.get(gene.getGroupId()).containsKey(mrnaEntry.getKey())) {
                    List<Gene> mRnaStuff = mRnaStuffMap.get(gene.getGroupId()).remove(mrnaEntry.getKey());
                    removeIfEmpty(mRnaStuffMap, gene.getGroupId());

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
        final ConcurrentMap<String, Gene> mrnas = mRnaMap.remove(gene.getGroupId());

        if (mrnas != null && scaleFactor > LARGE_SCALE_FACTOR_LIMIT) {
            for (Map.Entry<String, Gene> mrnaEntry : mrnas.entrySet()) {
                if (mRnaStuffMap.containsKey(gene.getGroupId())
                        && mRnaStuffMap.get(gene.getGroupId()).containsKey(mrnaEntry.getKey())) {
                    List<Gene> mRnaStuff = mRnaStuffMap.get(gene.getGroupId()).remove(mrnaEntry.getKey());

                    removeIfEmpty(mRnaStuffMap, gene.getGroupId());
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
    protected void addUnmappedFeatures(final List<Gene> genes, final ConcurrentMap<String, ConcurrentMap<String, Gene>>
            mRnaMap, final ConcurrentMap<String, ConcurrentMap<String, List<Gene>>> mRnaStuffMap, int step,
                                        Double scaleFactor) {

        TreeListMultiset<Integer, Gene> multiset = new TreeListMultiset<>(Block::getStartIndex);

        // A map, mapping geneIds to map of mrna and transcript ids
        mRnaMap.entrySet().stream().forEach(geneToMrnasMapEntry -> {
            for (Map.Entry<String, Gene> mrnaEntry : geneToMrnasMapEntry.getValue().entrySet()) {
                List<Gene> mRnaStuff = mRnaStuffMap.get(geneToMrnasMapEntry.getKey()).remove(mrnaEntry.getKey());
                if (mRnaStuffMap.get(geneToMrnasMapEntry.getKey()).isEmpty()) {
                    mRnaStuffMap.remove(geneToMrnasMapEntry.getKey());
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
