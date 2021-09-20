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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexReverser {

    private static final String REGEX_QUANTIFIER_PATTERN = "([\\+\\*]\\?)|[\\+\\*\\?]|(\\{,?\\d+,?\\d*\\})";
    private static final char INVERTING_REGEXP_CHARACTER = '^';

    private RegexReverser() {
    }

    public static void reverseRegex(final StringBuilder regex) {
        moveInvertingRegexpCharacter(regex);
        moveQuantifierRegexpCharacter(regex);
        exchangeAllBrackets(regex);
        regex.reverse();
    }

    private static void moveInvertingRegexpCharacter(final StringBuilder regex) {
        int i = 0;
        while (i < regex.length()) {
            if (regex.charAt(i) == INVERTING_REGEXP_CHARACTER) {
                int newPosition = findPositionBeforeClosingBracket(regex, i);
                regex.insert(newPosition, INVERTING_REGEXP_CHARACTER);
                regex.deleteCharAt(i);
                i = newPosition;
            }
            i++;
        }
    }

    private static int findPositionBeforeClosingBracket(final StringBuilder regex, final int start) {
        int cnt = 0;
        for (int i = start; i < regex.length(); i++) {
            if (regex.charAt(i) == '[') {
                cnt++;
            } else  if (regex.charAt(i) == ']') {
                cnt--;
            }
            if (cnt < 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("\"^\" regexp character ocured in a wrong place!");
    }

    private static void moveQuantifierRegexpCharacter(final StringBuilder regex) {
        final Matcher matcher = Pattern.compile(REGEX_QUANTIFIER_PATTERN).matcher(regex);

        while (matcher.find()) {
            int newPosition = findPositionAfterOpeningBracket(regex, matcher.start());
            for (int i = matcher.start(); i < matcher.end(); i++) {
                regex.insert(newPosition, regex.charAt(i));
                regex.deleteCharAt(i + 1);
            }
        }
    }

    private static int findPositionAfterOpeningBracket(final StringBuilder regex, final int start) {
        int cnt = 0;
        for (int i = start - 1; i >= 0; i--) {
            if (regex.charAt(i) == ']') {
                cnt++;
            } else  if (regex.charAt(i) == '[') {
                cnt--;
            }
            if (cnt == 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("\"^\" regexp character ocured in a wrong place!");
    }

    private static void exchangeAllBrackets(final StringBuilder regex) {
        int i = 0;
        final Map<Character, Character> bracketMapper = new HashMap<>();
        bracketMapper.put('[', ']');
        bracketMapper.put(']', '[');
        bracketMapper.put('(', ')');
        bracketMapper.put(')', '(');
        while (i < regex.length()) {
            char currentLetter = regex.charAt(i);
            if (bracketMapper.containsKey(currentLetter)) {
                regex.deleteCharAt(i);
                regex.insert(i, bracketMapper.get(currentLetter));
            }
            i++;
        }
    }
}
