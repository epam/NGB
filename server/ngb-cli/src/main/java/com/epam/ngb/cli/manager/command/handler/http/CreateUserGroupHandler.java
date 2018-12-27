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
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_ROLE_MODEL_IS_NOT_SUPPORTED;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * Creates User Group with specified name and assign users to this group if users are provided
 */
@Command(type = Command.Type.REQUEST, command = {"reg_group"})
public class CreateUserGroupHandler extends AbstractHTTPCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserGroupHandler.class);


    public static final String ASSIGN_USERS_TO_ROLE = "/restapi/role/%d/assign?userIds=%s";

    /**
     * If true command will output result in a table format, otherwise
     * json format will be used
     */
    private boolean printTable;

    /**
     * If true command will output result in a json format
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

        if (!isRoleModelEnable()) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ERROR_ROLE_MODEL_IS_NOT_SUPPORTED));
        }

        if (options.getUsers() != null) {
            List<String> userIdsOrNames = Arrays.stream(options.getUsers().split(","))
                    .collect(Collectors.toList());
            if (userIdsOrNames.stream().allMatch(NumberUtils::isDigits)) {
                users = userIdsOrNames.stream().map(Long::parseLong).collect(Collectors.toList());
            } else {
                List<NgbUser> loaded = loadListOfUsers(userIdsOrNames);
                if (loaded.size() != userIdsOrNames.size()) {
                    Set<String> namesSet = loaded.stream().map(NgbUser::getUserName).collect(Collectors.toSet());
                    userIdsOrNames.forEach(name -> {
                        if (namesSet.contains(name)) {
                            LOGGER.error("User with name: " + name + " not found!");
                        }
                    });
                }
                users = loaded.stream().map(NgbUser::getId).collect(Collectors.toList());
            }
        }
        roleName = arguments.get(0);
    }

    @Override public int runCommand() {
        RequestManager.executeRequest(getRequest(String.format(getRequestUrl(), roleName)));
        if (!users.isEmpty()) {
            HttpPost post = (HttpPost) getRequestFromURLByType("POST",
                    getServerParameters().getServerUrl()
                            + String.format(ASSIGN_USERS_TO_ROLE, loadRoleByName(roleName).getId(),
                    users.stream().map(String::valueOf).collect(Collectors.joining(","))));
            String result = getPostResult(null, post);
            ExtendedRole createdRole = getResult(result, ExtendedRole.class);
            if (printJson || printTable) {
                AbstractResultPrinter printer = AbstractResultPrinter
                        .getPrinter(printTable, createdRole.getFormatString(Collections.singletonList(createdRole)));
                printer.printHeader(createdRole);
                printer.printItem(createdRole);
            }
        }
        return 0;
    }

}
