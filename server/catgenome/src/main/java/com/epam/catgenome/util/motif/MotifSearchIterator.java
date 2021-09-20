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

package com.epam.catgenome.util.motif;

import com.epam.catgenome.entity.reference.motif.Motif;
import com.epam.catgenome.manager.gene.parser.StrandSerializable;
import lombok.Value;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MotifSearchIterator implements Iterator<Motif> {
    private static final byte CAPITAL_A = 'A';
    private static final byte CAPITAL_C = 'C';
    private static final byte CAPITAL_G = 'G';
    private static final byte CAPITAL_T = 'T';
    private static final byte CAPITAL_N = 'N';
    private static final byte LOWERCASE_A = 'a';
    private static final byte LOWERCASE_C = 'c';
    private static final byte LOWERCASE_G = 'g';
    private static final byte LOWERCASE_T = 't';
    private static final byte LOWERCASE_N = 'n';

    private final Deque<Match> positiveMatches;
    private final Deque<Match> negativeMatches;
    private final String contig;
    private final byte[] sequence;
    private final int offset;
    private final boolean includeSequence;


    public MotifSearchIterator(final byte[] seq, final String iupacRegex,
                               final StrandSerializable strand, final String contig,
                               final int start, final boolean includeSequence) {
        if (strand != null && strand != StrandSerializable.POSITIVE && strand != StrandSerializable.NEGATIVE) {
            throw new IllegalStateException("Not supported strand: " + strand);
        }
        this.contig = contig;
        this.sequence = seq;
        this.offset = start;
        this.includeSequence = includeSequence;

        final Pattern pattern =
                Pattern.compile(IupacRegexConverter.convertIupacToRegex(iupacRegex), Pattern.CASE_INSENSITIVE);
        if (strand == null) {
            this.positiveMatches = populatePositiveMatches(pattern.matcher(new String(seq)));
            this.negativeMatches = populateNegativeMatches(pattern.matcher(reverseAndComplement(seq)), seq.length);
        } else if (strand == StrandSerializable.POSITIVE) {
            this.positiveMatches = populatePositiveMatches(pattern.matcher(new String(seq)));
            this.negativeMatches = new LinkedList<>();
        } else {
            this.positiveMatches = new LinkedList<>();
            this.negativeMatches = populateNegativeMatches(pattern.matcher(reverseAndComplement(seq)), seq.length);
        }
    }

    private Deque<Match> populatePositiveMatches(final Matcher matcher) {
        int position = 0;
        LinkedList<Match> matches = new LinkedList<>();
        while (matcher.find(position)) {
            matches.add(new Match(matcher.start(), matcher.end() - 1));
            position = matcher.start() + 1;
        }
        return matches;
    }

    private Deque<Match> populateNegativeMatches(final Matcher matcher, final int seqLength) {
        int position = 0;
        LinkedList<Match> matches = new LinkedList<>();
        while (matcher.find(position)) {
            matches.add(new Match(seqLength - matcher.end(), seqLength - matcher.start() -1));
            position = matcher.start() + 1;
        }
        return matches;
    }

    @Override
    public boolean hasNext() {
        return !positiveMatches.isEmpty() || !negativeMatches.isEmpty();
    }

    @Override
    public Motif next() {
        Match match;
        StrandSerializable currentStrand;
        if (negativeMatches.isEmpty()) {
            match = positiveMatches.removeFirst();
            currentStrand = StrandSerializable.POSITIVE;
        } else if (positiveMatches.isEmpty() || negativeMatches.peekLast().start < positiveMatches.peekFirst().start) {
            match = negativeMatches.removeLast();
            currentStrand = StrandSerializable.NEGATIVE;
        } else {
            match = positiveMatches.removeFirst();
            currentStrand = StrandSerializable.POSITIVE;
        }
        if (includeSequence) {
            return getMotif(contig, match.start, match.end, currentStrand);
        }
        return new Motif(contig, match.start + offset, match.end + offset, currentStrand, null);
    }

    private Motif getMotif(final String contig, final int start, final int end,  StrandSerializable strand) {
        final StringBuilder result = new StringBuilder();
        for (int i = start; i <= end; i++) {
            result.append((char) sequence[i]);
        }
        return new Motif(contig, start + offset, end + offset, strand, result.toString());
    }

    /**
     * Provides reverse complement operation on sequence string
     *
     * @param sequence nucleotide sequence
     * @return reversed complement nucleotide sequence string
     */
    private static String reverseAndComplement(final byte[] sequence) {
        final byte[] reversedSequence = new byte[sequence.length];
        for (int i = 0, j = sequence.length - 1; i < sequence.length; i++, j--) {
            reversedSequence[i] = complement(sequence[j]);
        }
        return new String(reversedSequence);
    }

    /**
     * Converts specified nucleotide to complement one.
     *
     * @param nucleotide nucleotide
     * @return complement nucleotide
     */
    public static byte complement(final byte nucleotide) {
        switch (nucleotide) {
            case CAPITAL_A:
            case LOWERCASE_A:
                return CAPITAL_T;
            case CAPITAL_C:
            case LOWERCASE_C:
                return CAPITAL_G;
            case CAPITAL_G:
            case LOWERCASE_G:
                return CAPITAL_C;
            case CAPITAL_T:
            case LOWERCASE_T:
                return CAPITAL_A;
            case CAPITAL_N:
            case LOWERCASE_N:
                return CAPITAL_N;
            default:
                throw new IllegalStateException("Not supported nucleotide: " + (char)nucleotide);
        }
    }

    @Value
    public static class Match {
        Integer start;
        Integer end;
    }
}
