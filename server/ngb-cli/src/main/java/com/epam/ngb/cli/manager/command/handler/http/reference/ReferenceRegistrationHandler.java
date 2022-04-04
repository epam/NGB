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

package com.epam.ngb.cli.manager.command.handler.http.reference;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;
import static com.epam.ngb.cli.entity.BiologicalDataItemResourceType.getTypeFromPath;

import com.epam.ngb.cli.entity.BiologicalDataItemResourceType;
import com.epam.ngb.cli.entity.SpeciesEntity;
import java.util.List;

import com.epam.ngb.cli.app.Utils;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.RegistrationRequest;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;

/**
 * {@code {@link ReferenceRegistrationHandler}} represents a tool handling "register_reference" command and
 * sending request to NGB server for reference registration. This command requires strictly one argument:
 * path to the reference file.
 * By default the command doesn't produce any output, but it can be turned on by setting {@code true}
 * value to {@code printJson} or {@code printTable} fields.
 */
@Command(type = Command.Type.REQUEST, command = {"register_reference"})
public class ReferenceRegistrationHandler extends AbstractHTTPCommandHandler {

    /**
     * Optional reference name for registration
     */
    private String referenceName;
    /**
     * Pretty name of a reference
     */
    private String prettyName;
    /**
     * Path to the reference file
     */
    private String referencePath;
    /**
     * Specifies a registered gene file to add to reference during registration
     */
    private Long geneFileId;
    /**
     * Specifies a path to a new gene file be added to reference during registration
     */
    private String geneFilePath;
    /**
     * Specifies a path to a new gene file  index be added to reference during registration
     */
    private String geneIndexPath;

    /**
     * If true command will output result of reference registration in a json format
     */
    private boolean printJson;
    /**
     * If true command will output result of reference registration in a table format
     */
    private boolean printTable;


    private String speciesVersion;

    /**
     * If true, the GC content files won't be created for a reference during registration.
     */
    private boolean noGCContent = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceRegistrationHandler.class);

    /**
     * Verifies that input arguments contain the required parameters:
     * first and the only argument must be path to the reference file.
     * @param arguments command line arguments for 'register_reference' command
     * @param options to specify a reference name and output options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        referencePath = getTypeFromPath(arguments.get(0)) == BiologicalDataItemResourceType.FILE ?
                Utils.getNormalizeAndAbsolutePath(arguments.get(0)) : arguments.get(0);
        if (options.getName() != null) {
            referenceName = options.getName();
        }
        if (options.getGeneFile() != null) {
            try {
                geneFileId = loadFileByNameOrBioID(options.getGeneFile()).getId();
            } catch (ApplicationException e) {
                LOGGER.debug(e.getMessage(), e);
                Pair<String, String> path = parseAndVerifyFilePath(options.getGeneFile());
                geneFilePath = Utils.getNormalizeAndAbsolutePath(path.getLeft());
                if (geneIndexPath != null) {
                    geneIndexPath = Utils.getNormalizeAndAbsolutePath(path.getRight());
                }
            }
        }
        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
        noGCContent = options.isNoGCContent();
        prettyName = options.getPrettyName();
        speciesVersion = options.getSpeciesVersion();
    }

    /**
     * Performs a reference registration request to NGB server and prints registration result to
     * StdOut if it is specified by the command line options
     * @return 0 if request completed successfully
     */
    @Override public int runCommand() {
        HttpRequestBase request = getRequest(getRequestUrl());
        RegistrationRequest registration = new RegistrationRequest();
        registration.setName(referenceName);
        registration.setPrettyName(prettyName);
        registration.setPath(referencePath);
        registration.setGeneFileId(geneFileId);
        if (noGCContent) {
            registration.setNoGCContent(noGCContent);
        }
        if (geneFilePath != null) {
            RegistrationRequest geneRegistration = new RegistrationRequest();
            geneRegistration.setPath(geneFilePath);
            geneRegistration.setIndexPath(geneIndexPath);
            registration.setGeneFileRequest(geneRegistration);
        }
        if (speciesVersion != null) {
            SpeciesEntity speciesEntity = new SpeciesEntity();
            speciesEntity.setVersion(speciesVersion);
            registration.setSpecies(speciesEntity);
        }
        String result = getPostResult(registration, (HttpPost)request);
        checkAndPrintRegistrationResult(result, printJson, printTable);
        return 0;
    }
}
