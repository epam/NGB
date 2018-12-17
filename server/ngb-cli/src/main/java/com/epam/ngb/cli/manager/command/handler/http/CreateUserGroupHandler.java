/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2018 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.*;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 */
@Command(type = Command.Type.REQUEST, command = {"reg_group"})
public class CreateUserGroupHandler extends AbstractHTTPCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserGroupHandler.class);
    public static final String ASSIGN_USERS_TO_ROLE = "/restapi/role/%d/assign?userIds=%s";

    /**
     * If true command will output list of datasets in a table format, otherwise
     * json format will be used
     */
    private boolean printTable;

    /**
     * If true command will output result of reference registration in a json format
     */
    private boolean printJson;

    private List<Long> users = Collections.emptyList();
    private String roleName;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'grant_permission' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {

        printTable = options.isPrintTable();
        printJson = options.isPrintJson();

        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 1, arguments.size()));
        }

        if (options.getUsers() != null) {
            users = loadListOfUsers(Arrays.stream(options.getUsers().split(","))
                    .collect(Collectors.toList()));
        }
        roleName = arguments.get(0);
    }

    @Override public int runCommand() {
        RequestManager.executeRequest(getRequest(String.format(getRequestUrl(), roleName)));
        HttpPost post = (HttpPost) getRequestFromURLByType("POST",
                getServerParameters().getServerUrl()
                        + String.format(ASSIGN_USERS_TO_ROLE, loadRoleByName(roleName).getId(),
                users.stream().map(String::valueOf).collect(Collectors.joining(","))));
        String result = getPostResult(null, post);
        checkAndPrintResult(result, printJson, printTable, Role.class);
        return 0;
    }

}
