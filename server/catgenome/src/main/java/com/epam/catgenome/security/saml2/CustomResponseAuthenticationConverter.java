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

import com.epam.catgenome.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class CustomResponseAuthenticationConverter implements Converter<OpenSaml4AuthenticationProvider.ResponseToken, Saml2Authentication> {

    @Autowired
    private Saml2UserDetailsService saml2UserDetailsService;

    @Override
    public Saml2Authentication convert(OpenSaml4AuthenticationProvider.ResponseToken responseToken) {
        log.debug("LOGIN: Convert response token: {}", responseToken);
        Saml2Authentication authentication = OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter().convert(responseToken);

        if (authentication != null && authentication.isAuthenticated()) {
            Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            UserContext userContext = saml2UserDetailsService.loadUserBySaml2(principal);


            return new Saml2Authentication(
                    userContext,
                    authentication.getSaml2Response(),
                    userContext.getAuthorities()
            );
        }

        return authentication;
    }
}
