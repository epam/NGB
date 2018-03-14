package com.epam.catgenome.app;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringJUnit4ClassRunner.class)
@Import({JWTSecurityConfiguration.class})
@TestPropertySource(locations = "classpath:test-catgenome-auth.properties")
@ContextConfiguration({"classpath:applicationContext-test.xml", "classpath:catgenome-servlet-test.xml"})
@WebAppConfiguration
@EnableWebSecurity
public class JwtAuthenticationTest {

    @Value("${jwt.token.test.valid}")
    private String validToken;

    @Value("${jwt.token.test.invalid.claims}")
    private String invalidClaimsToken;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private FilterChainProxy filterChainProxy;

    private static final String INVALID_TOKEN = "1234556";
    private static final String BEARER = "Bearer ";
    private static final String AUTH = "Authorization";

    @Before
    public void setup() {
        assertNotNull("WebApplicationContext isn't provided.", wac);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(filterChainProxy).build();
        System.out.println();
    }

    private static final String TEST_URL = "/restapi/token/test";

    @Test
    public void cannotAccessWithoutToken() throws Exception {
        mockMvc.perform(get(TEST_URL))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void canAccessWithValidToken() throws Exception {
        mockMvc.perform(get(TEST_URL).header(AUTH, BEARER + validToken))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void cannotAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get(TEST_URL).header(AUTH, BEARER + INVALID_TOKEN))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void cannotAccessWithInvalidClaims() throws Exception {
        mockMvc.perform(get(TEST_URL).header(AUTH, BEARER + invalidClaimsToken))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

}