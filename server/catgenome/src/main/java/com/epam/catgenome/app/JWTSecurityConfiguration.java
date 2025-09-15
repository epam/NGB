/*
 * MIT License
 *
 * Copyright (c) 2017-2022 EPAM Systems
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

import com.epam.catgenome.security.jwt.JwtAuthenticationProvider;
import com.epam.catgenome.security.jwt.JwtFilterAuthenticationFilter;
import com.epam.catgenome.security.jwt.JwtTokenVerifier;
import com.epam.catgenome.security.jwt.RestAuthenticationEntryPoint;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class provides JWT Security Configuration for Spring Boot application according to property file
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "jwt.security.", name = "enable", havingValue = "true")
@Order(1)
@ComponentScan(basePackages = {"com.epam.catgenome.security.jwt"})
public class JWTSecurityConfiguration {

    @Value("${jwt.key.public}")
    private String publicKey;

    @Value("#{'${jwt.required.claims}'.split(',')}")
    private List<String> requiredClaims;

    @Value("${security.frame-options.disable:false}")
    private boolean frameOptionsDisable;

    @Autowired(required = false)
    private OpenSaml4AuthenticationProvider samlAuthenticationProvider;

    private static final String CLAIM_DELIMITER = "=";

    protected String getPublicKey() {
        return publicKey;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .defaultAuthenticationEntryPointFor(
                                new RestAuthenticationEntryPoint(),
                                new AntPathRequestMatcher(getSecuredResources())
                        )
                )
                .securityMatcher(getFullRequestMatcher())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(getUnsecuredResources()).permitAll()
                        .requestMatchers(getSecuredResources()).authenticated()
                )
                .headers(headers -> headers
                        .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .addFilterBefore(getJwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        if (frameOptionsDisable) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            JwtAuthenticationProvider jwtAuthenticationProvider) {
        List<AuthenticationProvider> providers = new ArrayList<>();

        if (samlAuthenticationProvider != null) {
            providers.add(samlAuthenticationProvider);
        }

        providers.add(jwtAuthenticationProvider);

        return new ProviderManager(providers);
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

    protected RequestMatcher getFullRequestMatcher() {
        return new AntPathRequestMatcher(getSecuredResources());
    }

    protected String getSecuredResources() {
        return "/restapi/**";
    }

    protected String[] getUnsecuredResources() {
        return new String[] {
                "/swagger-ui/**", "/api-docs/**", "/", "/index.html", "/app.css", "/app.bundle.js", "/ngb-logo.png",
                "/error-401.html", "/saml2/**"
        };
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
