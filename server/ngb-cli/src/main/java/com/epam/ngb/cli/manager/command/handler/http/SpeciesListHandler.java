package com.epam.ngb.cli.manager.command.handler.http;

import static com.epam.ngb.cli.constants.MessageConstants.ILLEGAL_COMMAND_ARGUMENTS;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.ResponseResult;
import com.epam.ngb.cli.entity.SpeciesEntity;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import java.io.IOException;
import java.util.List;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *{@code {@link SpeciesListHandler}} represents a tool for handling 'list_species' command and
 * listing species, registered on NGB server. This command doesn't support input arguments.
 */
@Command(type = Command.Type.REQUEST, command = {"list_species"})
public class SpeciesListHandler extends AbstractHTTPCommandHandler {

	private boolean printTable;

	private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesListHandler.class);

	@Override
	public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
		if (!arguments.isEmpty()) {
			throw new IllegalArgumentException(MessageConstants.getMessage(
				ILLEGAL_COMMAND_ARGUMENTS, getCommand(), 0, arguments.size()));
		}
		this.printTable = options.isPrintTable();
	}

	@Override
	public int runCommand() {
		HttpRequestBase request = getRequest(getRequestUrl());
		setDefaultHeader(request);
		if (isSecure()) {
			addAuthorizationToRequest(request);
		}
		String result = RequestManager.executeRequest(request);
		ResponseResult<List<SpeciesEntity>> responseResult;
		try {
			responseResult = getMapper().readValue(result,
				getMapper().getTypeFactory().constructParametrizedType(
					ResponseResult.class, ResponseResult.class,
					getMapper().getTypeFactory().constructParametrizedType(
						List.class, List.class, SpeciesEntity.class)));
		} catch (IOException e) {
			throw new ApplicationException(e.getMessage(), e);
		}
		if (ERROR_STATUS.equals(responseResult.getStatus())) {
			throw new ApplicationException(responseResult.getMessage());
		}
		if (responseResult.getPayload() == null ||
			responseResult.getPayload().isEmpty()) {
			LOGGER.info("No species registered on the server.");
		} else {
			List<SpeciesEntity> items = responseResult.getPayload();
			AbstractResultPrinter printer = AbstractResultPrinter
				.getPrinter(printTable, items.get(0).getFormatString(items));
			printer.printHeader(items.get(0));
			items.forEach(printer::printItem);
		}
		return 0;
	}
}
