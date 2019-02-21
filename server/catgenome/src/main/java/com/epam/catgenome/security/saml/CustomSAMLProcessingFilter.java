package com.epam.catgenome.security.saml;

import org.opensaml.common.SAMLException;
import org.opensaml.common.binding.decoding.URIComparator;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLAuthenticationToken;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.context.SAMLMessageContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.springframework.security.saml.util.SAMLUtil.getBindingForEndpoint;

public class CustomSAMLProcessingFilter extends SAMLProcessingFilter {

    private static final String INVALID_SAML_MESSAGE = "Incoming SAML message is invalid";
    private final URIComparator uriComparator;

    public CustomSAMLProcessingFilter(final URIComparator uriComparator) {
        this.uriComparator = uriComparator;
    }

    @Override
    public Authentication attemptAuthentication(final HttpServletRequest request,
                                                final HttpServletResponse response) throws AuthenticationException {
        try {

            logger.debug("Attempting SAML2 authentication using profile {}", getProfileName());
            SAMLMessageContext context = contextProvider.getLocalEntity(request, response);
            processor.retrieveMessage(context);

            // Override set values
            context.setCommunicationProfileId(getProfileName());
            context.setLocalEntityEndpoint(getEndpoint(context.getLocalEntityRoleMetadata().getEndpoints(),
                    context.getInboundSAMLBinding(), context.getInboundMessageTransport()));

            SAMLAuthenticationToken token = new SAMLAuthenticationToken(context);
            return getAuthenticationManager().authenticate(token);

        } catch (SAMLException | org.opensaml.xml.security.SecurityException e) {
            logger.debug(INVALID_SAML_MESSAGE, e);
            throw new AuthenticationServiceException(INVALID_SAML_MESSAGE, e);
        } catch (MetadataProviderException e) {
            logger.debug("Error determining metadata contracts", e);
            throw new AuthenticationServiceException("Error determining metadata contracts", e);
        } catch (MessageDecodingException e) {
            logger.debug("Error decoding incoming SAML message", e);
            throw new AuthenticationServiceException("Error decoding incoming SAML message", e);
        }
    }

    private Endpoint getEndpoint(final List<Endpoint> endpoints,
                                 final String messageBinding,
                                 final InTransport inTransport) throws SAMLException {
        HttpServletRequest httpRequest = ((HttpServletRequestAdapter) inTransport).getWrappedRequest();
        String requestURL = DatatypeHelper.safeTrimOrNullString(httpRequest.getRequestURL().toString());
        for (Endpoint endpoint : endpoints) {
            String binding = getBindingForEndpoint(endpoint);
            // Check that destination and binding matches
            if (binding.equals(messageBinding)) {
                if (endpoint.getLocation() != null &&
                        uriComparator.compare(endpoint.getLocation(), requestURL)) {
                    logger.debug("Found endpoint {} for request URL {} based on location attribute in metadata",
                            endpoint, requestURL);
                    return endpoint;
                } else if (endpoint.getResponseLocation() != null &&
                        uriComparator.compare(endpoint.getResponseLocation(), requestURL)) {
                    logger.debug("Found endpoint {} for request URL {} based on response " +
                            "location attribute in metadata", endpoint, requestURL);
                    return endpoint;
                }
            }
        }
        throw new SAMLException("Endpoint with message binding " + messageBinding + " and URL " +
                requestURL + " wasn't found in local metadata");
    }
}