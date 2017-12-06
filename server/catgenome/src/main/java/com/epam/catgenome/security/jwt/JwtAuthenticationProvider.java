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
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Class represents JWT authentication
 */
@AllArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private JwtTokenVerifier tokenVerifier;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtRawToken jwtRawToken = (JwtRawToken) authentication.getCredentials();
        if (jwtRawToken == null) {
            throw new AuthenticationServiceException("Authentication error: missing token");
        }
        JwtTokenClaims claims;
        try {
            claims = tokenVerifier.readClaims(jwtRawToken.getToken());
        } catch (TokenVerificationException e) {
            throw new AuthenticationServiceException("Authentication error", e);
        }
        UserContext context = new UserContext(jwtRawToken, claims);
        return new JwtAuthenticationToken(context, context.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
