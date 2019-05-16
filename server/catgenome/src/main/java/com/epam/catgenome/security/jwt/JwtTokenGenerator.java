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

package com.epam.catgenome.security.jwt;

import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_GROUPS;
import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_ORG_UNIT_ID;
import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_ROLES;
import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_USER_ID;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.security.JwtTokenClaims;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * A service class for generating JWT tokens of application users
 */
@Service
public class JwtTokenGenerator {
    private String privateKeyString;
    private Long jwtExpirationSeconds;
    private RSAPrivateKey privateKey;

    @Autowired
    public JwtTokenGenerator(@Value("${jwt.key.private:}") String privateKeyString,
                             @Value("#{catgenome['jwt.token.expiration.seconds']?:2592000}")
                                 Long jwtExpirationSeconds) {
        this.privateKeyString = privateKeyString;
        this.jwtExpirationSeconds = jwtExpirationSeconds;
    }

    @PostConstruct
    public void initKey() {
        try {
            if (StringUtils.isNotBlank(privateKeyString)) {
                this.privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString)));
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JwtInitializationException(e);
        }
    }

    /**
     * Genreate a token for specified JWT claims
     * @param claims claims of application user
     * @param expirationSeconds token expiration period
     * @return a JWT token
     */
    public String encodeToken(JwtTokenClaims claims, Long expirationSeconds) {
        Assert.notNull(privateKey, MessageHelper.getMessage(MessagesConstants.ERROR_NO_JWT_PRIVATE_KEY_CONFIGURED));

        Long expiration = expirationSeconds == null ? jwtExpirationSeconds : expirationSeconds;
        JWTCreator.Builder tokenBuilder = buildToken(claims);
        tokenBuilder.withExpiresAt(toDate(LocalDateTime.now().plusSeconds(expiration)));
        return tokenBuilder.sign(Algorithm.RSA512(privateKey));
    }

    private JWTCreator.Builder buildToken(JwtTokenClaims claims) {
        JWTCreator.Builder tokenBuilder = JWT.create();
        tokenBuilder.withHeader(ImmutableMap.of("typ", "JWT"));
        tokenBuilder
            .withIssuedAt(new Date())
            .withJWTId(Strings.isNullOrEmpty(claims.getJwtTokenId()) ?
                       UUID.randomUUID().toString() : claims.getJwtTokenId())
            .withSubject(claims.getUserName())
            .withClaim(CLAIM_USER_ID, getUserIdClaim(claims))
            .withClaim(CLAIM_ORG_UNIT_ID, claims.getOrgUnitId());

        if (claims.getRoles() != null) {
            tokenBuilder.withArrayClaim(CLAIM_ROLES, claims.getRoles().toArray(new String[claims.getRoles().size()]));
        }
        if (claims.getGroups() != null) {
            tokenBuilder.withArrayClaim(CLAIM_GROUPS,
                                        claims.getGroups().toArray(new String[claims.getGroups().size()]));
        }

        return tokenBuilder;
    }

    private Integer getUserIdClaim(final JwtTokenClaims claims) {
        return claims.getUserId() != null ? claims.getUserId().intValue() : null;
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
