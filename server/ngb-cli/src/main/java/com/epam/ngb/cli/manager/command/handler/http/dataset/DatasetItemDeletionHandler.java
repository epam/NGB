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

package com.epam.ngb.cli.manager.command.handler.http.dataset;

import static com.epam.ngb.cli.constants.MessageConstants.MINIMUM_COMMAND_ARGUMENTS;

import java.util.ArrayList;
import java.util.List;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;

/**
 * {@code {@link DatasetItemDeletionHandler}} represents a tool for handling 'remove_from_dataset'
 * command and removes files from a dataset (ex-project) on NGB server. This command requires at
 * least two arguments: dataset ID or name and file name or ID.
 */
@Command(type = Command.Type.REQUEST, command = {"remove_from_dataset"})
public class DatasetItemDeletionHandler extends AbstractHTTPCommandHandler {
    /**
     * ID of dataset to remove files from
     */
    private Long datasetId;
    /**
     * List of file IDs, that are deleted from the dataset
     */
    private List<Long> items = new ArrayList<>();
    /**
     * If true command will output result of dataset modification in a json format
     */
    private boolean printJson;
    /**
     * If true command will output result of dataset modification in a table format
     */
    private boolean printTable;

    /**
     * Verifies that input arguments contain the required parameters:
     * first argument must be dataset ID or name, second and the following arguments
     * arguments must specify files to be deleted from the dataset (addressed by name or ID).
     * @param arguments command line arguments for 'remove_from_dataset' command
     * @param options to specify output options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MINIMUM_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }
        datasetId = parseProjectId(arguments.get(0));
        for (int i = 1; i < arguments.size(); i++) {
            items.add(parseFileId(arguments.get(i)));
        }
        this.printTable = options.isPrintTable();
        this.printJson = options.isPrintJson();
    }

    /**
     * Performs deleting files from a dataset request to NGB server and prints registration result to
     * StdOut if it is specified by the command line options
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        addOrDeleteItemsFromDataset(items, datasetId, printJson, printTable);
        return 0;
    }
}
