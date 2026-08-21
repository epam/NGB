/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

package com.epam.catgenome.manager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.security.UserContext;

/**
 * A service class that encapsulates operations, connected with authenticated users
 */
@Service
public class AuthManager {
    public static final String UNAUTHORIZED_USER = "Unauthorized";

    /**
     * @return UserContext of currently logged in user
     */
    public UserContext getUserContext() {
        Object principal = getPrincipal();
        if (principal instanceof UserContext) {
            return (UserContext)principal;
        } else {
            return null;
        }
    }

    /**
     * @return user name of currently logged in user
     */
    public String getAuthorizedUser() {
        Object principal = getPrincipal();
        if (principal.equals(UNAUTHORIZED_USER)) {
            return UNAUTHORIZED_USER;
        }

        String user;
        if (principal instanceof UserContext) {
            user = ((UserContext) principal).getUsername();
        } else if (principal instanceof String) {
            user = (String) principal;
        } else if (principal instanceof User){
            user = ((User) principal).getUsername();
        } else {
            user = UNAUTHORIZED_USER;
        }

        return user;
    }

    private Object getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return UNAUTHORIZED_USER;
        }
        return authentication.getPrincipal();
    }

    /**
     * @param expiration expiration time for a JWT token. If null, a default expiration value is used
     * @return a JWT token for current user
     * @throws UnsupportedOperationException always, for as long as JWT is out of the build
     *
     * <p>Phase 3 of the Java 21 migration removed {@code security.jwt.JwtTokenGenerator} together
     * with the rest of the JWT and SAML stack (OpenSAML 2 cannot run on Spring Security 6), so there
     * is nothing left to sign a token with. The method, the {@code GET /restapi/user/token} endpoint
     * it backs and the "generate access token" button in the UI settings dialog therefore fail
     * explicitly rather than silently returning an unusable token. Phase 4 restores the generator
     * and this body along with it - see git history for {@code JwtTokenGenerator}, which is the
     * specification for the claims the reissued tokens must carry.
     */
    public JwtRawToken issueTokenForCurrentUser(Long expiration) {
        throw new UnsupportedOperationException(
                "JWT token issuing is unavailable: the JWT stack is out of the build until the "
                        + "Spring Security 6 rewrite. Only AUTH_MODE=none is supported.");
    }
}
