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

package com.epam.ngb.cli.manager.command.handler.http;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;

/**
 * {@code {@link SearchHandler}} represents a tool handling "search" command and
 * sending request to NGB server for file search registration. This command requires strictly one argument:
 * search query. Search is performed in file name in two ways, handled by option 'strict'.
 * If it is set to true value, a strict equality search is performed, otherwise a substring, case-insensitive
 * search is done.
 * Command output format can be managed by the options, default output format is json,
 * but it can be switched to table by setting {@code true} value to {@code printTable} field.
 */
@Command(type = Command.Type.REQUEST, command = {"search"})
public class SearchHandler extends AbstractHTTPCommandHandler {

    private String query;
    private boolean strict;
    private boolean printTable;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchHandler.class);

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        query = arguments.get(0);
        strict = options.isStrictSearch();
        printTable = options.isPrintTable();
    }

    @Override public int runCommand() {
        List<BiologicalDataItem> items = loadItemsByName(query, strict);
        if (items == null || items.isEmpty()) {
            LOGGER.info("No files found matching a request \"" + query + "\".");
            return 0;
        }
        AbstractResultPrinter printer = AbstractResultPrinter.getPrinter(printTable,
                items.get(0).getFormatString(items));
        printer.printHeader(items.get(0));
        items.forEach(printer::printItem);
        return 0;
    }
}
