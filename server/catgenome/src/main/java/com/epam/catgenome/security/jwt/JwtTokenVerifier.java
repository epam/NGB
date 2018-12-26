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

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.epam.catgenome.entity.security.JwtTokenClaims;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_ORG_UNIT_ID;
import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_USER_ID;
import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_GROUPS;
import static com.epam.catgenome.entity.security.JwtTokenClaims.CLAIM_ROLES;

/**
 * Class represents JWT token verification
 */
public class JwtTokenVerifier {

    private RSAPublicKey publicKey;
    private List<Pair<String, String>> requiredClaims;

    public JwtTokenVerifier(String publicKey, List<Pair<String, String>> requiredClaims) {
        try {
            this.publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            this.requiredClaims = requiredClaims;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JwtInitializationException(e);
        }
    }

    public JwtTokenClaims readClaims(String jwtToken) {
        DecodedJWT decodedToken;
        try {
            decodedToken = JWT.require(Algorithm.RSA512(publicKey))
                    .build()
                    .verify(jwtToken);

        } catch (JWTVerificationException jve) {
            throw new TokenVerificationException(jve);
        }
        JwtTokenClaims tokenClaims = JwtTokenClaims.builder()
                .jwtTokenId(decodedToken.getId())
                .userName(decodedToken.getSubject())
                .userId(new Long(decodedToken.getClaim(CLAIM_USER_ID).asInt()))
                .orgUnitId(decodedToken.getClaim(CLAIM_ORG_UNIT_ID).asString())
                .issuedAt(decodedToken.getIssuedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .expiresAt(decodedToken.getExpiresAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
        if (decodedToken.getClaim(CLAIM_ROLES) != null) {
            tokenClaims.setRoles(decodedToken.getClaim(CLAIM_ROLES).asList(String.class));
        }
        if (decodedToken.getClaim(CLAIM_GROUPS) != null) {
            tokenClaims.setGroups(decodedToken.getClaim(CLAIM_GROUPS).asList(String.class));
        }
        return validateClaims(tokenClaims);
    }

    private JwtTokenClaims validateClaims(JwtTokenClaims tokenClaims) {
        if (StringUtils.isEmpty(tokenClaims.getJwtTokenId())) {
            throw new TokenVerificationException("Invalid token: token ID is empty");
        }
        if (tokenClaims.getUserId() == null) {
            throw new TokenVerificationException("Invalid token: user ID is empty");
        }
        if (StringUtils.isEmpty(tokenClaims.getUserName())) {
            throw new TokenVerificationException("Invalid token: user name is empty");
        }
        validateRequiredClaims(tokenClaims);
        return tokenClaims;
    }

    private void validateRequiredClaims(JwtTokenClaims tokenClaims) {
        if (CollectionUtils.isEmpty(requiredClaims)) {
            return;
        }
        requiredClaims.forEach(claimPair -> {
            switch (claimPair.getLeft()) {
                case CLAIM_ORG_UNIT_ID:
                    if (!tokenClaims.getOrgUnitId().equals(claimPair.getRight())) {
                        throw new TokenVerificationException("Invalid token: invalid org unit id claim");
                    }
                    break;
                case CLAIM_ROLES:
                    if (tokenClaims.getRoles().stream().noneMatch(v -> v.equals(claimPair.getRight()))) {
                        throw new TokenVerificationException("Invalid token: invalid roles claim");
                    }
                    break;
                case CLAIM_GROUPS:
                    if (tokenClaims.getGroups().stream().noneMatch(v -> v.equals(claimPair.getRight()))) {
                        throw new TokenVerificationException("Invalid token: invalid groups claim");
                    }
                    break;
                default:
                    throw new TokenVerificationException("Unsupported claim: " + claimPair.getRight());
            }
        });
    }
}
