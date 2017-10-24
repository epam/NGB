package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * {@code {@link SpeciesRemovingHandler}} represents a tool for handling 'remove_species' command and
 * removing species from a reference, registered on NGB server. This command requires strictly one argument:
 * - reference ID or name
 */
@Command(type = Command.Type.REQUEST, command = {"remove_species"})
public class SpeciesRemovingHandler extends AbstractHTTPCommandHandler {

    private Long referenceId;

    /**
     * If true command will output result of species removing in json format
     */
    private boolean printJson;
    /**
     * If true command will output result of species removing in table format
     */
    private boolean printTable;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        referenceId = loadReferenceId(arguments.get(0));
        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        HttpRequestBase request = getRequest(String.format(getRequestUrl(), referenceId));
        setDefaultHeader(request);
        if (isSecure()) {
            addAuthorizationToRequest(request);
        }
        String result = RequestManager.executeRequest(request);
        checkAndPrintRegistrationResult(result, printJson, printTable);
        return 0;
    }
}
