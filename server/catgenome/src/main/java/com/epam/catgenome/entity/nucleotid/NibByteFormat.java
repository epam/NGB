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

package com.epam.catgenome.entity.nucleotid;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

import com.epam.catgenome.constant.Constants;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.reference.Sequence;

/**
 * Represents the constants corresponding to the *.nib sequence file format. This format is a compressed
 * representation of nucleotide sequnces consisting of the following characters: A, C, G, T, a, c, g, t, N.
 * Describes the characters, the char-code this character(ASCII) and the Nib code corresponding to the character
 */
public enum NibByteFormat {

    NUCLEOTIDE_UPPERCASE_C('C', (byte) 1, (byte) 67),
    NUCLEOTIDE_UPPERCASE_A('A', (byte) 2, (byte) 65),
    NUCLEOTIDE_UPPERCASE_T('T', (byte) 0, (byte) 84),
    NUCLEOTIDE_UPPERCASE_G('G', (byte) 3, (byte) 71),
    NUCLEOTIDE_UPPERCASE_N('N', (byte) 4, (byte) 78),
    NUCLEOTIDE_LOWERCASE_C('c', (byte) 9, (byte) 99),
    NUCLEOTIDE_LOWERCASE_A('a', (byte) 10, (byte) 97),
    NUCLEOTIDE_LOWERCASE_T('t', (byte) 8, (byte) 116),
    NUCLEOTIDE_LOWERCASE_G('g', (byte) 11, (byte) 103),
    NUCLEOTIDE_LOWERCASE_N('n', (byte) 12, (byte) 110),
    NUCLEOTIDE_LOWERCASE_DASH('-', (byte) 12, (byte) 45),
    NUCLEOTIDE_LOWERCASE_QUESTION_MARK('?', (byte) 12, (byte) 63);

    private static String messageErrorUnknownNibCode = getMessage(MessagesConstants.ERROR_UNKNOWN_NIB_CODE);

    public static final int LOW_TO_HIGH_NIB_CODE_SHIFT = 4;
    private static final int NUCLEOTIDES_CODE_LENGTH = 10;
    private static final int NIB_C_UPPER_INDEX = 0;
    private static final int NIB_A_UPPER_INDEX = 1;
    private static final int NIB_T_UPPER_INDEX = 2;
    private static final int NIB_G_UPPER_INDEX = 3;
    private static final int NIB_N_UPPER_INDEX = 4;
    private static final int NIB_C_LOWER_INDEX = 5;
    private static final int NIB_A_LOWER_INDEX = 6;
    private static final int NIB_T_LOWER_INDEX = 7;
    private static final int NIB_G_LOWER_INDEX = 8;
    private static final int NIB_N_LOWER_INDEX = 9;

    private static final int[] NUCLEOTIDES_CHAR_2_NIB_CODE = new int[Constants.CODING_ARRAY_LENGTH];
    private static final byte[] NUCLEOTIDES_NIB_CODE_2_CHAR = new byte[Constants.CODING_ARRAY_LENGTH];
    private static final int[] NIB_CODE_LOW = new int[NUCLEOTIDES_CODE_LENGTH];
    private static final int[] NIB_CODE_HIGH = new int[NUCLEOTIDES_CODE_LENGTH];

