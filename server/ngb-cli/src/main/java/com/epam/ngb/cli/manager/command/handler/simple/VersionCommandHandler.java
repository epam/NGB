package com.epam.ngb.cli.manager.command.handler.simple;

import static com.epam.ngb.cli.manager.command.CommandManager.SERVER_VERSION_PROPERTY;

import java.util.List;
import java.util.Properties;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.app.ConfigurationLoader;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.command.handler.http.FileRegistrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(type = Command.Type.SIMPLE, command = {"version"})
public class VersionCommandHandler extends AbstractSimpleCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRegistrationHandler.class);


    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (!arguments.isEmpty()) {
            LOGGER.debug("Version command doesn't consume arguments. "
                    + "All input arguments will be ignored.");
        }
    }

    @Override
    public int runCommand() {
        ConfigurationLoader loader = new ConfigurationLoader();
        Properties properties = loader.loadServerConfiguration(null);
        LOGGER.info(properties.getProperty(SERVER_VERSION_PROPERTY));
        return 0;
    }
}
