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

package com.epam.ngb.cli.constants;

/**
 * Helper class for defining the logging and exception messages and formatting them.
 */
public final class MessageConstants {

    //TODO: move messages to external file
    private static final String ILLEGAL_ARGUMENTS = "Wrong number of arguments for a [%s] command.";
    public static final String ILLEGAL_COMMAND_ARGUMENTS = ILLEGAL_ARGUMENTS
            + " Expected [%d] argument(s), received [%d] argument(s).";
    public static final String MINIMUM_COMMAND_ARGUMENTS = ILLEGAL_ARGUMENTS
            + " Expected at least [%d] argument(s), received [%d] argument(s).";

    public static final String ERROR_INDEX_REQUIRED = "Index file is required for file [%s].";
    public static final String ERROR_UNSUPPORTED_FORMAT = "Unsupported file format [%s].";
    public static final String ERROR_INDEX_FORMAT_DOES_NOT_MATCH =
            "Index file [%s] doesn't match file format [%s].";
    public static final String ERROR_UNSUPPORTED_ZIP = "GZip compressed files are not "
            + "supported for format [%s].";
    public static final String ERROR_REFERENCE_NOT_FOUND = "Failed to find a reference by name: [%s].";
    public static final String ILLEGAL_PATH_FORMAT = "Malformed file path provided [%s]. "
            + "Only one '\\?' symbol is supported in the path for file and it's index separation.";
    public static final String ERROR_MISSING_COMMAND = "Command is not specified.";
    public static final String ERROR_UNKNOWN_COMMAND = "Unknown command [%s].";
    public static final String ERROR_PARSE_COMMAND_LINE = "Failed to parse command line [ngb %s].";
    public static final String ERROR_LOAD_CONFIG_FILE = "Failed to load configuration from file %s.";
    public static final String ERROR_COMMAND_HANDLER_NOT_FOUND = "Failed to load a handler for command %s.";
    public static final String ERROR_LOAD_HANDLER_CLASS = "Failed to load a handler class %s.";
    public static final String SEVERAL_RESULTS_FOR_QUERY =  "Found several results for a query \"%s\".";
    public static final String ERROR_FILE_NOT_FOUND = "Failed to find a file by name: %s.";
    public static final String ERROR_PROJECT_NOT_FOUND = "Failed to find a dataset by ID: %d.";
    public static final String ERROR_FILES_NOT_REGISTERED = "Failed to register files: %s.";

    private MessageConstants(){
        //no op
    }

    /**
     * Formats and returns the message with input arguments
     * @param message to format
     * @param args to add to message
     * @return formatted message
     */
    public static String getMessage(String message, Object... args) {
        return String.format(message, args);
    }
}
