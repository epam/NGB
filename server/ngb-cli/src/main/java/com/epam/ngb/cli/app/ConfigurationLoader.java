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

package com.epam.ngb.cli.app;

import static com.epam.ngb.cli.constants.MessageConstants.ERROR_LOAD_CONFIG_FILE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Properties;


import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.CommandConfiguration;

/**
 * {@code ConfigurationLoader} is a service class for loading application configurations.
 */
public class ConfigurationLoader {

    /**
     * Path to the server properties file, holding the default server configuration.
     * During installation copy of this file is put into a 'config' folder to allow
     * changing server configuration without rebuilding the application.
     */
    private static final String SERVER_PROPERTIES = "/external/server.properties";

    /**
     * Path to the external properties file, holding the inner server configuration.
     * This file is available in a external folder 'config' in the application
     * installation folder and allows changing server configuration without rebuilding
     * the application.
     */
    private static final String SERVER_EXT_PROPERTIES = "/config/server.properties";

    /**
     * Path to the properties file containing mapping of the input command line arguments
     * to the application commands. Several arguments might be mapped to one application
     * command. These properties allow binding of the commands to application classes that
     * handle actual work.
     */
    private static final String COMMAND_PROPERTIES = "/command.properties";

    /**
     * Path to the XML configuration file for application commands. This file contains
     * information required for initializing and running commands: type of the command,
     * type of request and URL (for NGB server commands) and any supplementary data.
     */
    private static final String COMMAND_XML_CONFIG = "/command-configuration.xml";

    private static final String COMMAND_NAME_PROPERTY = ".name";
    private static final String COMMAND_URL_PROPERTY = ".url";
    private static final String COMMAND_TYPE_PROPERTY = ".type";
    private static final String COMMAND_SECURE_PROPERTY = ".secure";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoader.class);

    /**
     * Default constructor
     */
    public ConfigurationLoader() {
        //no op
    }

    /**
     * Loads the server configuration from a file. Configuration can be loaded from several
     * sources with the following priority:
     *  1.  Properties file can be specified explicitly by {@param configFile}, firstly
     *      method checks if {@param configFile} is not null and then tries to load
     *      properties from it
     *  2.  If {@param configFile} is not specified, method tries to load properties from
     *      a 'config' folder in the application installation directory
     *  3.  If both {@param configFile} and external properties file are unavailable, the default
     *      properties file from application resources is loaded
     * @param configFile path to a server properties file, it will override any default
     *                   properties
     * @return loaded server properties
     */
    public Properties loadServerConfiguration(final String configFile) {
        if (configFile != null && !configFile.isEmpty()) {
            File extConfig = new File(configFile);
            if (extConfig.exists()) {
                return getPropertiesFromFile(extConfig);
            }
        }
        try {
            File extProps = getConfigFile();
            if (extProps.exists()) {
                return getPropertiesFromFile(extProps);
            }
        } catch (UnsupportedEncodingException | ApplicationException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        LOGGER.error("Using default properties");
        return loadProperties(SERVER_PROPERTIES);
    }

    /**
     * Loads file with command mapping from application resources. These properties
     * provide mapping from command line input arguments to application commands.
     * @return properties with command mapping
     */
    public Properties loadCommandProperties() {
        return loadProperties(COMMAND_PROPERTIES);
    }

    /**
     * Loads XML configuration file with application commands. This file contains
     * information required for initializing and running commands: type of the command,
     * type of request and URL (for NGB server commands) and any supplementary data
     * @param fullCommand name of the command to load configuration for
     * @return configuration for the specifies command
     */
    public CommandConfiguration loadCommandConfiguration(String fullCommand) {
        Configuration configuration = loadXmlConfiguration(COMMAND_XML_CONFIG);
        String name = configuration.getString(fullCommand + COMMAND_NAME_PROPERTY);
        String url = configuration.getString(fullCommand + COMMAND_URL_PROPERTY);
        String type = configuration.getString(fullCommand + COMMAND_TYPE_PROPERTY);
        boolean secure = false;
        if (configuration.getString(fullCommand + COMMAND_SECURE_PROPERTY) != null) {
            secure = configuration.getBoolean(fullCommand + COMMAND_SECURE_PROPERTY);
        }
        return new CommandConfiguration(name, url, type, secure);
    }

    /**
     * Returns external properties file, holding server configuration. This file
     * is assumed to be located in 'config' folder of the application installation directory.
     * Path to file is found relatively to the location of the jar file with the
     * {@code Application} class
     * @return file with external server properties
     * @throws UnsupportedEncodingException
     */
    public File getConfigFile() throws UnsupportedEncodingException {
        String path =
                Application.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        String decodedPath = URLDecoder.decode(path, Charset.defaultCharset().displayName());
        String dir = new File(decodedPath).getParentFile().getParentFile().getAbsolutePath();
        File configFile = new File(dir + SERVER_EXT_PROPERTIES);
        if (!configFile.exists()) {
            throw new ApplicationException(MessageConstants.getMessage(ERROR_LOAD_CONFIG_FILE,
                    configFile.getName()));
        }
        return configFile;
    }

    private Properties loadProperties(String propertiesFileName) {
        try (InputStream stream = getClass().getResourceAsStream(propertiesFileName)) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new ApplicationException(
                    MessageConstants.getMessage(ERROR_LOAD_CONFIG_FILE,
                            propertiesFileName), e);
        }
    }

    private Properties getPropertiesFromFile(File config) {
        try (InputStream stream = new FileInputStream(config)) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new ApplicationException(MessageConstants.getMessage(ERROR_LOAD_CONFIG_FILE,
                    config), e);
        }
    }

    private Configuration loadXmlConfiguration(String filename) {
        XMLConfiguration config;
        try {
            config = new XMLConfiguration(getClass().getResource(filename));
        } catch (ConfigurationException e) {
            throw new ApplicationException(
                    MessageConstants.getMessage(ERROR_LOAD_CONFIG_FILE, filename), e);
        }
        return config;
    }
}
