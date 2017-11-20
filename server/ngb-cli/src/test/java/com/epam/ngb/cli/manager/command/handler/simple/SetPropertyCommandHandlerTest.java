package com.epam.ngb.cli.manager.command.handler.simple;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.exception.ApplicationException;

import java.util.Arrays;
import java.util.Collections;

public class SetPropertyCommandHandlerTest extends AbstractCliTest {

    protected SetPropertyCommandHandler initCommandHandler(SetPropertyCommandHandler handler,
                                                           String command, String propertyValue) {
        handler.setConfiguration(getCommandConfiguration(command));
        try {
            //this will always fail, as we cannot save configuration to an external file in test
            //as this command works only in distribution
            handler.parseAndVerifyArguments(Collections.singletonList(propertyValue), new ApplicationOptions());
            handler.runCommand();
        } catch (ApplicationException e) {
            return handler;
        }
        return handler;
    }

    protected SetPropertyCommandHandler initNoArgsCommandHandler(SetPropertyCommandHandler handler,
                                                                 String command) {
        handler.setConfiguration(getCommandConfiguration(command));
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
        return handler;
    }

    protected SetPropertyCommandHandler initTooManyArgsCommandHandler(SetPropertyCommandHandler handler,
                                                                      String command, String propertyValue) {
        handler.setConfiguration(getCommandConfiguration(command));
        handler.parseAndVerifyArguments(Arrays.asList(propertyValue, propertyValue), new ApplicationOptions());
        return handler;
    }

}