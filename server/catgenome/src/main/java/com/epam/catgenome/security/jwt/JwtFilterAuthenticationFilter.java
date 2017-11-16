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

package com.epam.catgenome.security.jwt;

import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.entity.security.JwtTokenClaims;
import com.epam.catgenome.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Class represents a security filter for JWT token
 */
public class JwtFilterAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtFilterAuthenticationFilter.class);

    private JwtTokenVerifier tokenVerifier;

    public JwtFilterAuthenticationFilter(JwtTokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        JwtRawToken rawToken;
        String authorizationHeader = extractAuthHeader(request);
        try {
            if (!StringUtils.isEmpty(authorizationHeader)) { // attempt obtain JWT token from HTTP header
                rawToken = JwtRawToken.fromHeader(authorizationHeader);
                LOGGER.debug("Extracted JWT token from authorization HTTP header");
            } else {                                           // else try to get token from cookies
                Cookie authCookie = extractAuthCookie(request);
                rawToken = JwtRawToken.fromCookie(authCookie);
                LOGGER.debug("Extracted JWT token from authorization cookie");
            }
            JwtTokenClaims claims = tokenVerifier.readClaims(rawToken.getToken());
            UserContext context = new UserContext(rawToken, claims);
            JwtAuthenticationToken token = new JwtAuthenticationToken(context, context.getAuthorities());
            token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(token);
        } catch (AuthenticationServiceException | TokenVerificationException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    private String extractAuthHeader(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    private Cookie extractAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("HttpAuthorization".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }
}
