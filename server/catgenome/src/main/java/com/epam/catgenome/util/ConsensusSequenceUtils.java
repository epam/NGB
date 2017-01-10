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

package com.epam.catgenome.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epam.catgenome.entity.NucleotideContent;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import htsjdk.samtools.SAMRecord;

/**
 * Created: 6/3/2016
 * Project: catgenome
 * <p>
 * Provides methods to calculate consensus sequence for BAM file.
 * </p>
 */
public final class ConsensusSequenceUtils {
    private static final List<String> NUCLEOTIDE_PAIRS = Arrays.asList("AT", "AG", "AC", "TA", "TG", "TC", "GA", "GT",
            "GC", "CA", "CT", "CG");
    private static final double CO_CONSENSUS_CRITERIA = 0.75;
    private static final double CONSENSUS_CRITERIA = 0.5;

    /**
     * There are only 4 nucleotides; A, T, C, G
     */
    private static final int NUCLEOTIDES_COUNT = 4;

    private ConsensusSequenceUtils() {
        // no-op
    }

    /**
     * Calculates consensus sequence
     * @param track a sequence track
     * @param blockToReadsMap map of reads per nucleotide
     */
    public static void calculateConsensusSequence(final Track<Sequence> track,
                                                  final Map<Sequence, List<SAMRecord>> blockToReadsMap) {
        // For each range calculate consensus nucleotide.
        for (Sequence sequence : track.getBlocks()) {
            final List<SAMRecord> blockReads = blockToReadsMap.get(sequence);
            final Map<String, Integer> atgcMap = new HashMap<>();
            final NucleotideContent nucleotideContent = new NucleotideContent();


            // Calculate frequencies.
            if (blockReads == null || blockReads.isEmpty()) {
                continue;
            }
            nucleotideContent.setTotalNumberOfNucleotides(calculateNucleotideFrequencies(blockReads, atgcMap,
                    nucleotideContent));

            String consensusNucleotide = checkFirstCriteria(atgcMap, nucleotideContent.getTotalNumberOfNucleotides());
            if (consensusNucleotide == null) {
                consensusNucleotide = checkSecondCriteria(atgcMap, nucleotideContent.getTotalNumberOfNucleotides());
            }

            // Otherwise
            if (consensusNucleotide == null) {
                consensusNucleotide = "N";
            }

            sequence.setText(consensusNucleotide);
        }
    }

    private static Integer calculateNucleotideFrequencies(final List<SAMRecord> blockReads,
                                                          final Map<String, Integer> atgcMap,
                                                          final NucleotideContent nucleotideContent) {
        for (SAMRecord read : blockReads) {
            char[] nucleotides = read.getReadString().toCharArray();
            if (nucleotides.length != 0) {
                nucleotideContent.incTotalNumberOfNucleotides(nucleotides.length);
                calculateCount(nucleotides, nucleotideContent);
            }
        }
        atgcMap.put("A", nucleotideContent.getACount());
        atgcMap.put("T", nucleotideContent.getTCount());
        atgcMap.put("G", nucleotideContent.getGCount());
        atgcMap.put("C", nucleotideContent.getCCount());
        return nucleotideContent.getTotalNumberOfNucleotides();
    }


    private static void calculateCount(final char[] nucleotides, final NucleotideContent nucleotideContent) {
        for (char nucleotide : nucleotides) {
            switch (Character.toUpperCase(nucleotide)) {
                case 'A':
                    nucleotideContent.incACount();
                    break;
                case 'T':
                    nucleotideContent.incTCount();
                    break;
                case 'G':
                    nucleotideContent.incGCount();
                    break;
                case 'C':
                    nucleotideContent.incCCount();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown nucleotide in the sequence: " + nucleotide);
            }
        }

    }

    private static String checkSecondCriteria(Map<String, Integer> atgcMap, double totalNumberOfNucleotides) {
        for (String nucleotidePair : NUCLEOTIDE_PAIRS) {
            Integer firstCnt = atgcMap.get(String.valueOf(nucleotidePair.charAt(0)));
            Integer secondCnt = atgcMap.get(String.valueOf(nucleotidePair.charAt(1)));
            double frequency = (double) (firstCnt + secondCnt) / totalNumberOfNucleotides;
            if (Double.compare(frequency, CO_CONSENSUS_CRITERIA) > 0) {
                return "[" + nucleotidePair + "]";
            }
        }

        return null;
    }

    private static String checkFirstCriteria(Map<String, Integer> atgcMap, double totalNumberOfNucleotides) {
        String consensusNucleotide = null;
        Integer[] values = atgcMap.values().toArray(new Integer[NUCLEOTIDES_COUNT]);
        Arrays.sort(values);
        for (Map.Entry<String, Integer> nucleotide : atgcMap.entrySet()) {
            Integer cnt = nucleotide.getValue();
            if (cnt.equals(values[values.length - 1])) { // most frequent nucleotide
                double frequency = (double) cnt / totalNumberOfNucleotides;
                if (Double.compare(frequency, CONSENSUS_CRITERIA) > 0 && cnt > 2 * values[2]) {
                    consensusNucleotide = nucleotide.getKey();
                }
            }
        }
        return consensusNucleotide;
    }

}
