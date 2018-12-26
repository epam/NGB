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
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * Deletes user with specific name of id
 */
@Command(type = Command.Type.REQUEST, command = {"del_user"})
public class DeleteUserHandler extends AbstractHTTPCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUserHandler.class);

    private String userName;
    private Long userToDeleteId;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'grant_permission' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 1, arguments.size()));
        }

        userName = arguments.get(0);
        if (NumberUtils.isDigits(userName)) {
            userToDeleteId = Long.parseLong(userName);
        } else {
            HttpGet request = (HttpGet) getRequestFromURLByType("GET",
                    getServerParameters().getServerUrl()
                    + String.format(getServerParameters().getFindUserUrl(), userName));

            try {
                ResponseResult<NgbUser> responseResult = getMapper().readValue(RequestManager.executeRequest(request),
                        getMapper().getTypeFactory()
                                .constructParametrizedType(ResponseResult.class, ResponseResult.class, NgbUser.class));
                if (responseResult.getPayload() != null) {
                    userToDeleteId = responseResult.getPayload().getId();
                } else {
                    throw new ApplicationException(String.format("User: %s not found!", userName));
                }
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }
    }

    @Override public int runCommand() {
        HttpRequestBase request = getRequest(String.format(getRequestUrl(), userToDeleteId));
        String result = RequestManager.executeRequest(request);
        getResult(result, Object.class);
        LOGGER.info("User : '" + userName +  "' deleted!");
        return 0;
    }

}
