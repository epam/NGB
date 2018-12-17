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

package com.epam.ngb.cli.manager.command;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_COMMAND_HANDLER_NOT_FOUND;
import static com.epam.ngb.cli.constants.MessageConstants.ERROR_LOAD_HANDLER_CLASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.CommandHandler;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.command.handler.simple.AbstractSimpleCommandHandler;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * {@code CommandManager} provides service creating and running CLI commands
 */
public class CommandManager {

    private CommandHandler commandHandler;

    public static final String SERVER_URL_PROPERTY = "server_url";
    public static final String JWT_AUTHENTICATION_TOKEN_PROPERTY = "token";
    private static final String SEARCH_URL_PROPERTY = "search_url";
    private static final String REGISTRATION_URL_PROPERTY = "register_url";
    private static final String PROJECT_URL_PROPERTY = "project_url";
    private static final String PROJECT_URL_BY_ID_PROPERTY = "project_load_url";
    private static final String FIND_FILE_URL_PROPERTY = "find_url";
    private static final String PROJECT_TREE_URL_PROPERTY = "project_load_tree";
    private static final String VERSION_URL_PROPERTY = "version_url";
    private static final String ALL_ROLES_URL_PROPERTY = "roles_url";
    private static final String ROLE_URL_PROPERTY = "role_url";
    private static final String FIND_USER_URL_PROPERTY = "find_user_url";
    private static final String FIND_USERS_URL_PROPERTY = "find_users_url";
    public static final String SERVER_VERSION_PROPERTY = "version";
    private static final String HANDLER_PACKAGE = "com.epam.ngb.cli.manager.command.handler";
    public static final String GET_EXISTING_INDEX_URL_PROPERTY = "get_existing_index_url";


    /**
     * Creates {@code CommandManager} for an input {@param command}, loads it's configuration and
     * instantiates a handler for the  {@param command}
     * @param command input CLI command
     * @param configuration of the CLI commands
     * @param serverProperties NGB server configuration
     */
    public CommandManager(String command, CommandConfiguration configuration,
            Properties serverProperties) {
        ServerParameters serverParameters = loadServerParameters(serverProperties);
        this.commandHandler = createCommandHandler(command, serverParameters, configuration);
    }

    /**
     * Checks the correctness of the input arguments and options and runs a CLI command
     * @param arguments
     * @param options
     * @return 0 on success
     */
    public int run(List<String> arguments, ApplicationOptions options) {
        commandHandler.parseAndVerifyArguments(arguments, options);
        return commandHandler.runCommand();
    }

    private CommandHandler createCommandHandler(String command,
            ServerParameters serverParameters, CommandConfiguration configuration) {
        final List<Class<?>> handlers = new ArrayList<>();
        new FastClasspathScanner(HANDLER_PACKAGE)
                .matchClassesWithAnnotation(Command.class, handlers::add).scan();
        Class<?> handler = null;
        Command annotation = null;
        for (Class<?> handlerClass : handlers) {
            annotation = handlerClass.getAnnotation(Command.class);
            if (Arrays.stream(annotation.command()).anyMatch(command::equals)) {
                handler = handlerClass;
                break;
            }
        }
        if (handler == null) {
            throw new ApplicationException(
                    MessageConstants.getMessage(ERROR_COMMAND_HANDLER_NOT_FOUND, command));
        }
        try {
            return instatiateCommandHandler(serverParameters, configuration, handler, annotation);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ApplicationException(MessageConstants.getMessage(ERROR_LOAD_HANDLER_CLASS,
                    handler.getName()), e);
        }
    }

    private CommandHandler instatiateCommandHandler(ServerParameters serverParameters,
            CommandConfiguration configuration, Class<?> handler, Command annotation)
            throws InstantiationException, IllegalAccessException {
        if (annotation.type() == Command.Type.REQUEST) {
            AbstractHTTPCommandHandler restHandler =
                    (AbstractHTTPCommandHandler) handler.newInstance();
            restHandler.setServerParameters(serverParameters);
            restHandler.setConfiguration(configuration);
            return restHandler;

        } else {
            AbstractSimpleCommandHandler simpleHandler =
                    (AbstractSimpleCommandHandler) handler.newInstance();
            simpleHandler.setConfiguration(configuration);
            return simpleHandler;
        }
    }

    private ServerParameters loadServerParameters(Properties serverProperties) {
        ServerParameters parameters = new ServerParameters();
        parameters.setJwtAuthenticationToken(serverProperties.getProperty(JWT_AUTHENTICATION_TOKEN_PROPERTY));
        parameters.setServerUrl(serverProperties.getProperty(SERVER_URL_PROPERTY));
        parameters.setSearchUrl(serverProperties.getProperty(SEARCH_URL_PROPERTY));
        parameters.setRegistrationUrl(serverProperties.getProperty(REGISTRATION_URL_PROPERTY));
        parameters.setProjectLoadUrl(serverProperties.getProperty(PROJECT_URL_PROPERTY));
        parameters.setProjectLoadByIdUrl(serverProperties.getProperty(PROJECT_URL_BY_ID_PROPERTY));
        parameters.setFileFindUrl(serverProperties.getProperty(FIND_FILE_URL_PROPERTY));
        parameters.setVersionUrl(serverProperties.getProperty(VERSION_URL_PROPERTY));
        parameters.setServerVersion(serverProperties.getProperty(SERVER_VERSION_PROPERTY));
        parameters.setProjectTreeUrl(serverProperties.getProperty(PROJECT_TREE_URL_PROPERTY));
        parameters.setAllRolesUrl(serverProperties.getProperty(ALL_ROLES_URL_PROPERTY));
        parameters.setRoleUrl(serverProperties.getProperty(ROLE_URL_PROPERTY));
        parameters.setFindUserUrl(serverProperties.getProperty(FIND_USER_URL_PROPERTY));
        parameters.setFindUsersUrl(serverProperties.getProperty(FIND_USERS_URL_PROPERTY));
        parameters.setExistingIndexSearchUrl(serverProperties.getProperty(GET_EXISTING_INDEX_URL_PROPERTY));
        return parameters;
    }

    CommandHandler getCommandHandler() {
        return commandHandler;
    }
}
