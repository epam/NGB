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

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.manager.command.CommandConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class ConfigurationLoaderTest extends AbstractCliTest{

    private static final String SEARCH_COMMAND_SHORT = "s";
    private static final String SEARCH_COMMAND_FULL = "search";
    private static final String EXTERNAL_CONFIG = "server_ext.properties";
    private static final int DEFAULT_PORT = 8080;
    private static final int EXTERNAL_PORT = 15666;

    @Test
    public void loadServerConfiguration() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        Properties serverConfiguration = loader.loadServerConfiguration(null);
        Assert.assertTrue(serverConfiguration != null);
        Assert.assertEquals(String.format(SERVER_URL, DEFAULT_PORT),
                serverConfiguration.getProperty(SERVER_URL_PROPERTY));
    }

    @Test
    public void loadServerConfigurationExternal() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        InputStream configFile =
                this.getClass().getResourceAsStream("/" + EXTERNAL_CONFIG);
        Path path = Files.createTempFile(EXTERNAL_CONFIG, EXTERNAL_CONFIG);
        Files.copy(configFile, path, StandardCopyOption.REPLACE_EXISTING);
        Properties serverConfiguration = loader.loadServerConfiguration(path.toString());
        Assert.assertTrue(serverConfiguration != null);
        Assert.assertEquals(String.format(SERVER_URL, EXTERNAL_PORT),
                serverConfiguration.getProperty(SERVER_URL_PROPERTY));
    }

    @Test
    public void loadServerConfigurationWrongExternal() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        Properties serverConfiguration = loader.loadServerConfiguration(EXTERNAL_CONFIG);
        Assert.assertTrue(serverConfiguration != null);
        Assert.assertEquals(String.format(SERVER_URL, DEFAULT_PORT),
                serverConfiguration.getProperty(SERVER_URL_PROPERTY));
    }

    @Test
    public void loadCommandProperties() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        Properties properties = loader.loadCommandProperties();
        Assert.assertTrue(properties != null);
        Assert.assertEquals(SEARCH_COMMAND_FULL, properties.getProperty(SEARCH_COMMAND_SHORT));
        Assert.assertEquals(SEARCH_COMMAND_FULL, properties.getProperty(SEARCH_COMMAND_FULL));
    }

    @Test
    public void loadCommandConfiguration() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        CommandConfiguration commandConfiguration = loader.loadCommandConfiguration(SEARCH_COMMAND_FULL);
        Assert.assertEquals(SEARCH_COMMAND_FULL, commandConfiguration.getName());
        Assert.assertEquals(HTTP_GET, commandConfiguration.getRequestType());
    }

    @Test
    public void loadCommandConfigurationWrongCommand() throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        CommandConfiguration commandConfiguration =
                loader.loadCommandConfiguration(SEARCH_COMMAND_FULL + "/");
        Assert.assertTrue(commandConfiguration.getName() == null);
    }
}
