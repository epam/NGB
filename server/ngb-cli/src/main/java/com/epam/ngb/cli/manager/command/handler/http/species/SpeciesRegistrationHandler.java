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
import com.epam.ngb.cli.entity.RegistrationRequest;
import com.epam.ngb.cli.entity.SpeciesEntity;
import com.epam.ngb.cli.manager.command.handler.Command;
import java.util.List;

import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * {@code {@link SpeciesRegistrationHandler}} represents a tool handling "register_species" command and
 * sending request to NGB server for species registration. This command requires strictly two argument:
 * name of the species and version.
 * By default the command doesn't produce any output, but it can be turned on by setting {@code true}
 * value to {@code printJson} or {@code printTable} fields.
 */
@Command(type = Command.Type.REQUEST, command = {"register_species"})
public class SpeciesRegistrationHandler extends AbstractHTTPCommandHandler {

    /**
     * Species representation.
     */
    private SpeciesEntity entity;

    /**
     * If true command will output result of species registration in a json format
     */
    private boolean printJson;

    /**
     * If true command will output result of species registration in a table format
     */
    private boolean printTable;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MessageConstants.ILLEGAL_COMMAND_ARGUMENTS,
                getCommand(), 2, arguments.size()));
        }

        String speciesName = StringUtils.trim(arguments.get(0));
        String speciesVersion = StringUtils.trim(arguments.get(1));
        if (!StringUtils.isAlphanumeric(speciesName) || !StringUtils.isAlphanumeric(speciesVersion)) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MessageConstants.NOT_ALPHANUMERIC_ARGUMENTS,
                    getCommand()));
        }

        entity = new SpeciesEntity();
        entity.setName(speciesName);
        entity.setVersion(speciesVersion);
        entity.setTaxId(options.getTaxId());

        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        HttpRequestBase request = getRequest(getRequestUrl());
        RegistrationRequest registration = new RegistrationRequest();
        registration.setSpecies(entity);
        String result = getPostResult(registration, (HttpPost) request);
        checkAndPrintSpeciesResult(result, printJson, printTable);
        return 0;
    }
}
