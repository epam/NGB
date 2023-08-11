/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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

package com.epam.ngb.cli.manager.command.handler.http.pdb;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epam.ngb.cli.entity.pdb.PdbFile;
import com.epam.ngb.cli.entity.pdb.PdbFileRegistrationRequest;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.TextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Slf4j
@Command(type = Command.Type.REQUEST, command = {"reg_pdb"})
public class PdbFileRegistrationHandler extends AbstractHTTPCommandHandler {

    private PdbFileRegistrationRequest registrationRequest;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'reg_pdb' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 2, arguments.size()));
        }
        registrationRequest = PdbFileRegistrationRequest.builder()
                .geneId(arguments.get(0))
                .path(arguments.get(1))
                .build();
        registrationRequest.setName(options.getName());
        registrationRequest.setPrettyName(options.getPrettyName());
        if (!TextUtils.isBlank(options.getMetadata())) {
            registrationRequest.setMetadata(deSerialize(options.getMetadata()));
        }
    }

    @Override public int runCommand() {
        final HttpPost request = (HttpPost) getRequest(getRequestUrl());
        final String result = getPostResult(registrationRequest, request);
        try {
            ResponseResult<PdbFile> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            PdbFile.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            log.info("PDB File was successfully registered. ID: " +
                    responseResult.getPayload().getPdbFileId() + ".");
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }

    @SneakyThrows
    private static Map<String, String> deSerialize(final String data) {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(data, HashMap.class);
    }
}
