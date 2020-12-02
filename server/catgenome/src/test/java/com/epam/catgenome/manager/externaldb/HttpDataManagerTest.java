package com.epam.catgenome.manager.externaldb;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class HttpDataManagerTest {
    private static final String TEST_RESPONSE_BODY = "test response";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private HttpDataManager httpDataManager = new HttpDataManager();
    private String mockServerRootUrl;

    @Before
    public void setUp() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/test?testParam=testValue"))
                             .willReturn(WireMock.aResponse()
                                             .withBody(TEST_RESPONSE_BODY)));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/test2")).willReturn(WireMock.notFound()));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/test3"))
                             .willReturn(WireMock.aResponse().withBody("{\"error\":\"A server error\"}").withStatus(
                                 HttpStatus.BAD_REQUEST.value())));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/test4"))
                             .willReturn(WireMock.badRequest()));

        mockServerRootUrl = "http://localhost:" + wireMockRule.port();
    }

    @Test(expected = ExternalDbUnavailableException.class)
    public void testFetchDataFailOnUnexpectedStatus() throws ExternalDbUnavailableException {
        httpDataManager.fetchData(mockServerRootUrl + "/test2",
                                  new ParameterNameValue[]{});
    }

    @Test
    public void testFetchData() throws ExternalDbUnavailableException {
        String response = httpDataManager.fetchData(mockServerRootUrl + "/test?",
                                  new ParameterNameValue[]{new ParameterNameValue("testParam", "testValue")});
        Assert.assertEquals(TEST_RESPONSE_BODY, response.replaceAll("\n", "").trim());
    }

    @Test(expected = ExternalDbUnavailableException.class)
    public void testFetchDataFailOnBadRequest() throws ExternalDbUnavailableException {
        httpDataManager.fetchData(mockServerRootUrl + "/test3",
                                  new ParameterNameValue[]{});
    }

    @Test(expected = ExternalDbUnavailableException.class)
    public void testFetchDataFailOnBadRequest2() throws ExternalDbUnavailableException {
        httpDataManager.fetchData(mockServerRootUrl + "/test4",
                                  new ParameterNameValue[]{});
    }
}