    //for Nib-file
    static {
        NIB_CODE_LOW[NIB_C_UPPER_INDEX] = NUCLEOTIDE_UPPERCASE_C.getByteCode();
        NIB_CODE_LOW[NIB_A_UPPER_INDEX] = NUCLEOTIDE_UPPERCASE_A.getByteCode();
        NIB_CODE_LOW[NIB_T_UPPER_INDEX] = NUCLEOTIDE_UPPERCASE_T.getByteCode();
        NIB_CODE_LOW[NIB_G_UPPER_INDEX] = NUCLEOTIDE_UPPERCASE_G.getByteCode();
        NIB_CODE_LOW[NIB_N_UPPER_INDEX] = NUCLEOTIDE_UPPERCASE_N.getByteCode();
        NIB_CODE_LOW[NIB_C_LOWER_INDEX] = NUCLEOTIDE_LOWERCASE_C.getByteCode();
        NIB_CODE_LOW[NIB_A_LOWER_INDEX] = NUCLEOTIDE_LOWERCASE_A.getByteCode();
        NIB_CODE_LOW[NIB_T_LOWER_INDEX] = NUCLEOTIDE_LOWERCASE_T.getByteCode();
        NIB_CODE_LOW[NIB_G_LOWER_INDEX] = NUCLEOTIDE_LOWERCASE_G.getByteCode();
        NIB_CODE_LOW[NIB_N_LOWER_INDEX] = NUCLEOTIDE_LOWERCASE_N.getByteCode();

        for (int i = 0; i < NIB_CODE_LOW.length; i++) {
            NIB_CODE_HIGH[i] = NIB_CODE_LOW[i] << LOW_TO_HIGH_NIB_CODE_SHIFT;
        }
        Arrays.fill(NUCLEOTIDES_CHAR_2_NIB_CODE, -1);
        Arrays.fill(NUCLEOTIDES_NIB_CODE_2_CHAR, (byte)-1);

        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_UPPERCASE_C.getCharCode()] = NIB_C_UPPER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_UPPERCASE_A.getCharCode()] = NIB_A_UPPER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_UPPERCASE_T.getCharCode()] = NIB_T_UPPER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_UPPERCASE_G.getCharCode()] = NIB_G_UPPER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_UPPERCASE_N.getCharCode()] = NIB_N_UPPER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_LOWERCASE_C.getCharCode()] = NIB_C_LOWER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_LOWERCASE_A.getCharCode()] = NIB_A_LOWER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_LOWERCASE_T.getCharCode()] = NIB_T_LOWER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_LOWERCASE_G.getCharCode()] = NIB_G_LOWER_INDEX;
        NUCLEOTIDES_CHAR_2_NIB_CODE[NUCLEOTIDE_LOWERCASE_N.getCharCode()] = NIB_N_LOWER_INDEX;

        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_UPPERCASE_C.getByteCode()] = NUCLEOTIDE_UPPERCASE_C.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_UPPERCASE_A.getByteCode()] = NUCLEOTIDE_UPPERCASE_A.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_UPPERCASE_T.getByteCode()] = NUCLEOTIDE_UPPERCASE_T.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_UPPERCASE_G.getByteCode()] = NUCLEOTIDE_UPPERCASE_G.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_UPPERCASE_N.getByteCode()] = NUCLEOTIDE_UPPERCASE_N.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_LOWERCASE_C.getByteCode()] = NUCLEOTIDE_LOWERCASE_C.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_LOWERCASE_A.getByteCode()] = NUCLEOTIDE_LOWERCASE_A.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_LOWERCASE_T.getByteCode()] = NUCLEOTIDE_LOWERCASE_T.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_LOWERCASE_G.getByteCode()] = NUCLEOTIDE_LOWERCASE_G.getCharCode();
        NUCLEOTIDES_NIB_CODE_2_CHAR[NUCLEOTIDE_LOWERCASE_N.getByteCode()] = NUCLEOTIDE_LOWERCASE_N.getCharCode();
    }

    private final char nucleobase;

    private final byte byteCode;

    private final byte charCode;

    NibByteFormat(final char nucleobase, byte nucleobaseCode, byte charCode) {
        this.nucleobase = nucleobase;
        this.byteCode = nucleobaseCode;
        this.charCode = charCode;
    }

    /**
     * Returns a byte value of the position and code nucleotide at nib-format
     *
     * @param firstByte  {@code byte} first code of nucleotide(ASCII)
     * @param secondByte {@code byte} second code of nucleotide(ASCII)
     */
    public static int fastaByteFormatToByteNibFormat(final byte firstByte, final byte secondByte) {
        return NIB_CODE_HIGH[getNibCode(firstByte)] + NIB_CODE_LOW[getNibCode(secondByte)];
    }

    /**
     * Returns a byte value of the position and code nucleotide at nib-format (for the last byte at Fasta-file)
     *
     * @param firstByte {@code byte} first code of nucleotide(ASCII)
     */
    public static int fastaByteFormatToByteNibFormat(final byte firstByte) {
        return NIB_CODE_HIGH[getNibCode(firstByte)];
    }

    /**
     * Returns the int value of the character, corresponding to the input nib code
     * @param code in the nib format
     * @return corresponding character value
     */
    public static int getNibCode(final int code) {
        final int charCode = NUCLEOTIDES_CHAR_2_NIB_CODE[code];
        if (charCode == -1) {
            throw new IllegalArgumentException(getMessage(MessagesConstants.ERROR_UNKNOWN_NIB_CODE, code));
        }
        return charCode;
    }

    /**
     * Returns an array of char, consist of nucleotides from nibArray
     *
     * @param startPosition  {@code int} start position at nibArray
     * @param sequenceLength {@code int} length nucleotides to need
     * @param nibArray       {@code byte[]} array reference if nib-code
     * @return {@code char[]} array of char, consist of nucleotides from nibArray
     */
    public static List<Sequence> nibByteArrayToNucleotidesList(final int absolutePosition, final int startPosition,
                                                         final int sequenceLength, final byte[] nibArray) {
        Assert.notNull(nibArray, getMessage(MessagesConstants.ERROR_NO_SUCH_FILE));
        Assert.isTrue(sequenceLength <= nibArray.length * 2, getMessage(MessagesConstants.ERROR_ARRAY_SO_SMALL));
        int nibCode;
        List<Sequence> result = new ArrayList<>();
        for (int i = 0; i < sequenceLength; i++) {
            if ((startPosition + i) % 2 == 0) {
                nibCode = nibArray[(startPosition + i) >>> 1] & Constants.MASK_HIGH_BYTE;
                nibCode = nibCode >> LOW_TO_HIGH_NIB_CODE_SHIFT;
            } else {
                nibCode = nibArray[(startPosition + i) >>> 1] & Constants.MASK_LOW_BYTE;
            }
            // i + absolutePosition + 1 - because at chromosome started at first index but ib array started from zero
            result.add(new Sequence(i + absolutePosition + 1, FormatCoder.nibByteToString(nibCode)));
        }
        return result;
    }

    public static byte nibCodeToByteNucleotide(final int nibCode) {
        Assert.isTrue(nibCode >= 0 && nibCode < Constants.CODING_ARRAY_LENGTH, messageErrorUnknownNibCode);
        byte charCode = NUCLEOTIDES_NIB_CODE_2_CHAR[nibCode];
        Assert.isTrue(charCode != -1, messageErrorUnknownNibCode);
        return charCode;
    }

    public final byte getByteCode() {
        return byteCode;
    }

    public final char getNucleobase() {
        return nucleobase;
    }

    public final byte getCharCode() {
        return charCode;
    }


}
