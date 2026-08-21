/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.app;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.epam.catgenome.security.jwt.JwtAuthenticationProvider;
import com.epam.catgenome.security.jwt.JwtFilterAuthenticationFilter;
import com.epam.catgenome.security.jwt.JwtTokenVerifier;
import com.epam.catgenome.security.jwt.RestAuthenticationEntryPoint;

/**
 * Class provides JWT Security Configuration for Spring Boot application according to property file
 *
 * <p>Spring Security 6 removed {@code WebSecurityConfigurerAdapter}, so the chain this class used to
 * configure by overriding {@code configure(HttpSecurity)} is now a {@link SecurityFilterChain} bean.
 * Two consequences worth knowing about:
 *
 * <ul>
 *   <li>{@code @Order} belongs on the bean, not on the configuration class. The JWT chain keeps the
 *       first slot it has always had - ahead of the SAML chain ({@link SAMLSecurityConfiguration},
 *       order 2), which matches every request and therefore has to come last.</li>
 *   <li>The list of unsecured resources the old configuration carried
 *       ({@code /swagger-ui/**}, {@code /}, {@code /index.html}, ...) is not repeated here. It could
 *       never have had any effect: the chain is scoped to {@code /restapi/**} by
 *       {@code securityMatcher}, and none of those paths is under {@code /restapi}. They are
 *       permitted by {@link SAMLSecurityConfiguration}, which is the chain that actually sees them,
 *       and when SAML is off nothing guards them at all.</li>
 * </ul>
 *
 * <p>The dependency on the SAML {@code SAMLEntryPoint} bean is gone too. {@code /restapi/navigate} is
 * the one REST path that is opened in a browser rather than called by a client, so an unauthenticated
 * request to it has to start an interactive login instead of returning 401. Spring Security 6 installs
 * the SAML redirect entry point on the SAML chain, but this chain matches {@code /restapi/**} first,
 * so it needs its own: a plain {@link LoginUrlAuthenticationEntryPoint} pointed at the SP-internal
 * AuthnRequest endpoint. That is a URL, not a bean, so JWT no longer needs SAML in the context.
 */
@Configuration
@ConditionalOnProperty(prefix = "jwt.security.", name = "enable", havingValue = "true")
@ComponentScan(basePackages = {"com.epam.catgenome.security.jwt"})
public class JWTSecurityConfiguration {

    /**
     * Ahead of the SAML chain (2) and the anonymous one ({@link NoSecurityConfiguration}, 3).
     */
    private static final int CHAIN_ORDER = 1;

    private static final String CLAIM_DELIMITER = "=";

    private static final String ROUTE_URL = "/restapi/navigate";

    private static final String SECURED_RESOURCES = "/restapi/**";

    @Value("${jwt.key.public}")
    private String publicKey;

    @Value("#{'${jwt.required.claims}'.split(',')}")
    private List<String> requiredClaims;

    @Value("${security.frame-options.disable:false}")
    private boolean frameOptionsDisable;

    /**
     * {@code true} when {@link SAMLSecurityConfiguration} is active, in which case there is an
     * interactive login to redirect {@link #ROUTE_URL} to.
     */
    @Value("${saml.security.enable:false}")
    private boolean samlEnabled;

    protected String getPublicKey() {
        return publicKey;
    }

    @Bean
    @Order(CHAIN_ORDER)
    public SecurityFilterChain jwtFilterChain(final HttpSecurity http) throws Exception {
        http.securityMatcher(getSecuredResources())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .defaultAuthenticationEntryPointFor(interactiveEntryPoint(),
                                PathPatternRequestMatcher.withDefaults().matcher(ROUTE_URL))
                        .defaultAuthenticationEntryPointFor(unauthorizedEntryPoint(),
                                PathPatternRequestMatcher.withDefaults().matcher(getSecuredResources())))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers
                        .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .addFilterBefore(getJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        if (frameOptionsDisable) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        }

        return http.build();
    }

    /**
     * {@code defaultAuthenticationEntryPointFor} keeps the entry points in the order they are
     * registered and {@code DelegatingAuthenticationEntryPoint} takes the first whose matcher accepts
     * the request, so the narrow {@link #ROUTE_URL} matcher has to be registered before the catch-all
     * one for it to be reachable at all.
     */
    private AuthenticationEntryPoint interactiveEntryPoint() {
        if (samlEnabled) {
            return new LoginUrlAuthenticationEntryPoint(SAMLSecurityConfiguration.AUTHENTICATION_REQUEST_URL);
        }
        return unauthorizedEntryPoint();
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    @Bean
    protected JwtAuthenticationProvider jwtAuthenticationProvider() {
        return new JwtAuthenticationProvider(jwtTokenVerifier());
    }

    @Bean
    public JwtTokenVerifier jwtTokenVerifier() {
        return new JwtTokenVerifier(getPublicKey(), splitRequiredClaims());
    }

    protected JwtFilterAuthenticationFilter getJwtAuthenticationFilter() {
        return new JwtFilterAuthenticationFilter(jwtTokenVerifier());
    }

    protected String getSecuredResources() {
        return SECURED_RESOURCES;
    }

    private List<Pair<String, String>> splitRequiredClaims() {
        if (CollectionUtils.isEmpty(requiredClaims)) {
            return Collections.emptyList();
        }
        return requiredClaims.stream()
                .filter(v -> v.contains(CLAIM_DELIMITER))
                .map(v -> {
                    String[] splittedClaims = v.split(CLAIM_DELIMITER);
                    return new ImmutablePair<>(splittedClaims[0], splittedClaims[1]);
                }).collect(Collectors.toList());
    }
}
