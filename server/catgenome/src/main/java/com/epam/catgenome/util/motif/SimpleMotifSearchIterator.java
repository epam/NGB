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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

public class SimpleMotifSearchIterator implements Iterator<Motif> {

    private static final int POSITIVE = 1;
    private static final int NEGATIVE = 2;
    private static final int BOTH = 3;
    private static final int MISMATCH = 0;

    private final byte[] positiveRegex;
    private final byte[] positiveRegexUppercase;
    private final byte[] negativeRegex;
    private final byte[] negativeRegexUppercase;
    private final byte[] sequence;
    private int currentPosition;
    private int matchType;
    private final String contig;
    private final int offset;
    private final boolean includeSequence;
    private final boolean requiresPositive;
    private final boolean requiresNegative;

    public SimpleMotifSearchIterator(final byte[] seq, final String motif,  final StrandSerializable strand,
                                     final String contig, final int start, final boolean includeSequence) {
        positiveRegex = motif.toLowerCase(Locale.US).getBytes(StandardCharsets.UTF_8);
        positiveRegexUppercase = motif.toUpperCase(Locale.US).getBytes(StandardCharsets.UTF_8);
        negativeRegex = reverseComplementAddToLowerCase(motif).getBytes(StandardCharsets.UTF_8);
        negativeRegexUppercase =
                reverseComplementAddToLowerCase(motif).toUpperCase(Locale.US).getBytes(StandardCharsets.UTF_8);
        sequence = seq;
        this.contig = contig;
        this.offset = start;
        this.includeSequence = includeSequence;
        matchType = MISMATCH;
        requiresPositive = strand != StrandSerializable.NEGATIVE;
        requiresNegative = strand != StrandSerializable.POSITIVE;
    }

    @Override
    public boolean hasNext() {
        boolean positiveMatch;
        boolean negativeMatch;
        int sequenceMatchingPosition;
        final int lastSearchablePosition = sequence.length - positiveRegex.length;
        if (matchType != MISMATCH && currentPosition <= lastSearchablePosition) {
            return true;
        }
        while (currentPosition <= lastSearchablePosition) {
            positiveMatch = requiresPositive;
            negativeMatch = requiresNegative;
            for (int i = 0; i < positiveRegex.length; i++) {
                sequenceMatchingPosition = currentPosition + i;
                if (positiveMatch) {
                    positiveMatch = (sequence[sequenceMatchingPosition] == positiveRegex[i]
                            || sequence[sequenceMatchingPosition] == positiveRegexUppercase[i]);
                }
                if (negativeMatch) {
                    negativeMatch = (sequence[sequenceMatchingPosition] == negativeRegex[i]
                            || sequence[sequenceMatchingPosition] == negativeRegexUppercase[i]);
                }
                if (!positiveMatch && !negativeMatch) {
                    break;
                }
            }
            if (positiveMatch && negativeMatch) {
                matchType = BOTH;
                return true;
            }
            if (positiveMatch) {
                matchType = POSITIVE;
                return true;
            }
            if (negativeMatch) {
                matchType = NEGATIVE;
                return true;
            }
            currentPosition++;
        }
        return false;
    }

    @Override
    public Motif next() {
        final int startPosition = currentPosition + offset;
        final int endPosition = startPosition + positiveRegex.length - 1;
        final StrandSerializable strand;
        switch (matchType) {
            case BOTH:
                strand = StrandSerializable.POSITIVE;
                matchType = NEGATIVE;
                break;
            case POSITIVE:
                strand = StrandSerializable.POSITIVE;
                matchType = MISMATCH;
                currentPosition++;
                break;
            case NEGATIVE:
                strand = StrandSerializable.NEGATIVE;
                matchType = MISMATCH;
                currentPosition++;
                break;
            default:
                throw new NoSuchElementException("There is not next element!");
        }
        return new Motif(contig, startPosition, endPosition, strand,
                includeSequence ? new String(Arrays.copyOfRange(
                        sequence, startPosition - offset, endPosition - offset + 1)) : null, null, null);
    }

    private String reverseComplementAddToLowerCase(final String motif){
        StringBuilder result = new StringBuilder(motif.toLowerCase(Locale.US));
        result.reverse();
        for (int i = 0; i < result.length(); i++) {
            switch (result.charAt(i)) {
                case 'a':
                    result.setCharAt(i, 't');
                    break;
                case 't':
                    result.setCharAt(i, 'a');
                    break;
                case 'c':
                    result.setCharAt(i, 'g');
                    break;
                case 'g':
                    result.setCharAt(i, 'c');
                    break;
                default:
                    throw new IllegalArgumentException("Letter \"" + result.charAt(i) + "\" not matches [acgt] regex!");
            }
        }
        return result.toString();
    }
}
