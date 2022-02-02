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

public final class IupacRegexConverter {

    private static final String REVERSIBLE_MOTIF_REGEX = "^[\\w\\[\\]\\(\\)\\|\\.]+$";
    private static final String PLAIN_MOTIF = "^[acgtACGT]+$";

    private IupacRegexConverter() {
    }

    public static boolean validateReversibleRegex(final String regex) {
        return regex.matches(REVERSIBLE_MOTIF_REGEX);
    }

    public static boolean validatePlainMotif(final String motif) {
        return motif.matches(PLAIN_MOTIF);
    }

    public static String combineIupacRegex(final String iupacRegex, final String posAlias, final String negAlias) {
        return "(?<" + posAlias + ">" + convertIupacToRegex(iupacRegex) + ")|(?<"
                + negAlias + ">"+ convertIupacToComplementReversedRegex(iupacRegex) + ")";
    }

    /**
     * Converts specified IUPAC regex to the plain nucleotide regex
     *
     * @param regex IUPAC nucleotide regex
     * @return plain nucleotide regex
     */
    public static String convertIupacToRegex(final String regex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regex.length(); i++) {
            result.append(IupacRegex.getRegexByIupacLetter(regex.substring(i, i + 1)));
        }
        return result.toString();
    }

    /**
     * Converts specified IUPAC regex to the reversed complement nucleotide regex
     *
     * @param regex IUPAC nucleotide regex
     * @return reversed complement nucleotide regex
     */
    public static String convertIupacToComplementReversedRegex(final String regex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < regex.length(); i++) {
            result.append(IupacRegex.getComplementRegexByIupacLetter(regex.substring(i, i + 1)));
        }
        RegexReverser.reverseRegex(result);
        return result.toString();
    }
}
