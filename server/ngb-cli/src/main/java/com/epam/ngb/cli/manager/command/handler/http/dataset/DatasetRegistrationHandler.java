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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.command.handler.http.FileRegistrationHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.entity.ProjectItem;
import com.epam.ngb.cli.entity.RegistrationRequest;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;


/**
 * {@code {@link DatasetRegistrationHandler}} represents a tool for handling 'register_dataset' command and
 * creating new datasets (ex-projects) on NGB server. This command requires at least two arguments:
 * reference ID or name and dataset name. The command also supports creating a dataset with files, that can
 * be either already registered in the server (addressed by name or ID) or new files (path to files
 * must be provided in this case). New files at first will be registered with help of
 * {@code {@link FileRegistrationHandler }} and then added to a dataset.
 * Command supports hierarchical datasets, parent dataset is specified by 'parent' option.
 */
@Command(type = Command.Type.REQUEST, command = {"register_dataset"})
public class DatasetRegistrationHandler extends AbstractHTTPCommandHandler {

    /**
     * Name of a created dataset, name must be unique
     */
    private String name;
    /**
     * Pretty name of a created dataset
     */
    private String prettyName;
    /**
     * ID of a parent dataset in case of hierarchical datasets
     */
    private Long parentId;
    /**
     * List of files in a dataset, at least one - reference - file is required for
     * dataset registration
     */
    private List<ProjectItem> items;
    /**
     * If true command will output result of dataset registration in a json format
     */
    private boolean printJson;
    /**
     * If true command will output result of dataset registration in a table format
     */
    private boolean printTable;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetRegistrationHandler.class);

    /**
     * Verifies that input arguments contain the required parameters:
     * first argument must be reference ID or name, second dataset name and optional following
     * arguments may specify files to be added to the dataset (registered on the server files are
     * addressed by name or ID, for new files path should be provided).
     * If new files are registered by this command, all rules and restrictions that are
     * applied to file registration are used.
     * If a dataset is a part of hierarchical project, it's parent dataset may ne specified
     * by 'parentID' option (name or ID of parent dataset).
     * @param arguments command line arguments for 'register_dataset' command
     * @param options to specify a parentID of the dataset and output options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MINIMUM_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }
        name = arguments.get(1);
        prettyName = options.getPrettyName();
        if (options.getParent() != null) {
            parentId = parseProjectId(options.getParent());
        }
        items = new ArrayList<>();
        Long referenceId = parseFileId(arguments.get(0));
        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
        items.add(new ProjectItem(referenceId, false));
        Long referenceUniqueId = null;
        for (int i = 2; i < arguments.size(); i++) {
            String file = arguments.get(i);
            try {
                Long id = parseFileId(file);
                items.add(new ProjectItem(id, false));
                continue;
            } catch (ApplicationException e) {
                LOGGER.debug(e.getMessage(), e);
            }

            ApplicationOptions registerOptions = new ApplicationOptions();
            registerOptions.setDoIndex(options.isDoIndex());

            referenceUniqueId = referenceUniqueId == null ? loadReferenceId(arguments.get(0)) : referenceUniqueId;
            FileRegistrationHandler handler =
                    new FileRegistrationHandler(file, referenceUniqueId, this, registerOptions);
            handler.setRequestUrl(handler.getServerParameters().getRegistrationUrl());
            handler.setRequestType(HttpPost.METHOD_NAME);
            List<BiologicalDataItem> registeredFiles = handler.registerItems();
            if (!registeredFiles.isEmpty()) {
                registeredFiles.forEach(f -> items.add(new ProjectItem(f.getBioDataItemId(), false)));
            }
        }
    }

    /**
     * Performs a dataset registration request to NGB server and prints registration result to
     * StdOut if it is specified by the command line options
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        try {
            URIBuilder builder = new URIBuilder(serverParameters.getServerUrl() + getRequestUrl());
            if (parentId != null) {
                builder.addParameter("parentId", String.valueOf(parentId));
            }
            HttpRequestBase request = getRequestFromURLByType(HttpPost.METHOD_NAME, builder.build().toString());
            RegistrationRequest registration = new RegistrationRequest();
            registration.setName(name);
            registration.setItems(items);
            registration.setPrettyName(prettyName);
            String result = getPostResult(registration, (HttpPost) request);
            checkAndPrintDatasetResult(result, printJson, printTable);
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
