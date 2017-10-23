package com.epam.ngb.cli.manager.command.handler.http;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.entity.RegistrationRequest;
import com.epam.ngb.cli.entity.SpeciesEntity;
import com.epam.ngb.cli.manager.command.handler.Command;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

/**
 * {@code {@link SpeciesRegistrationHandler}} represents a tool handling "register_species" command and
 * sending request to NGB server for species registration. This command requires strictly two argument:
 * name of the species and version.
 * By default the command doesn't produce any output, but it can be turned on by setting {@code true}
 * value to {@code printJson} or {@code printTable} fields.
 */
@Command(type = Command.Type.REQUEST, command = {"register_species"})
public class SpeciesRegistrationHandler extends AbstractHTTPCommandHandler {

	/**
	 * Species representation.
	 */
	private SpeciesEntity entity;

	/**
	 * If true command will output result of species registration in a json format
	 */
	private boolean printJson;

	/**
	 * If true command will output result of species registration in a table format
	 */
	private boolean printTable;

	@Override
	public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
		if (arguments.isEmpty() || arguments.size() != 2) {
			throw new IllegalArgumentException(MessageConstants.getMessage(MessageConstants.ILLEGAL_COMMAND_ARGUMENTS,
				getCommand(), 2, arguments.size()));
		}
		entity = new SpeciesEntity();
		entity.setName(StringUtils.trim(arguments.get(0)));
		entity.setVersion(StringUtils.trim(arguments.get(1)));

		printJson = options.isPrintJson();
		printTable = options.isPrintTable();
	}

	@Override
	public int runCommand() {
		HttpRequestBase request = getRequest(getRequestUrl());
		setDefaultHeader(request);
		if (isSecure()) {
			addAuthorizationToRequest(request);
		}
		RegistrationRequest registration = new RegistrationRequest();
		registration.setSpecies(entity);
		String result = getPostResult(registration, (HttpPost) request);
		checkAndPrintRegistrationResult(result, printJson, printTable);
		return 0;
	}
}
