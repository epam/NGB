/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.ngb.cli.manager.command.handler.http.species;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * {@code {@link SpeciesAddingHandler}} represents a tool for handling 'add_species' command and
 * adding a species to a reference, registered on NGB server. This command requires strictly two arguments:
 * - reference ID or name
 * - species version (should be registered on NGB server before)
 */
@Command(type = Command.Type.REQUEST, command = {"add_species"})
public class SpeciesAddingHandler extends AbstractHTTPCommandHandler {

    private Long referenceId;
    private String speciesVersion;

    /**
     * If true command will output result of species addition in json format
     */
    private boolean printJson;
    /**
     * If true command will output result of species addition in table format
     */
    private boolean printTable;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }
        referenceId = loadReferenceId(arguments.get(0));
        speciesVersion = StringUtils.trim(arguments.get(1));
        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        try {
            String url = serverParameters.getServerUrl() + getRequestUrl();
            URIBuilder builder = new URIBuilder(String.format(url, referenceId));
            if (speciesVersion != null) {
                builder.addParameter("speciesVersion", speciesVersion);
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
