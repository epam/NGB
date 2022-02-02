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

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReversingRegexMotifSearchIterator implements Iterator<Motif> {

    private final Matcher positiveMatcher;
    private final Matcher negativeMatcher;
    private int currentStartPositionPos;
    private int currentStartPositionNeg;
    private final String contig;
    private final int offset;
    private final boolean includeSequence;

    private boolean posFlag;
    private boolean negFlag;

    public ReversingRegexMotifSearchIterator(final byte[] seq, final String iupacRegex, final String contig,
                                             final int start, final boolean includeSequence) {

        final String sequence = new String(seq);
        positiveMatcher = Pattern.compile(IupacRegexConverter.convertIupacToRegex(iupacRegex), Pattern.CASE_INSENSITIVE)
                .matcher(sequence);
        negativeMatcher = Pattern.compile(
                        IupacRegexConverter.convertIupacToComplementReversedRegex(iupacRegex),
                        Pattern.CASE_INSENSITIVE)
                .matcher(sequence);
        this.contig = contig;
        this.offset = start;
        this.includeSequence = includeSequence;
    }

    @Override
    public boolean hasNext() {
        if (!posFlag) {
            posFlag = positiveMatcher.find(currentStartPositionPos);
        }
        if (!negFlag) {
            negFlag = negativeMatcher.find(currentStartPositionNeg);
        }
        return negFlag || posFlag;
    }

    @Override
    public Motif next() {
        if (!negFlag || posFlag && positiveMatcher.start() <= negativeMatcher.start()) {
            currentStartPositionPos = positiveMatcher.start() + 1;
            posFlag = false;
            return new Motif(contig, positiveMatcher.start() + offset,
                    positiveMatcher.end() - 1 + offset, StrandSerializable.POSITIVE,
                    includeSequence ? positiveMatcher.group() : null);
        }
        currentStartPositionNeg = negativeMatcher.start() + 1;
        negFlag = false;
        return new Motif(contig, negativeMatcher.start() + offset,
                negativeMatcher.end() - 1 + offset, StrandSerializable.NEGATIVE,
                includeSequence ? negativeMatcher.group() : null);
    }
}
