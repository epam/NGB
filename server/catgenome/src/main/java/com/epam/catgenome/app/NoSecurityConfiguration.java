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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Class represents Configuration to disables security according to property file
 */
@Configuration
@ConditionalOnProperty(prefix = "jwt.security.", name = "enable", havingValue = "false")
public class NoSecurityConfiguration {

    /**
     * The order the class used to carry as {@code @Order(3)}, behind the JWT (2) and SAML (1)
     * chains. In Spring Security 6 the order belongs to the {@link SecurityFilterChain} bean rather
     * than to the configuration class, and while SAML and JWT are out of the build (Phase 3) this
     * is the only chain there is - but the number is kept so Phase 4 can slot the other two in
     * front of it without having to rediscover the intended precedence.
     */
    private static final int CHAIN_ORDER = 3;

    @Value("${security.frame-options.disable:false}")
    private boolean frameOptionsDisable;

    /**
     * {@code WebSecurityConfigurerAdapter} was removed in Spring Security 6, so the chain is a bean.
     *
     * <p>The matcher widens from {@code antMatchers("/*")} to {@code anyRequest()} deliberately, and
     * it is not a behaviour change: {@code /*} matches a single path segment, so under
     * {@code authorizeRequests()} nothing below the first level was ever matched by a rule - and an
     * unmatched request was <em>permitted</em>, because the old {@code FilterSecurityInterceptor}
     * only rejected requests that had a matching-but-failing rule. Spring Security 6 inverted that:
     * {@code authorizeHttpRequests} denies anything no rule matches. {@code anyRequest().permitAll()}
     * is what the old configuration actually did.
     */
    @Bean
    @Order(CHAIN_ORDER)
    public SecurityFilterChain noSecurityFilterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());

        if (frameOptionsDisable) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        }

        return http.build();
    }

}
