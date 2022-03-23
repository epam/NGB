/*
 * MIT License
 *
 * Copyright (c) 2022 EPAM Systems
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

package com.epam.ngb.cli.manager.command.handler.http.pathway;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.pathway.BioPAXRegistrationRequest;
import com.epam.ngb.cli.entity.pathway.NGBPathway;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.TextUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;
import static com.epam.ngb.cli.manager.command.handler.http.pathway.PathwayRegistrationHandler.getTaxIds;

@Slf4j
@Command(type = Command.Type.REQUEST, command = {"reg_biopax"})
public class BioPAXRegistrationHandler extends AbstractHTTPCommandHandler {

    private BioPAXRegistrationRequest registrationRequest;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'reg_biopax' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 1, arguments.size()));
        }
        registrationRequest = BioPAXRegistrationRequest.builder()
                .path(arguments.get(0))
                .build();
        if (!TextUtils.isBlank(options.getTaxIds())) {
            final Set<Long> taxIds = getTaxIds(options.getTaxIds());
            registrationRequest.setTaxIds(taxIds);
        }
    }

    @Override public int runCommand() {
        final HttpPost request = (HttpPost) getRequest(getRequestUrl());
        final String result = getPostResult(registrationRequest, request);
        try {
            ResponseResult<Boolean> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            NGBPathway.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            log.info("BioPAX file was successfully registered.");
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
