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
import com.epam.ngb.cli.entity.NgbUser;
import com.epam.ngb.cli.entity.NgbUserVO;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.Role;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 */
@Command(type = Command.Type.REQUEST, command = {"reg_user"})
public class CreateUserHandler extends AbstractHTTPCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserHandler.class);
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * If true command will output registered user in a table format, otherwise
     * json format will be used
     */
    private boolean printTable;

    /**
     * If true command will output result in a json format
     */
    private boolean printJson;

    private List<Long> groups = Collections.emptyList();
    private NgbUserVO userVO;

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

        HttpGet request = (HttpGet) getRequestFromURLByType("GET",
                getServerParameters().getServerUrl() + getServerParameters().getAllRolesUrl());
        String result = RequestManager.executeRequest(request);
        ResponseResult<List<Role>> responseResult;
        try {
            responseResult = getMapper().readValue(result, getMapper().getTypeFactory()
                    .constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            getMapper().getTypeFactory()
                                    .constructParametrizedType(List.class, List.class,
                                            Role.class)));
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        Map<String, Role> rolesByName = responseResult.getPayload().stream()
                .collect(Collectors.toMap(Role::getName, r -> r));

        if (options.getGroups() != null) {
            groups = Arrays.stream(options.getGroups().split(","))
                    .map(g -> {
                        String groupName = !g.startsWith(ROLE_PREFIX) ? ROLE_PREFIX + g : g;
                        Role role = rolesByName.get(groupName);
                        if (role != null) {
                            return role.getId();
                        } else {
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        userVO = new NgbUserVO(arguments.get(0), groups);
    }

    @Override public int runCommand() {
        HttpPost request = (HttpPost) getRequest(getRequestUrl());
        NgbUser result = getResult(getPostResult(userVO, request), NgbUser.class);
        if (printJson || printTable) {
            AbstractResultPrinter printer = AbstractResultPrinter
                    .getPrinter(printTable, result.getFormatString(Collections.singletonList(result)));
            printer.printHeader(result);
            printer.printItem(result);
        }
        return 0;
    }

}
