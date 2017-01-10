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

package com.epam.catgenome.manager.reference.io;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.manager.bam.BamHelper;
import com.epam.catgenome.manager.reference.ReferenceManager;
import com.epam.catgenome.util.Utils;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;

/**
 * {@code ChromosomeReferenceSequence} provides an implementation of {@code ReferenceSequenceFile}
 * interface for reading reference data for CRAM file format decoding. This implementation wraps
 * an NGB reference, where each chromosome is stored in a separate file in the Nib format.
 */
public class ChromosomeReferenceSequence implements ReferenceSequenceFile {

    private Set<Chromosome> chromosomes;
    private Map<String, Chromosome> chromosomeMap;
    private SAMSequenceDictionary dictionary;
    private Iterator<Chromosome> chromosomeIterator;
    private Long referenceId;
    private ReferenceManager referenceManager;

    private static final Logger LOG = LoggerFactory.getLogger(BamHelper.class);

    public ChromosomeReferenceSequence(final List<Chromosome> chromosomes, final Long referenceId,
            ReferenceManager referenceManager) {
        this.referenceId = referenceId;
        this.chromosomes = new LinkedHashSet<>(chromosomes);
        this.chromosomeIterator = this.chromosomes.iterator();
        this.chromosomeMap = chromosomes.stream()
                .collect(Collectors.toMap(Chromosome::getName, Function.identity()));
        this.dictionary = createDictionary(chromosomes);
        this.referenceManager = referenceManager;
    }

    private SAMSequenceDictionary createDictionary(List<Chromosome> chromosomes) {
        SAMSequenceDictionary newDictionary = new SAMSequenceDictionary();
        for (Chromosome chromosome : chromosomes) {
            newDictionary.addSequence(new SAMSequenceRecord(chromosome.getName(), chromosome.getSize()));
        }
        return newDictionary;
    }

    @Override
    public SAMSequenceDictionary getSequenceDictionary() {
        return dictionary;
    }

    @Override
    public ReferenceSequence nextSequence() {
        if(!chromosomeIterator.hasNext()) {
            return null;
        }
        return getSequence(chromosomeIterator.next());
    }

    @Override
    public void reset() {
        this.chromosomeIterator = this.chromosomes.iterator();
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public ReferenceSequence getSequence(String contig) {
        Chromosome chromosome = chromosomeMap.get(contig);
        if (chromosome == null) {
            chromosome = chromosomeMap.get(Utils.changeChromosomeName(contig));
        }
        return getSubsequenceAt(contig, 1, chromosome.getSize());
    }

    private ReferenceSequence getSequence(Chromosome chromosome) {
        return getSequence(chromosome.getName());
    }

    @Override
    public ReferenceSequence getSubsequenceAt(String contig, long start, long stop) {
        Chromosome chromosome = chromosomeMap.get(contig);
        if (chromosome == null) {
            chromosome = chromosomeMap.get(Utils.changeChromosomeName(contig));
        }
        try {
            byte[] sequenceBytes = referenceManager
                    .getSequenceByteArray((int) start, (int) stop, referenceId, chromosome.getName());
            return new ReferenceSequence(contig, chromosome.getId().intValue(), sequenceBytes);
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        //no op
    }
}
