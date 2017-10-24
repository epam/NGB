package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.List;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

/**
 * {@code {@link SpeciesAddingHandler}} represents a tool for handling 'add_species' command and
 * adding a species to a reference, registered on NGB server. This command requires strictly two arguments:
 * - reference ID or name
 * - species version (should be registered on NGB server before)
 */
@Command(type = Command.Type.REQUEST, command = {"add_species"})
public class SpeciesAddingHandler extends AbstractHTTPCommandHandler {

    private Long referenceId;
    private String speciesVersion;

    /**
     * If true command will output result of species addition in json format
     */
    private boolean printJson;
    /**
     * If true command will output result of species addition in table format
     */
    private boolean printTable;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 2) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 2, arguments.size()));
        }
        referenceId = loadReferenceId(arguments.get(0));
        speciesVersion = StringUtils.trim(arguments.get(1));
        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
    }

    @Override
    public int runCommand() {
        try {
            String url = serverParameters.getServerUrl() + getRequestUrl();
            URIBuilder builder = new URIBuilder(String.format(url, referenceId));
            if (speciesVersion != null) {
                builder.addParameter("speciesVersion", speciesVersion);
            }
            HttpPut put = new HttpPut(builder.build());
            setDefaultHeader(put);
            if (isSecure()) {
                addAuthorizationToRequest(put);
            }
            String result = RequestManager.executeRequest(put);
            checkAndPrintRegistrationResult(result, printJson, printTable);
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
