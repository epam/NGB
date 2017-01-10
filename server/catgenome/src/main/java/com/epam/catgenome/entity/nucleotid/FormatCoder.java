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

import org.springframework.util.Assert;

import com.epam.catgenome.constant.MessagesConstants;

/**
 * Util class for converting a byte nucleotide code to a String
 */
public final class FormatCoder {

    private static String messageCharCodeAboveZero = getMessage(MessagesConstants.ERROR_CHAR_CODE_ABOVE_ZERO);
    private static String messageErrorUnknownNibCode = getMessage(MessagesConstants.ERROR_UNKNOWN_NIB_CODE);
    private static String messageErrorInvalidResult = getMessage(MessagesConstants.ERROR_INVALID_RESULT);
    private FormatCoder() {
        //No-op
    }

    /**
     * Converts a byte nucleotide code into a {@code String} representation
     * @param byteCode for conversion
     * @return {@code String} representation of the nucleotide
     */
    public static String nibByteToString(final int byteCode) {
        Assert.isTrue(byteCode >= 0, messageCharCodeAboveZero);
        Assert.isTrue(byteCode <= NibByteFormat.NUCLEOTIDE_LOWERCASE_N.getByteCode(),
                messageErrorUnknownNibCode);
        char result = 0;
        if (byteCode == NibByteFormat.NUCLEOTIDE_LOWERCASE_N.getByteCode()) {
            result = NibByteFormat.NUCLEOTIDE_LOWERCASE_N.getNucleobase();
        } else {
            for (NibByteFormat nibFormat : NibByteFormat.values()) {
                if (byteCode == nibFormat.getByteCode()) {
                    result = nibFormat.getNucleobase();
                    break;
                }
            }
        }
        Assert.isTrue(result != 0, messageErrorInvalidResult);
        return String.valueOf(result);
    }
}
