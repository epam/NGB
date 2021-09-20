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
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class MotifSearcher {

    private MotifSearcher() {
    }

    public static List<Motif> search(final byte[] seq, final String regex,
                                     final String contig, final int start, final boolean includeSequence) {
        return search(seq, regex, null, contig, start, includeSequence);
    }

    public static List<Motif> search(final byte[] seq, final String regex,
                                     final StrandSerializable strand, final String contig,
                                     final int start, final boolean includeSequence) {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(
                        getIterator(seq, regex, strand, contig, start, includeSequence),
                        Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    public static Iterator<Motif> getIterator(final byte[] seq, final String regex,
                                              final StrandSerializable strand, final String contig,
                                              final int start, final boolean includeSequence) {
        if (IupacRegexConverter.validatePlainMotif(regex)) {
            return new SimpleMotifSearchIterator(seq, regex, strand, contig, start, includeSequence);
        } else if (strand == null && IupacRegexConverter.validateReversibleRegex(regex)) {
            return new ReversingRegexMotifSearchIterator(seq, regex, contig, start, includeSequence);
        } else {
            return new MotifSearchIterator(seq, regex, strand, contig, start, includeSequence);
        }
    }
}
