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

import java.util.Arrays;
import java.util.Locale;

/**
 * IUPAC Ambiguity codes translated into regex, using java syntax
 * Source: http://www.chem.qmul.ac.uk/iubmb/misc/naseq.html
 */
enum IupacRegex {

    G("g", "c"),
    A("a", "t"),
    T("t", "a"),
    C("c", "g"),
    R("[rga]", "[yct]"),
    Y("[ytc]", "[rag]"),
    M("[mac]", "[ktg]"),
    K("[kgt]", "[mca]"),
    S("[sgc]", "[sgc]"),
    W("[wat]", "[wat]"),
    H("[hact]", "[dtga]"),
    B("[bgtc]", "[vcag]"),
    V("[vgca]", "[bcgt]"),
    D("[dgat]", "[hcta]"),
    N(".", ".");

    private final String regex;
    private final String complementRegex;

    IupacRegex(final String regex, final String complementRegex) {
        this.regex = regex;
        this.complementRegex = complementRegex;
    }

    static String getRegexByIupacLetter(final String letter) {
        return Arrays.stream(values())
                .filter(v -> v.toString().equalsIgnoreCase(letter))
                .findFirst()
                .map(v -> v.regex)
                .orElseGet(() -> letter.toLowerCase(Locale.US));
    }

    static String getComplementRegexByIupacLetter(final String letter) {
        return Arrays.stream(values())
                .filter(v -> v.toString().equalsIgnoreCase(letter))
                .findFirst()
                .map(v -> v.complementRegex)
                .orElseGet(() -> letter.toLowerCase(Locale.US));

    }
}
