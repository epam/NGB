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

import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@code AbstractResultPrinter} provides methods for printing objects in several formats.
 * Two formats are supported for now: Json and Table
 */
public abstract class AbstractResultPrinter {

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("dd-MM-yyyy HH:mm:ss");

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResultPrinter.class);

    /**
     * Factory method for creating a printer according to options.
     * @param printTable if true, {@code TablePrinter} will be created, otherwise - {@code JsonPrinter}
     * @param format String to apply to the printed items
     * @return {@code AbstractResultPrinter} with required type
     */
    public static AbstractResultPrinter getPrinter(boolean printTable, String format) {
        if (printTable) {
            return new TablePrinter(format);
        } else {
            return new JsonPrinter();
        }
    }

    /**
     * Prints the header of the output
     * @param item
     */
    public abstract void printHeader(Printable item);

    /**
     * Prints an {@param item}
     * @param item
     */
    public abstract void printItem(Printable item);

    public void printSimple(String str) {
        LOGGER.info(str);
    }

    private static final class TablePrinter extends AbstractResultPrinter {

        private String format;

        private TablePrinter(String format) {
            this.format = format;
        }

        @Override public void printHeader(Printable item) {
            LOGGER.info(item.formatHeader(format));
        }

        @Override public void printItem(Printable item) {
            LOGGER.info(item.formatItem(format));
        }
    }

    private static class JsonPrinter extends AbstractResultPrinter {

        private ObjectMapper mapper = JsonMapper.getMapper();

        @Override public void printHeader(Printable item) {
            // no header for Json format
        }

        @Override public void printItem(Printable item) {
            try {
                LOGGER.info(mapper.writeValueAsString(item));
            } catch (JsonProcessingException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }
    }
}
