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

package com.epam.ngb.cli.manager.printer;

import java.util.List;

/**
 * {@code Printable} is an interface for classes, that support printing in a table view.
 * @param <T> type of printed objects
 */
public interface Printable<T> {

    /**
     * Calculates a format string from a {@code List} of objects
     * @param table table {@code List} of objects for formatting
     * @return format String to be applied to the {@param table} printing
     */
    String getFormatString(List<T> table);

    /**
     * Creates a String representation of an object according to {@param format}
     * @param format to apply to object
     * @return a String representation of an object
     */
    String formatItem(String format);

    /**
     * Creates a String representation of table header
     * @param format to apply to header
     * @return a String representation of table header
     */
    String formatHeader(String format);
}
