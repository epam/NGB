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

import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.command.handler.http.FileRegistrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;

/**
 * {@code {@link DatasetItemAddingHandler}} represents a tool for handling 'add_to_dataset' command and
 * add files to a dataset (ex-project) on NGB server. This command requires at least two arguments:
 * dataset ID or name and file name, ID or path. The command supports adding to dataset files, that can
 * be either already registered in the server (addressed by name or ID) or new files (path to files
 * must be provided in this case). New files at first will be registered with help of
 * {@code {@link FileRegistrationHandler }} and then added to a dataset.
 */
@Command(type = Command.Type.REQUEST, command = {"add_to_dataset"})
public class DatasetItemAddingHandler extends AbstractHTTPCommandHandler {
    /**
     * ID of dataset to add files to
     */
    private Long datasetId;
    /**
     * List of file IDs, that are added to the dataset
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetItemAddingHandler.class);

    /**
     * Verifies that input arguments contain the required parameters:
     * first argument must be dataset ID or name, second and the following arguments
     * arguments must specify files to be added to the dataset (registered on the server files are
     * addressed by name or ID, for new files path should be provided).
     * If new files are registered by this command, all rules and restrictions that are
     * applied to file registration are used.
     * @param arguments command line arguments for 'add_to_dataset' command
     * @param options to specify output options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MINIMUM_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }

        datasetId = parseProjectId(arguments.get(0));
        Long referenceUniqueId = null;

        for (int i = 1; i < arguments.size(); i++) {
            String file = arguments.get(i);
            try {
                Long id = parseFileId(file);
                items.add(id);
                continue;
            } catch (ApplicationException e) {
                LOGGER.debug(e.getMessage(), e);
            }

            ApplicationOptions registerOptions = new ApplicationOptions();
            registerOptions.setDoIndex(options.isDoIndex());

            referenceUniqueId = referenceUniqueId == null ? loadReferenceIdFromDataset(datasetId) : referenceUniqueId;
            FileRegistrationHandler handler =
                    new FileRegistrationHandler(file, referenceUniqueId, this, registerOptions);
            handler.setRequestUrl(handler.getServerParameters().getRegistrationUrl());
            handler.setRequestType("POST");

            List<BiologicalDataItem> registeredFiles = handler.registerItems();
            registeredFiles.forEach(f -> items.add(f.getBioDataItemId()));
        }

        this.printTable = options.isPrintTable();
        this.printJson = options.isPrintJson();
    }

    /**
     * Performs adding files to a dataset request to NGB server and prints registration result to
     * StdOut if it is specified by the command line options
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        addOrDeleteItemsFromDataset(items, datasetId, printJson, printTable);
        return 0;
    }
}
