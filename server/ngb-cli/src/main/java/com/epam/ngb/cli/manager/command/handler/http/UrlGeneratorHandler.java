package com.epam.ngb.cli.manager.command.handler.http;

import static com.epam.ngb.cli.constants.MessageConstants.MINIMUM_COMMAND_ARGUMENTS;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;

import com.epam.ngb.cli.entity.UrlWithAliasItem;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;

import com.epam.ngb.cli.app.ApplicationOptions;
import com.epam.ngb.cli.constants.MessageConstants;
import com.epam.ngb.cli.exception.ApplicationException;
import com.epam.ngb.cli.manager.command.handler.Command;
import com.epam.ngb.cli.manager.printer.AbstractResultPrinter;
import com.epam.ngb.cli.manager.request.RequestManager;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Source:      UrlGeneratorHandler
 * Created:     31.01.17, 12:03
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 15.0.3, JDK 1.8
 *
 * @author Mikhail Miroliubov
 */
@Command(type = Command.Type.REQUEST, command = {"generate_url"}) public class UrlGeneratorHandler
        extends AbstractHTTPCommandHandler {

    private static final String SHORT_URL_FORMAT = "%s/restapi/navigate?alias=%s";

    private List<String> ids;
    private String dataset;
    private String chrName;
    private Integer startIndex;
    private Integer endIndex;
    private boolean alias;

    /**
     * If true command will output result of dataset registration in a json format
     */
    private boolean printJson;
    /**
     * If true command will output result of dataset registration in a table format
     */
    private boolean printTable;

    @Override
    public void parseAndVerifyArguments(List<String> arguments, ApplicationOptions options) {
        if (CollectionUtils.isEmpty(arguments)) {
            throw new IllegalArgumentException(MessageConstants
                    .getMessage(MINIMUM_COMMAND_ARGUMENTS, getCommand(), 1, arguments.size()));
        }

        dataset = arguments.get(0);
        ids = arguments.subList(1, arguments.size());

        printJson = options.isPrintJson();
        printTable = options.isPrintTable();
        alias = options.isAlias();

        String location = options.getLocation();
        if (StringUtils.isNoneBlank(location)) {
            String[] parts = location.split(":");
            chrName = parts[0];

            if (parts.length > 1 && parts[1].contains("-")) {
                String[] subParts = parts[1].split("-");
                if (NumberUtils.isDigits(subParts[0])) {
                    startIndex = Integer.parseInt(subParts[0]);
                }
                if (NumberUtils.isDigits(subParts[1])) {
                    endIndex = Integer.parseInt(subParts[1]);
                }
            }
        }
    }

    @Override public int runCommand() {
        //AbstractResultPrinter printer = AbstractResultPrinter.getPrinter(false, "%s");
        try {
            URIBuilder builder = new URIBuilder(serverParameters.getServerUrl() + getRequestUrl());
            if (chrName != null) {
                builder.addParameter("chromosomeName", chrName);
            }
            if (startIndex != null) {
                builder.addParameter("startIndex", startIndex.toString());
            }
            if (endIndex != null) {
                builder.addParameter("endIndex", endIndex.toString());
            }
            HttpPost request = (HttpPost) getRequestFromURLByType(HttpPost.METHOD_NAME, builder.build().toString());
            request.setEntity(
                    new StringEntity(getMapper().writeValueAsString(new UrlRequest(dataset, ids))));

            String result = RequestManager.executeRequest(request);
            String url = getUrl(result);

            url = serverParameters.getServerUrl() + url;

            if (alias && StringUtils.isNotBlank(url)) {
                final UrlWithAliasItem urlWithAliasItem = UrlWithAliasItem.builder()
                        .url(url)
                        .build();
                builder = new URIBuilder(serverParameters.getServerUrl() +
                        serverParameters.getGenerateShortUrlUrl());
                request = (HttpPost) getRequestFromURLByType(HttpPost.METHOD_NAME, builder.build().toString());
                result = getPostResult(urlWithAliasItem, request);

                url = getUrl(result);
                url = String.format(SHORT_URL_FORMAT, serverParameters.getServerUrl(), url);
            }

            AbstractResultPrinter printer;
            if (!printJson && !printTable) {
                printer = AbstractResultPrinter.getPrinter(true, "%s");
            } else {
                printer = AbstractResultPrinter.getPrinter(printTable, "%s");
            }
            printer.printSimple(url);
            return 0;
        } catch (URISyntaxException | JsonProcessingException | UnsupportedEncodingException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    private String getUrl(final String result) {
        isResultOk(result);
        return getResult(result, String.class);
    }

    private static final class UrlRequest {
        String dataset;
        private List<String> ids;

        private UrlRequest(String dataset, List<String> ids) {
            this.dataset = dataset;
            this.ids = ids;
        }

        public String getDataset() {
            return dataset;
        }

        public void setDataset(String dataset) {
            this.dataset = dataset;
        }

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }
    }
}
