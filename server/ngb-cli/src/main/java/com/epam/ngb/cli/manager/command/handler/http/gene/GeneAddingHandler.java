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

package com.epam.ngb.cli.manager.command.handler.http.gene;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import java.net.URISyntaxException;
import java.util.List;

import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.command.handler.http.FileRegistrationHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.BiologicalDataItem;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;


/**
 * {@code {@link GeneAddingHandler}} represents a tool for handling 'add_genes' command and
 * adding a gene file to a reference, registered on NGB server. This command requires strictly two arguments:
 * - reference ID or name
 * - gene file ID, name or path to a file (for registration a new file)
 */
@Command(type = Command.Type.REQUEST, command = {"add_genes"})
public class GeneAddingHandler extends AbstractHTTPCommandHandler {

    private Long referenceId;
    private Long geneFileId;
    /**
     * If true command will output result of reference registration in a json format
     */
    private boolean printJson;
    /**
     * If true command will output result of reference registration in a table format
     */
    private boolean printTable;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneAddingHandler.class);

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }
        referenceId = loadReferenceId(arguments.get(0));
        try {
            geneFileId = loadFileByNameOrBioID(arguments.get(1)).getId();
        } catch (ApplicationException e) {
            LOGGER.debug(e.getMessage(), e);

            ApplicationOptions registerOptions = new ApplicationOptions();
            registerOptions.setDoIndex(options.isDoIndex());

            FileRegistrationHandler handler =
                    new FileRegistrationHandler(arguments.get(1), referenceId, this, registerOptions);
            handler.setRequestUrl(handler.getServerParameters().getRegistrationUrl());
            handler.setRequestType("POST");

            List<BiologicalDataItem> registeredItems = handler.registerItems();
            if (!registeredItems.isEmpty()) {
                geneFileId = registeredItems.get(0).getId();
            }
        }
        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        try {
            String url = serverParameters.getServerUrl() + getRequestUrl();
            URIBuilder builder = new URIBuilder(String.format(url, referenceId));
            if (geneFileId != null) {
                builder.addParameter("geneFileId", String.valueOf(geneFileId));
            }
            HttpRequestBase request = getRequestFromURLByType(HttpPut.METHOD_NAME, builder.build().toString());
            String result = RequestManager.executeRequest(request);
            checkAndPrintRegistrationResult(result, printJson, printTable);
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
