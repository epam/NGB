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

import com.epam.catgenome.security.jwt.JwtAuthenticationProvider;
import com.epam.catgenome.security.jwt.JwtFilterAuthenticationFilter;
import com.epam.catgenome.security.jwt.JwtTokenVerifier;
import com.epam.catgenome.security.jwt.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@ConditionalOnProperty(prefix = "jwt.security.", name = "enable", havingValue = "true")
@ComponentScan(basePackages = {"com.epam.catgenome.security.jwt"})
public class JWTSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${jwt.key.public}")
    private String publicKey;

    protected String getPublicKey() {
        return publicKey;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(jwtAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .exceptionHandling().authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .and()
                .requestMatcher(getFullRequestMatcher())
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .antMatchers(getSecuredResources()).authenticated()
                .antMatchers(getUnsecuredResources()).permitAll()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(getJwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    protected JwtAuthenticationProvider jwtAuthenticationProvider() {
        return new JwtAuthenticationProvider(jwtTokenVerifier());
    }

    @Bean
    public JwtTokenVerifier jwtTokenVerifier() {
        return new JwtTokenVerifier(getPublicKey());
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
                "/restapi/swagger-ui/**",
        };
    }

}
