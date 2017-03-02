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

package com.epam.ngb.cli;

import com.epam.ngb.cli.app.ConfigurationLoader;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.CommandConfiguration;
import com.epam.ngb.cli.manager.command.ServerParameters;
import com.epam.ngb.cli.manager.command.handler.http.AbstractHTTPCommandHandler;
import com.epam.ngb.cli.manager.command.handler.http.FileRegistrationHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class AbstractCliTest {

    public static final int RUN_STATUS_OK = 0;
    public static final int HTTP_STATUS_OK = 200;
    public static final String TOKEN = "123456";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_PUT = "PUT";
    public static final String HTTP_DELETE = "DELETE";

    //REST URLS
    public static final String FILE_FIND_URL = "/catgenome/restapi/dataitem/find";
    public static final String REGISTRATION_URL = "/catgenome/restapi/%s/register";
    public static final String INDEXATION_URL = "/catgenome/restapi/%s/%d/index";
    public static final String SEARCH_URL = "/catgenome/restapi/dataitem/search";
    public static final String DATASET_REGISTRATION_URL = "/catgenome/restapi/project/save";
    public static final String DATASET_LOAD_BY_NAME_URL = "/catgenome/restapi/project/load";
    public static final String DATASET_LOAD_BY_ID_URL = "/catgenome/restapi/project/%d/load";
    public static final String DATASET_DELETE_URL = "/catgenome/restapi/project/%d";
    public static final String DATASET_ADDING_URL = "/catgenome/restapi/project/%d/add/%d";
    public static final String DATASET_REMOVE_URL = "/catgenome/restapi/project/%d/remove/%d";
    public static final String DATASET_LIST_URL = "/catgenome/restapi/project/loadMy";
    public static final String DATASET_TREE_URL = "/catgenome/restapi/project/tree";
    public static final String REFERENCE_LIST_URL = "/catgenome/restapi/reference/loadAll";
    public static final String FILE_DELETE_URL = "/catgenome/restapi/dataitem/delete";
    public static final String REF_REGISTRATION_URL = "/catgenome/restapi/secure/reference/register/fasta";
    public static final String REF_DELETION_URL = "/catgenome/restapi/secure/reference/register/fasta";
    public static final String GENE_ADDING_URL = "/catgenome/restapi/secure/reference/%d/genes";
    public static final String DATASET_MOVING_URL = "/catgenome/restapi/project/%d/move";
    public static final String  AUTHENTICATION_URL = "/catgenome/oauth/token";
    public static final String  AUTHENTICATION_PAYLOAD = "client_id=restapp&client_secret=restapp&username="
            + "default_admin&password=admin&grant_type=password&scope=read";
    public static final String URL_GENERATION_URL = "/catgenome/restapi/url";

    //server properties
    public static final String TEST_SERVER_PROPERTIES = "server.properties";
    public static final String AUTHENTICATION_PROPERTY = "authentication";
    public static final String AUTHENTICATION_URL_PROPERTY = "authentication_url";
    public static final String SERVER_URL_PROPERTY = "server_url";
    public static final String SEARCH_URL_PROPERTY = "search_url";
    public static final String REGISTRATION_URL_PROPERTY = "register_url";
    public static final String PROJECT_URL_PROPERTY = "project_url";
    public static final String PROJECT_URL_BY_ID_PROPERTY = "project_load_url";
    public static final String FILE_FIND_URL_PROPERTY = "find_url";
    public static final String VERSION_URL_PROPERTY = "version_url";
    public static final String SERVER_VERSION_PROPERTY = "minimum_server_version";
    public static final String PROJECT_TREE_PROPERTY = "project_load_tree";

    public static final String SERVER_URL = "http://localhost:%d/catgenome";

    private static ConfigurationLoader configLoader = new ConfigurationLoader();

    protected static ServerParameters getDefaultServerOptions(int port) {
        Properties serverProperties = configLoader.loadServerConfiguration(null);
        ServerParameters parameters = new ServerParameters();
        parameters.setServerUrl(String.format(SERVER_URL, port));
        parameters.setAuthPayload(serverProperties.getProperty(AUTHENTICATION_PROPERTY));
        parameters.setAuthenticationUrl(serverProperties.getProperty(AUTHENTICATION_URL_PROPERTY));
        parameters.setSearchUrl(serverProperties.getProperty(SEARCH_URL_PROPERTY));
        parameters.setRegistrationUrl(serverProperties.getProperty(REGISTRATION_URL_PROPERTY));
        parameters.setProjectLoadUrl(serverProperties.getProperty(PROJECT_URL_PROPERTY));
        parameters.setProjectLoadByIdUrl(serverProperties.getProperty(PROJECT_URL_BY_ID_PROPERTY));
        parameters.setFileFindUrl(serverProperties.getProperty(FILE_FIND_URL_PROPERTY));
        parameters.setVersionUrl(serverProperties.getProperty(VERSION_URL_PROPERTY));
        parameters.setProjectTreeUrl(serverProperties.getProperty(PROJECT_TREE_PROPERTY));
        parameters.setMinSupportedServerVersion(
                Integer.parseInt(serverProperties.getProperty(SERVER_VERSION_PROPERTY)));
        return parameters;
    }

    public CommandConfiguration getCommandConfiguration(String command) {
        return configLoader.loadCommandConfiguration(command);
    }

    public Properties loadServerProperties(int port) {
        Properties properties = loadProperties(TEST_SERVER_PROPERTIES);
        properties.setProperty(SERVER_URL_PROPERTY, String.format(SERVER_URL, port));
        return properties;
    }

    private static Properties loadProperties(String propertiesFileName) {
        try (InputStream stream = AbstractCliTest.class.getClassLoader().getResourceAsStream(propertiesFileName)) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new ApplicationException(
                    "Failed to load properties from file " + propertiesFileName, e);
        }
    }

    public AbstractHTTPCommandHandler createFileRegCommandHandler(ServerParameters serverParameters,
            String command) {
        FileRegistrationHandler handler = new FileRegistrationHandler();
        handler.setServerParameters(serverParameters);
        handler.setConfiguration(getCommandConfiguration(command));
        return handler;
    }
}
