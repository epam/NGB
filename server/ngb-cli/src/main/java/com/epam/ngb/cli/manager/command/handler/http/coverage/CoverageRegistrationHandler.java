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

package com.epam.ngb.cli.manager.command.handler.http.coverage;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.coverage.BamCoverage;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Slf4j
@Command(type = Command.Type.REQUEST, command = {"add_coverage"})
public class CoverageRegistrationHandler extends AbstractHTTPCommandHandler {

    private BamCoverage bamCoverage;
    private Long bamId;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'add_coverage' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 2, arguments.size()));
        }
        bamId = loadItemId(arguments.get(0));

        bamCoverage = BamCoverage.builder()
                .bamId(bamId)
                .step(Integer.parseInt(arguments.get(1)))
                .build();
    }

    @Override public int runCommand() {
        final HttpPost request = (HttpPost) getRequest(getRequestUrl());
        final String result = getPostResult(bamCoverage, request);
        try {
            ResponseResult<BamCoverage> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            BamCoverage.class));
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            log.info("Bam file coverage was successfully registered. ID: " +
                    responseResult.getPayload().getCoverageId() + ".");
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
