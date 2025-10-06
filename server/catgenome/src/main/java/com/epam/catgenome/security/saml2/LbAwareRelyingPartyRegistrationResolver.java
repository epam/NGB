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

package com.epam.catgenome.security.saml2;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;

@Slf4j
public class LbAwareRelyingPartyRegistrationResolver implements RelyingPartyRegistrationResolver {

    private final RelyingPartyRegistrationRepository repository;
    private final LbConfig lbConfig;
    private final String entityId;

    public LbAwareRelyingPartyRegistrationResolver(RelyingPartyRegistrationRepository repository,
                                                   LbConfig lbConfig, String entityId) {
        this.repository = repository;
        this.lbConfig = lbConfig;
        this.entityId = entityId;
    }

    @Override
    public RelyingPartyRegistration resolve(HttpServletRequest request, String registrationId) {
        log.info("Trying to resolve relying party registration with id {}", registrationId);
        RelyingPartyRegistration registration = this.repository.findByRegistrationId(registrationId);
        if (registration == null) {
            return null;
        }

        if (!lbConfig.isEnabled()) {
            return registration;
        }

        String baseUrl = buildBaseUrl();

        // Use the new builder pattern without deprecated methods
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration
                .withRegistrationId(registration.getRegistrationId())
                .entityId(this.entityId)
                .assertionConsumerServiceLocation(baseUrl + "/login/saml2/sso/{registrationId}")
                .assertingPartyDetails(assertingParty ->
                        assertingParty
                                .entityId(registration.getAssertingPartyDetails().getEntityId())
                                .singleSignOnServiceLocation(registration.getAssertingPartyDetails().getSingleSignOnServiceLocation())
                                .wantAuthnRequestsSigned(registration.getAssertingPartyDetails().getWantAuthnRequestsSigned())
                                .verificationX509Credentials(c -> c.addAll(registration.getAssertingPartyDetails().getVerificationX509Credentials()))
                );

        // Copy signing credentials if they exist
        if (!registration.getSigningX509Credentials().isEmpty()) {
            builder.signingX509Credentials(c -> c.addAll(registration.getSigningX509Credentials()));
        }

        // Copy decryption credentials if they exist
        if (!registration.getDecryptionX509Credentials().isEmpty()) {
            builder.decryptionX509Credentials(c -> c.addAll(registration.getDecryptionX509Credentials()));
        }

        return builder.build();
    }

    private String buildBaseUrl() {
        StringBuilder url = new StringBuilder();
        url.append(lbConfig.getScheme()).append("://").append(lbConfig.getServerName());

        if (lbConfig.isIncludeServerPortInRequestURL()) {
            url.append(":").append(lbConfig.getServerPort());
        }

        url.append(lbConfig.getContextPath());
        return url.toString();
    }
}
