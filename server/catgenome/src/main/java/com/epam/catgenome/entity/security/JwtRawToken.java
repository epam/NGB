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

package com.epam.catgenome.entity.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Getter
@AllArgsConstructor
public class JwtRawToken {

    private static final String HEADER_PREFIX = "Bearer ";
    private String token;

    public static JwtRawToken fromHeader(String authorizationHeader) {
        if (StringUtils.isEmpty(authorizationHeader)) {
            throw new AuthenticationServiceException("Authorization header is blank");
        }

        if (!authorizationHeader.startsWith(HEADER_PREFIX)) {
            throw new AuthenticationServiceException("Authorization type Bearer is missed");
        }

        return new JwtRawToken(authorizationHeader.substring(HEADER_PREFIX.length(), authorizationHeader.length()));
    }

    public static JwtRawToken fromCookie(Cookie authCookie) throws UnsupportedEncodingException {
        if (authCookie == null || StringUtils.isEmpty(authCookie.getValue())) {
            throw new AuthenticationServiceException("Authorization cookie is blank");
        }

        String authCookieValue = URLDecoder.decode(authCookie.getValue(), "UTF-8");

        if (!authCookieValue.startsWith(HEADER_PREFIX)) {
            throw new AuthenticationServiceException("Authorization type Bearer is missed");
        }

        return new JwtRawToken(authCookieValue.substring(HEADER_PREFIX.length(), authCookieValue.length()));
    }
}
