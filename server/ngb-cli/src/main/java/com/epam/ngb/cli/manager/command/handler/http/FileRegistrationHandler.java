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

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_FILES_NOT_REGISTERED;
import static com.epam.ngb.cli.constants.MessageConstants.MINIMUM_COMMAND_ARGUMENTS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.epam.ngb.cli.entity.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code {@link FileRegistrationHandler}} represents a tool handling "register_file" command and
 * sending request to NGB server for file registration. This command requires at least two arguments:
 * reference ID or name and path to a file. Registration of several files by
 * one command is also supported.
 * Indexed files should be provided in one string argument containing path to file and path to index
 * delimited by a '?' symbol.
 * By default the command doesn't produce any output, but it can be turned on by setting {@code true}
 * value to {@code printJson} or {@code printTable} fields.
 */
@Command(type = Command.Type.REQUEST, command = {"register_file"})
public class FileRegistrationHandler extends AbstractHTTPCommandHandler {

    /**
     * List of files for registration, files are represented as a pair of Strings:
     * left String is path to file itself, right one - path to index (might be optional or
     * required, depending on file format)
     */
    private List<Pair<String, String>> files = new ArrayList<>();
    /**
     * Optional file name for registration, this option is skipped,
     * if several files are registered at once
     */
    private String fileName;
    /**
     * Pretty name of a file
     */
    private String prettyName;
    /**
     * ID of the reference, for which files are registered for
     */
    private Long referenceId;
    /**
     * If true command will output result of file registration in a json format
     */
    private boolean printJson;
    /**
     * If true command will output result of file registration in a table format
     */
    private boolean printTable;

    /**
     * Specifies if a feature index for registered VCF or GFF/GTF file should be created
     */
    private boolean doIndex;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRegistrationHandler.class);

    /**
     * Default constructor is required to enable reflexion creation of an instance
     */
    public FileRegistrationHandler(){
        //no operations
    }

    /**
     * Constructor for creating {@code FileRegistrationHandler} from an outer
     * {@code AbstractHTTPCommandHandler}
     * @param path to file for registration, if file with index is given, file and index
     *             must be separated with '?' symbol
     * @param referenceId ID of the reference to use for file registration
     *                    (not BiologicalDataItemId!)
     * @param handler an outer {@code AbstractHTTPCommandHandler} for providing configuration
     * @param options {@code {@link ApplicationOptions}} with optional name and output options
     */
    public FileRegistrationHandler(String path, Long referenceId,
            AbstractHTTPCommandHandler handler, ApplicationOptions options) {
        super(handler);
        files.add(super.parseAndVerifyFilePath(path));
        this.referenceId = referenceId;
        this.printJson = options.isPrintJson();
        this.printTable = options.isPrintTable();
        this.doIndex = options.isDoIndex();
        this.prettyName = options.getPrettyName();
    }

    /**
     * Verifies that input arguments contain the required parameters:
     * first argument must be reference ID or name, second and the following arguments specify files
     * and their indexes for registration. Method also checks that input files are supported by
     * NGB server and converts a reference name to reference ID, if it is required.
     * @param arguments command line arguments for 'register_file' command
     * @param options to specify a file name (if only one file is registered) and output options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() < 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(MINIMUM_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }

        referenceId = loadReferenceId(arguments.get(0));

        if (options.getName() != null && arguments.size() == 2) {
            fileName = options.getName();
        }

        for (int i = 1; i < arguments.size(); i++) {
            files.add(parseAndVerifyFilePath(arguments.get(i)));
        }

        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
        doIndex = options.isDoIndex();
        prettyName = options.getPrettyName();
    }

    /**
     * Performs a file registration request to NGB server and prints registration result to
     * StdOut if it is specified by the command line options
     * @return 0 if request completed successfully
     */
    @Override
    public int runCommand() {
        List<BiologicalDataItem> items = registerItems();
        if (!items.isEmpty() && (printJson || printTable)) {
            AbstractResultPrinter printer = AbstractResultPrinter
                    .getPrinter(printTable, items.get(0).getFormatString(items));
            printer.printHeader(items.get(0));
            items.forEach(printer::printItem);
        }
        //if we failed to register all files return 1
        return items.size() != files.size() ? 1 : 0;
    }

    protected List<BiologicalDataItem> registerItems() {
        List<BiologicalDataItem> items = new ArrayList<>(files.size());
        List<Pair<String, String>> failed = new ArrayList<>(files.size());
        for (Pair<String, String> file : files) {
            BiologicalDataItemFormat format = BiologicalDataItemFormat.getByFilePath(file.getLeft());
            String url = String.format(getRequestUrl(), format.name().toLowerCase());
            HttpRequestBase request = getRequest(url);
            setDefaultHeader(request);
            if (isSecure()) {
                addAuthorizationToRequest(request);
            }

            RegistrationRequest registration = createRegistrationRequest(file, format);

            String result = getPostResult(registration, (HttpPost) request);
            try {
                ResponseResult<BiologicalDataItem> responseResult = getMapper().readValue(result,
                        getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class,
                                ResponseResult.class, BiologicalDataItem.class));
                if (ERROR_STATUS.equals(responseResult.getStatus())) {
                    LOGGER.error(responseResult.getMessage());
                    failed.add(file);
                } else {
                    items.add(responseResult.getPayload());
                }
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }
        if (!failed.isEmpty()) {
            LOGGER.error(MessageConstants.getMessage(ERROR_FILES_NOT_REGISTERED,
                    failed.stream().map(Pair::getLeft).collect(Collectors.joining(","))));
        }
        return items;
    }

    private RegistrationRequest createRegistrationRequest(Pair<String, String> file, BiologicalDataItemFormat format) {
        RegistrationRequest registration = new RegistrationRequest();
        registration.setName(fileName);
        registration.setPrettyName(prettyName);
        registration.setPath(file.getLeft());
        registration.setIndexPath(file.getRight());
        registration.setReferenceId(referenceId);
        registration.setType(BiologicalDataItemResourceType.getTypeFromPath(file.getKey()));

        if (file.getValue() != null) {
            registration.setIndexType(BiologicalDataItemResourceType.getTypeFromPath(file.getValue()));
        }

        if (format == BiologicalDataItemFormat.VCF || format == BiologicalDataItemFormat.GENE) {
            registration.setDoIndex(doIndex);
        }
        return registration;
    }
}
