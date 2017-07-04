package com.epam.ngb.cli.manager.command.handler.simple;

import java.util.Collections;

import com.epam.ngb.cli.AbstractCliTest;
import com.epam.ngb.cli.app.ApplicationOptions;
import org.junit.Assert;
import org.junit.Test;

public class VersionCommandHandlerTest extends AbstractCliTest {

    private static final String COMMAND = "version";

    @Test
    public void testSetServer() throws Exception {
        VersionCommandHandler handler = new VersionCommandHandler();
        handler.setConfiguration(getCommandConfiguration(COMMAND));
        handler.parseAndVerifyArguments(Collections.emptyList(), new ApplicationOptions());
        Assert.assertEquals(RUN_STATUS_OK, handler.runCommand());
    }
}
