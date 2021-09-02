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

package com.epam.catgenome.security.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.metadata.ExtendedMetadata;

public class SAMLContextProviderCustomSignKey extends SAMLContextProviderImpl {
    private String signingKey;
    private LBConfig lbConfig;

    public SAMLContextProviderCustomSignKey(final String signingKey) {
        this.signingKey = signingKey;
    }

    public SAMLContextProviderCustomSignKey(final String signingKey, final LBConfig lbConfig) {
        this(signingKey);
        this.lbConfig = lbConfig;
    }

    @Override
    protected void populateLocalEntity(final SAMLMessageContext samlContext)
        throws MetadataProviderException {
        String localEntityId = samlContext.getLocalEntityId();
        QName localEntityRole = samlContext.getLocalEntityRole();

        if (localEntityId == null) {
            throw new MetadataProviderException("No hosted service provider is configured and no alias was selected");
        }

        EntityDescriptor entityDescriptor = metadata.getEntityDescriptor(localEntityId);
        RoleDescriptor
            roleDescriptor = metadata.getRole(localEntityId, localEntityRole, SAMLConstants.SAML20P_NS);
        ExtendedMetadata extendedMetadata = metadata.getExtendedMetadata(localEntityId);

        if (entityDescriptor == null || roleDescriptor == null) {
            throw new MetadataProviderException("Metadata for entity " + localEntityId +
                                                " and role " + localEntityRole + " wasn't found");
        }

        samlContext.setLocalEntityMetadata(entityDescriptor);
        samlContext.setLocalEntityRoleMetadata(roleDescriptor);
        samlContext.setLocalExtendedMetadata(extendedMetadata);

        if (extendedMetadata.getSigningKey() != null) {
            samlContext.setLocalSigningCredential(keyManager.getCredential(extendedMetadata.getSigningKey()));
        } else {
            samlContext.setLocalSigningCredential(keyManager.getCredential(signingKey));
        }
    }

    @Override
    protected void populateGenericContext(final HttpServletRequest request,
                                          final HttpServletResponse response,
                                          final SAMLMessageContext context) throws MetadataProviderException {
        final HttpServletRequest wrappedRequest = lbConfig == null ? request : new LPRequestWrapper(request);
        super.populateGenericContext(wrappedRequest, response, context);

    }

    private final class LPRequestWrapper extends HttpServletRequestWrapper {

        private LPRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getContextPath() {
            return lbConfig.getContextPath();
        }

        @Override
        public String getScheme() {
            return lbConfig.getScheme();
        }

        @Override
        public String getServerName() {
            return lbConfig.getServerName();
        }

        @Override
        public int getServerPort() {
            return lbConfig.getServerPort();
        }

        @Override
        public String getRequestURI() {
            final StringBuilder sb = new StringBuilder(lbConfig.getContextPath());
            sb.append(getServletPath());
            return sb.toString();
        }

        @Override
        public StringBuffer getRequestURL() {
            final StringBuffer sb = new StringBuffer();
            sb.append(lbConfig.getScheme()).append("://").append(lbConfig.getServerName());
            if (lbConfig.isIncludeServerPortInRequestURL()) {
                sb.append(':').append(lbConfig.getServerPort());
            }
            sb.append(lbConfig.getContextPath())
                    .append(getServletPath());
            if (getPathInfo() != null) {
                sb.append(getPathInfo());
            }
            return sb;
        }

        @Override
        public boolean isSecure() {
            return "https".equalsIgnoreCase(lbConfig.getScheme());
        }

    }
}
