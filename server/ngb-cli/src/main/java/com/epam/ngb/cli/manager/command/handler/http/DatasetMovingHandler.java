package com.epam.ngb.cli.manager.command.handler.http;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import java.net.URISyntaxException;
import java.util.List;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.request.RequestManager;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;

/**
 * {@code {@link DatasetMovingHandler}} represents a tool for handling 'move_dataset' command and
 * changing the hierarchy of a dataset. This command requires strictly one argument:
 * - dataset ID or name (data set to apply changes)
 * Option "parentId" is used to determine command result, if it is not specified, the dataset will be
 * moved to dataset top level (no parent at all), if option is set, its value will be set
 * as parentID to the processed dataset.
 */
@Command(type = Command.Type.REQUEST, command = {"move_dataset"})
public class DatasetMovingHandler extends AbstractHTTPCommandHandler {

    private Long datasetId;
    private Long parentId;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException(MessageConstants.getMessage(ILLEGAL_COMMAND_ARGUMENTS,
                    getCommand(), 1, arguments.size()));
        }
        datasetId = parseProjectId(arguments.get(0));
        if (options.getParent() != null) {
            parentId = parseProjectId(options.getParent());
        }
    }

    @Override
    public int runCommand() {
        try {
            String url = serverParameters.getServerUrl() + getRequestUrl();
            URIBuilder builder = new URIBuilder(String.format(url, datasetId));
            if (parentId != null) {
                builder.addParameter("parentId", String.valueOf(parentId));
            }
            HttpPut put = new HttpPut(builder.build());
            setDefaultHeader(put);
            if (isSecure()) {
                addAuthorizationToRequest(put);
            }
            RequestManager.executeRequest(put);
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
        return 0;
    }
}
