/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.ngb.cli.manager.command.handler.simple;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.app.ConfigurationLoader;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * Represents a {@code SetPropertyCommandHandler} for commands, that set properties to configuration file and
 * don't include interaction with NGB server
 */
public class SetPropertyCommandHandler extends AbstractSimpleCommandHandler {

    private Properties properties;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.isEmpty() || arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
    }

    @Override
    public int runCommand() {
        return 0;
    }

    protected void addPropertyToConfigurationFile(String propertyName, String propertyValue) {
        ConfigurationLoader loader = new ConfigurationLoader();
        Properties properties = loader.loadServerConfiguration(null);
        properties.setProperty(propertyName, propertyValue);
        this.properties = properties;
        try {
            File extProps = loader.getConfigFile();
            try (OutputStream stream = new FileOutputStream(extProps)) {
                properties.store(stream, "");
            }
        } catch (IOException e) {
            throw new ApplicationException("Failed to save property " + propertyName + " to config file", e);
        }
    }

    Properties getProperties() {
        return properties;
    }
}
