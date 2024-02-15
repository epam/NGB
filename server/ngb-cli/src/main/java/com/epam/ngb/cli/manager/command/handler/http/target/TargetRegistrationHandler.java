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

package com.epam.ngb.cli.manager.command.handler.http.target;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.target.*;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_INCORRECT_FILE_FORMAT;
import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

@Slf4j
@Command(type = Command.Type.REQUEST, command = {"reg_target"})
public class TargetRegistrationHandler extends AbstractHTTPCommandHandler {

    private static final String SEPARATOR = ",";
    private TargetRegistrationRequest registrationRequest;

    /**
     * Verifies input arguments
     * @param arguments command line arguments for 'reg_target' command
     * @param options
     */
    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 5) {
            throw new IllegalArgumentException(MessageConstants.getMessage(
                    ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 5, arguments.size()));
        }

        final String targetGenesPath = arguments.get(3);
        registrationRequest = TargetRegistrationRequest.builder()
                .targetName(arguments.get(0))
                .products(argToList(arguments.get(1)))
                .diseases(argToList(arguments.get(2)))
                .targetGenes(readTargetGenes(targetGenesPath))
                .type(TargetType.valueOf(arguments.get(4)))
                .build();
    }

    @Override public int runCommand() {
        final HttpPost request = (HttpPost) getRequest(getRequestUrl());
        final String result = getPostResult(registrationRequest, request);
        System.out.println(result);
        try {
            ResponseResult<Target> responseResult = getMapper().readValue(result,
                    getMapper().getTypeFactory().constructParametrizedType(ResponseResult.class, ResponseResult.class,
                            Target.class));
            System.out.println("request");
            if (!SUCCESS_STATUS.equals(responseResult.getStatus())) {
                throw new ApplicationException(responseResult.getMessage());
            }
            log.info("Target was successfully registered. ID: " +
                    responseResult.getPayload().getId() + ".");
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }

    private static List<String> argToList(final String argument) {
        return Stream.of(argument.split(SEPARATOR))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static List<TargetGene> readTargetGenes(final String path) {
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            parseLine(line);
            String[] cells;
            final List<TargetGene> targetGenes = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    break;
                }
                cells = parseLine(line);
                TargetGene targetGene = TargetGene.builder()
                        .geneId(cells[0])
                        .geneName(cells[1])
                        .taxId(Long.parseLong(cells[2]))
                        .speciesName(cells[3])
                        .priority(TargetGenePriority.getByValue(Integer.parseInt(cells[4])))
                        .build();
                targetGenes.add(targetGene);
            }
            return targetGenes;
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage());
        }
    }

    private static String[] parseLine(final String line) {
        final String[] cells = line.split(SEPARATOR);
        if (cells.length != 5) {
            throw new ApplicationException(MessageConstants.getMessage(ERROR_INCORRECT_FILE_FORMAT));
        }
        return cells;
    }
}
