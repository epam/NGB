/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.manager.externaldb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <p>
 * A service class that manages http connections to external databases
 * </p>
 */
@Service
public class HttpDataManager {
    private static final String EXCEPTION_MESSAGE = "Couldn't fetch data for URL %s";
    private static final int MILLIS_IN_SECOND = 1000;
    private static final String HTTP_HEADER_RETRY_AFTER = "Retry-After";
    private static final Logger LOGGER = Logger.getLogger(HttpDataManager.class.getName());
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String WAITING_FORMAT = "Waiting (%d)...";
    private static final int MAX_HTTP_OK_STATUS = 299;
    private static final int MIN_HTTP_OK_STATUS = 200;


    @Value("#{catgenome['externaldb.proxy.host'] ?: null}")
    private String proxyHost;
    @Value("#{catgenome['externaldb.proxy.port'] ?: null}")
    private Integer proxyPort;
    @Value("#{catgenome['externaldb.proxy.user'] ?: null}")
    private String proxyUser;
    @Value("#{catgenome['externaldb.proxy.password'] ?: null}")
    private String proxyPassword;


    /**
     * Performs HTTP connection to a given URL and delegates processing of input stream to an abstract
     * method processStream()
     *
     * @param locationStub target URL stub
     * @return String with data
     * @throws ExternalDbUnavailableException
     */
    public String fetchData(String locationStub, ParameterNameValue[] params)
            throws ExternalDbUnavailableException {

        final String location = getLocationStub(locationStub, params);

        String resultData = getResultFromURL(location);


        if (StringUtils.isBlank(resultData)) {
            throw new ExternalDbUnavailableException(String.format(EXCEPTION_MESSAGE, location));
        }

        return resultData;
    }

    /**
     *
     * @param location target URL stub
     * @param object Json object
     * @return String with data
     * @throws ExternalDbUnavailableException
     */
    public String fetchData(String location, JSONObject object) throws ExternalDbUnavailableException {
        String resultData = getResultFromHttp(location, object);
        if (StringUtils.isBlank(resultData)) {
            throw new ExternalDbUnavailableException(String.format(EXCEPTION_MESSAGE, location));
        }
        return resultData;
    }


    private String getLocationStub(String locationStub, ParameterNameValue[] params) {
        StringBuilder locationBuilder = new StringBuilder(locationStub);

        if (params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    locationBuilder.append('&');
                }
                locationBuilder.append(params[i].getName()).append('=').append(params[i].getValue());
            }
        }
        return locationBuilder.toString();
    }

    private String getResultFromURL(final String location) throws ExternalDbUnavailableException {

        HttpURLConnection conn = null;
        try {
            conn = createConnection(location);
            HttpURLConnection.setFollowRedirects(true);
            conn.setDoInput(true);
            conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
            conn.connect();

            int status = conn.getResponseCode();

            while (true) {
                long wait = 0;
                String header = conn.getHeaderField(HTTP_HEADER_RETRY_AFTER);

                if (header != null) {
                    wait = Integer.valueOf(header);
                }

                if (wait == 0) {
                    break;
                }

                LOGGER.info(String.format(WAITING_FORMAT, wait));
                conn.disconnect();

                Thread.sleep(wait * MILLIS_IN_SECOND);

                conn = (HttpURLConnection) new URL(location).openConnection();
                conn.setDoInput(true);
                conn.connect();
                status = conn.getResponseCode();
            }
            return getHttpResult(status, location, conn);
        } catch (InterruptedException | IOException e) {
            throw new ExternalDbUnavailableException(String.format(EXCEPTION_MESSAGE, location), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }

            resetProxy();
        }
    }

    private void resetProxy() {
        if (proxyUser != null && proxyPassword != null) {
            Authenticator.setDefault(null);
        }
    }

    private HttpURLConnection createConnection(final String location) throws IOException {

        URL url = new URL(location);
        HttpURLConnection conn;

        if (proxyHost != null && proxyPort != null) {
            if (proxyUser != null && proxyPassword != null) {
                Authenticator authenticator = new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                };
                Authenticator.setDefault(authenticator);
            }
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            conn = (HttpURLConnection) url.openConnection(proxy);
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        return conn;
    }

    private String getHttpResult(final int status, final String location, final HttpURLConnection conn)
        throws IOException, ExternalDbUnavailableException {
        switch (status) {
            case HttpURLConnection.HTTP_OK:
                LOGGER.info("HTTP_OK reply from destination server");
                return fetchContent(conn, status);
            case HttpURLConnection.HTTP_BAD_REQUEST:
                try {
                    Map<String, Object> errorPayload = new ObjectMapper().readValue(fetchContent(conn, status),
                                                                                    new TypeReference<Map<String, Object>>(){});
                    throw new ExternalDbUnavailableException(
                        errorPayload.getOrDefault("error",
                                                  "External DB thrown an error with code: " + status)
                            .toString());
                } catch (JsonMappingException e) {
                    throw new ExternalDbUnavailableException("External DB thrown an error with code: " + status);
                }

            default:
                LOGGER.severe("Unexpected HTTP status:" + conn.getResponseMessage() + " for " + location);
                throw new ExternalDbUnavailableException(
                    String.format("Unexpected HTTP status: %d %s for URL %s", status,
                                  conn.getResponseMessage(), location));
        }
    }

    private String fetchContent(final HttpURLConnection conn, int status) throws ExternalDbUnavailableException {
        boolean successful = status <= MAX_HTTP_OK_STATUS && status >= MIN_HTTP_OK_STATUS;
        try (BufferedReader streamReader = new BufferedReader(new InputStreamReader(
            successful ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"))) {
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr).append('\n');
            }

            return responseStrBuilder.toString();
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(
                "Could not get any response from destination server.Response status: " + status, e);
        }
    }

    private String getResultFromHttp(final String location, final JSONObject object)
            throws ExternalDbUnavailableException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(location);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);

            OutputStreamWriter stream = new OutputStreamWriter(conn.getOutputStream(), Charset.defaultCharset());
            stream.write(object.toString());
            stream.flush();

            int status = conn.getResponseCode();

            while (true) {
                String header = conn.getHeaderField(HTTP_HEADER_RETRY_AFTER);
                long wait = 0;

                if (header != null) {
                    wait = Integer.valueOf(header);
                }

                if (wait == 0) {
                    break;
                }

                LOGGER.info(String.format(WAITING_FORMAT, wait));
                conn.disconnect();

                Thread.sleep(wait * MILLIS_IN_SECOND);

                conn = (HttpURLConnection) new URL(location).openConnection();
                conn.setDoInput(true);
                conn.connect();
                status = conn.getResponseCode();
            }
            return getHttpResult(status, location, conn);
        } catch (IOException | InterruptedException | ExternalDbUnavailableException e) {
            throw new ExternalDbUnavailableException(String.format(EXCEPTION_MESSAGE, location), e);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

    }
}