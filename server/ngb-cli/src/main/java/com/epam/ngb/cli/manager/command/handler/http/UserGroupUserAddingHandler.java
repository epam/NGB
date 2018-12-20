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
import com.epam.ngb.cli.entity.ExtendedRole;
import com.epam.ngb.cli.entity.Role;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.client.methods.HttpPost;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 */
@Command(type = Command.Type.REQUEST, command = {"add_group"})
public class UserGroupUserAddingHandler extends AbstractHTTPCommandHandler {

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
    private Long groupId;

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
            List<String> namesOrIds = Arrays.stream(options.getUsers().split(",")).collect(Collectors.toList());
            if (namesOrIds.stream().allMatch(NumberUtils::isDigits)) {
                users = namesOrIds.stream().map(Long::parseLong).collect(Collectors.toList());
            } else {
                users = loadListOfUsers(namesOrIds);
            }
        } else {
            throw new IllegalArgumentException("At least one user should be provided! Use -u (--users) option");
        }

        String roleIdentifier = arguments.get(0);
        if (NumberUtils.isDigits(roleIdentifier)) {
            this.groupId = Long.parseLong(roleIdentifier);
        } else {
            Role role = loadRoleByName(roleIdentifier);
            if (role == null) {
                throw new IllegalArgumentException("Group with name: '" + roleIdentifier + "' not found!");
            }
            this.groupId = role.getId();
        }
    }

    @Override public int runCommand() {
        HttpPost post = (HttpPost) getRequestFromURLByType("POST",
                getServerParameters().getServerUrl()
                        + String.format(getRequestUrl(), groupId,
                users.stream().map(String::valueOf).collect(Collectors.joining(","))));
        ExtendedRole result = getResult(getPostResult(null, post), ExtendedRole.class);

        if (printJson || printTable) {
            AbstractResultPrinter printer = AbstractResultPrinter
                    .getPrinter(printTable, result.getUsers().get(0).getFormatString(result.getUsers()));
            printer.printHeader(result.getUsers().get(0));
            result.getUsers().forEach(printer::printItem);
        }
        return 0;
    }

}
