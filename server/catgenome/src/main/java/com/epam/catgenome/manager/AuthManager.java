package com.epam.catgenome.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.epam.catgenome.entity.security.JwtRawToken;
import com.epam.catgenome.security.UserContext;
import com.epam.catgenome.security.jwt.JwtTokenGenerator;

/**
 * A service class that encapsulates operations, connected with authenticated users
 */
@Service
public class AuthManager {
    public static final String UNAUTHORIZED_USER = "Unauthorized";

    private final JwtTokenGenerator jwtTokenGenerator;

    @Autowired
    public AuthManager(JwtTokenGenerator jwtTokenGenerator) {
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

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
     */
    public JwtRawToken issueTokenForCurrentUser(Long expiration) {
        Object principal = getPrincipal();
        if (principal instanceof UserContext) {
            return new JwtRawToken(jwtTokenGenerator.encodeToken(((UserContext) principal).toClaims(), expiration));
        } else {
            return new JwtRawToken(jwtTokenGenerator.encodeToken(new UserContext(principal.toString()).toClaims(),
                                                                 expiration));
        }
    }
}
