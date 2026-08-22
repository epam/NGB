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

package com.epam.catgenome.app;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.assertion.SAML2AssertionValidationParameters;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2ResponseValidatorResult;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.AssertionToken;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.metadata.OpenSaml4MetadataResolver;
import org.springframework.security.saml2.provider.service.metadata.Saml2MetadataResponseResolver;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.security.saml2.provider.service.web.metadata.RequestMatcherMetadataResponseResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.ResourceUtils;

import com.epam.catgenome.security.UserContext;
import com.epam.catgenome.security.saml.SamlUserDetailsService;
import com.epam.catgenome.util.Utils;

import jakarta.servlet.DispatcherType;

/**
 * Configures SAML 2.0 Web SSO for the browser client, on Spring Security 6's own SAML support
 * ({@code spring-security-saml2-service-provider} over OpenSAML 4).
 *
 * <h2>The endpoint URLs are the legacy ones, deliberately</h2>
 *
 * The OpenSAML 2 extension this replaces owned the {@code /saml/**} namespace and Spring Security 6
 * defaults to a different one ({@code /saml2/service-provider-metadata/{registrationId}},
 * {@code /login/saml2/sso/{registrationId}}, {@code /logout/saml2/slo}). Every existing NGB
 * deployment is registered with its identity provider under the old URLs, so all four externally
 * visible endpoints are configured back to exactly where they were:
 *
 * <table border="1">
 *   <caption>SAML endpoints</caption>
 *   <tr><th>What</th><th>URL</th><th>Configured by</th></tr>
 *   <tr><td>SP metadata</td><td>{@code /saml/metadata}</td>
 *       <td>{@link #metadataResponseResolver} on {@code saml2Metadata}</td></tr>
 *   <tr><td>Assertion consumer service</td><td>{@code /saml/SSO}</td>
 *       <td>{@code saml2Login().loginProcessingUrl}</td></tr>
 *   <tr><td>Single logout receiver</td><td>{@code /saml/SingleLogout}</td>
 *       <td>{@code saml2Logout().logoutRequest/logoutResponse}</td></tr>
 *   <tr><td>SP-initiated logout</td><td>{@code /saml/logout}</td>
 *       <td>{@code saml2Logout().logoutUrl}</td></tr>
 * </table>
 *
 * <p>The one URL that had to change is the SP-internal one: Spring Security addresses relying party
 * registrations by id, so the endpoint that builds an {@code <AuthnRequest>} is
 * {@code /saml/login/{registrationId}} rather than the extension's {@code /saml/login}. With a single
 * registration that is {@value #AUTHENTICATION_REQUEST_URL}. Nothing outside this server knows that
 * URL - the identity provider never sees it - so it is a safe change.
 *
 * <h2>Legacy behaviour that has no Spring Security 6 equivalent</h2>
 *
 * <ul>
 *   <li>{@code /saml/SSOHoK} (Holder-of-Key) and {@code /saml/discovery} plus {@code /saml/web/**}
 *       (the extension's IdP discovery UI) are not ported. NGB has always had exactly one identity
 *       provider, so discovery was dead weight, and nothing supported Holder-of-Key.</li>
 *   <li>{@code saml.lb.*} is gone. It existed so the extension could derive absolute endpoint URLs
 *       from a load balancer's view of the request instead of the container's; here the assertion
 *       consumer and single logout locations are absolute values built from {@code saml.base.url},
 *       so there is nothing to derive. {@code server.forward-headers-strategy} covers the rest.</li>
 *   <li>{@code saml.validate.url.without.scheme} is gone with it. It made the extension compare the
 *       assertion's {@code Destination} to the request URL ignoring the scheme; Spring Security
 *       compares it to the configured assertion consumer service location, which is a constant.</li>
 *   <li>The logout response clock skew ({@code LOGOUT_RESPONSE_SKEW}, 120s) has nowhere to go:
 *       {@code OpenSaml4LogoutResponseValidator} does not time-validate logout responses at all.</li>
 * </ul>
 *
 * @see JWTSecurityConfiguration the chain in front of this one, for {@code /restapi/**}
 */
@Configuration
@ConditionalOnProperty(value = "saml.security.enable", havingValue = "true")
@ComponentScan(basePackages = {"com.epam.catgenome.security.saml"})
public class SAMLSecurityConfiguration {

    /**
     * The single relying party registration id. It appears in {@link #AUTHENTICATION_REQUEST_URL} and
     * nowhere an identity provider can see, so its value is arbitrary.
     */
    public static final String REGISTRATION_ID = "ngb";

    /**
     * Where an unauthenticated browser is sent to start SSO. {@link JWTSecurityConfiguration} points
     * {@code /restapi/navigate} here as well, since that is the one REST path opened in a browser.
     */
    public static final String AUTHENTICATION_REQUEST_URL = "/saml/login/" + REGISTRATION_ID;

    private static final String AUTHENTICATION_REQUEST_URL_TEMPLATE = "/saml/login/{registrationId}";

    private static final String METADATA_URL = "/saml/metadata";

    private static final String ASSERTION_CONSUMER_SERVICE_URL = "/saml/SSO";

    private static final String SINGLE_LOGOUT_URL = "/saml/SingleLogout";

    private static final String LOGOUT_URL = "/saml/logout";

    private static final String LOGOUT_SUCCESS_URL = "/";

    /**
     * Behind the JWT chain ({@link JWTSecurityConfiguration}, 1) and ahead of the anonymous one
     * ({@link NoSecurityConfiguration}, 3). This chain matches everything, so it has to come last of
     * the two that authenticate.
     */
    private static final int CHAIN_ORDER = 2;

    /**
     * Tolerance for clock drift between this server and the identity provider, as
     * {@code WebSSOProfileConsumerImpl.setResponseSkew} used to be given.
     */
    private static final Duration RESPONSE_SKEW = Duration.ofSeconds(1200);

    private static final String KEY_STORE_TYPE = "JKS";

    /**
     * These are springdoc-openapi's paths. {@code /api-docs/**}, which this list used to carry, was
     * Swagger 1's; springdoc serves {@code /v3/api-docs} and {@code /swagger-ui/**} instead, and the
     * old entry would have left the API documentation behind SSO.
     */
    private static final String[] UNSECURED_RESOURCES = {
        "/swagger-ui/**", "/v3/api-docs/**", "/error-401.html"
    };

    private static final String SECURED_RESOURCES = "/**";

    @Value("${server.ssl.endpoint.id}")
    private String endpointId;

    @Value("${server.ssl.key-store}")
    private String keyStore;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.keyAlias}")
    private String keyAlias;

    @Value("${saml.sign.key}")
    private String signingKey;

    @Value("${server.ssl.metadata}")
    private String federationMetadataFile;

    @Value("${saml.authn.request.binding}")
    private String authnRequestBinding;

    @Value("${saml.login.failure.redirect:/error-401.html}")
    private String loginFailureRedirect;

    @Value("${saml.base.url:}")
    private String samlBaseUrl;

    @Value("${security.frame-options.disable:false}")
    private boolean frameOptionsDisable;

    @Value("${saml.authn.max.authentication.age:93600}")
    private Long maxAuthenticationAge;

    /**
     * Required, as it was before: {@link SamlUserDetailsService} is
     * {@code @ConditionalOnProperty(security.acl.enable=true)}, so {@code saml.security.enable=true}
     * without ACL has never been a startable configuration and still is not.
     */
    @Autowired
    private SamlUserDetailsService samlUserDetailsService;

    /**
     * The SAML chain. It matches every request, permits the few unauthenticated resources and
     * requires authentication for the rest.
     *
     * <p>{@code securityMatcher(SECURED_RESOURCES)} rather than leaving the matcher implicit: an
     * implicit matcher is {@code AnyRequestMatcher}, and {@code WebSecurityFilterChainValidator}
     * rejects one of those in any position but last. {@link NoSecurityConfiguration} is such a chain
     * and is only inactive because {@code jwt.security.enable} is normally on wherever SAML is; a
     * {@code /**} pattern keeps the pathological combination a shadowed chain, as it was before,
     * instead of a startup failure.
     *
     * <p>{@code dispatcherTypeMatchers(ERROR).permitAll()} is not optional. Spring Security 6 filters
     * ERROR dispatches, so when the JWT chain answers an unauthenticated {@code /restapi/**} call with
     * {@code sendError(401)} the container's dispatch to {@code /error} comes back through the filters -
     * and {@code /error} is not under {@code /restapi/**}, so it lands on this chain, which requires
     * authentication and turns the 401 into a 302 to the login page. That breaks every REST client,
     * ngb-cli included: it looks for a status code and gets an HTML redirect.
     */
    @Bean
    @Order(CHAIN_ORDER)
    public SecurityFilterChain samlFilterChain(final HttpSecurity http,
                                               final RelyingPartyRegistrationRepository registrations)
            throws Exception {
        http.securityMatcher(SECURED_RESOURCES)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(UNSECURED_RESOURCES).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint(AUTHENTICATION_REQUEST_URL)))
                .saml2Metadata(metadata -> metadata
                        .metadataResponseResolver(metadataResponseResolver(registrations)))
                .saml2Login(login -> login
                        .relyingPartyRegistrationRepository(registrations)
                        .loginProcessingUrl(ASSERTION_CONSUMER_SERVICE_URL)
                        .authenticationRequestUriQuery(AUTHENTICATION_REQUEST_URL_TEMPLATE)
                        .authenticationManager(new ProviderManager(samlAuthenticationProvider()))
                        .successHandler(successRedirectHandler())
                        .failureHandler(authenticationFailureHandler()))
                .saml2Logout(logout -> logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutRequest(request -> request.logoutUrl(SINGLE_LOGOUT_URL))
                        .logoutResponse(response -> response.logoutUrl(SINGLE_LOGOUT_URL)))
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutSuccessUrl(LOGOUT_SUCCESS_URL)
                        .invalidateHttpSession(false));

        if (frameOptionsDisable) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        }

        return http.build();
    }

    /**
     * The relying party registration, i.e. everything this server needs to know about itself and about
     * the identity provider. The asserting party half is read out of {@code server.ssl.metadata}; the
     * relying party half is built from the same properties the OpenSAML 2 configuration used.
     *
     * <p>Two things the metadata file cannot tell us and that therefore have to be set explicitly:
     * {@code authnRequestsSigned} defaults to {@code false} in Spring Security and is <em>not</em>
     * derived from the identity provider's {@code WantAuthnRequestsSigned}, while the extension's
     * {@code MetadataGenerator} defaulted it to true and so always signed; and the
     * {@code <AuthnRequest>} binding, which {@code saml.authn.request.binding} pins rather than
     * letting the first binding in the metadata win.
     *
     * <p>Note that {@code signingX509Credentials} is {@code saml.sign.key} and not
     * {@code server.ssl.keyAlias}. Spring Security uses one credential list for both signing outgoing
     * messages and describing the signing key in published metadata. The extension used two: it signed
     * messages with {@code saml.sign.key} (via {@code SAMLContextProviderCustomSignKey}) but generated
     * and signed metadata with the key store's default key, which was the HTTPS key. Anything reading
     * NGB's metadata to learn the message signing key therefore learned the wrong one; it now learns
     * {@code saml.sign.key}, which is also the certificate {@code .devenv} exports for the IdP.
     */
    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        final KeyStore store = loadKeyStore();
        final String baseUrl = Utils.getUrlWithoutTrailingSlash(StringUtils.defaultIfBlank(samlBaseUrl, endpointId));
        final RelyingPartyRegistration registration = readAssertingPartyMetadata()
                .registrationId(REGISTRATION_ID)
                .entityId(endpointId)
                .signingX509Credentials(credentials -> credentials.add(signingCredential(store, signingKey)))
                .decryptionX509Credentials(credentials -> credentials.add(decryptionCredential(store, keyAlias)))
                .authnRequestsSigned(true)
                .assertionConsumerServiceLocation(baseUrl + ASSERTION_CONSUMER_SERVICE_URL)
                .assertionConsumerServiceBinding(Saml2MessageBinding.POST)
                .singleLogoutServiceLocation(baseUrl + SINGLE_LOGOUT_URL)
                .singleLogoutServiceResponseLocation(baseUrl + SINGLE_LOGOUT_URL)
                .singleLogoutServiceBindings(bindings -> {
                    bindings.add(Saml2MessageBinding.POST);
                    bindings.add(Saml2MessageBinding.REDIRECT);
                })
                .assertingPartyMetadata(party -> party.singleSignOnServiceBinding(singleSignOnServiceBinding()))
                .build();
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    /**
     * Publishes this server's metadata at {@value #METADATA_URL} as a signed {@code <md:EntityDescriptor>}.
     *
     * <p>{@code saml2Metadata().metadataUrl(...)} would put the endpoint in the right place but builds
     * an {@link OpenSaml4MetadataResolver} with signing left at its default of off, and the extension
     * signed its metadata ({@code ExtendedMetadata.setSignMetadata(true)}). Hence the resolver here.
     *
     * <p>Pretty-printing has to stay off. The signature is enveloped, computed over the marshalled DOM,
     * and {@code SerializeSupport.prettyPrintXML} then inserts whitespace text nodes into that same DOM
     * - canonicalisation does not strip them, so the digest no longer matches what was signed. It also
     * matches what {@code MetadataDisplayFilter} used to serve, which was not pretty-printed either.
     */
    private Saml2MetadataResponseResolver metadataResponseResolver(
            final RelyingPartyRegistrationRepository registrations) {
        final OpenSaml4MetadataResolver metadata = new OpenSaml4MetadataResolver();
        metadata.setSignMetadata(true);
        metadata.setUsePrettyPrint(false);
        final RequestMatcherMetadataResponseResolver resolver =
                new RequestMatcherMetadataResponseResolver(registrations, metadata);
        resolver.setRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(METADATA_URL));
        return resolver;
    }

    /**
     * Validates the {@code <Response>} and turns it into an {@code Authentication}.
     *
     * <p>Built here and handed to {@code saml2Login().authenticationManager(...)} rather than exposed as
     * a bean. {@code Saml2LoginConfigurer} looks for an {@code OpenSaml4AuthenticationProvider} bean
     * with {@code getIfUnique()}, and this application also publishes a {@code JwtAuthenticationProvider}
     * - so as beans the two would compete over the same lookup and both could end up ignored.
     */
    private OpenSaml4AuthenticationProvider samlAuthenticationProvider() {
        final OpenSaml4AuthenticationProvider provider = new OpenSaml4AuthenticationProvider();
        provider.setAssertionValidator(assertionValidator());
        provider.setResponseAuthenticationConverter(responseAuthenticationConverter());
        return provider;
    }

    /**
     * The default assertion validator with {@link #RESPONSE_SKEW} for clock drift, plus the
     * {@code saml.authn.max.authentication.age} check that {@code WebSSOProfileConsumerImpl} used to do.
     */
    private Converter<AssertionToken, Saml2ResponseValidatorResult> assertionValidator() {
        final Converter<AssertionToken, Saml2ResponseValidatorResult> delegate =
                OpenSaml4AuthenticationProvider.createDefaultAssertionValidatorWithParameters(
                    parameters -> parameters.put(SAML2AssertionValidationParameters.CLOCK_SKEW, RESPONSE_SKEW));
        return assertionToken -> Objects.requireNonNull(delegate.convert(assertionToken))
                .concat(validateAuthenticationAge(assertionToken.getAssertion()));
    }

    /**
     * Rejects an assertion whose authentication happened longer than
     * {@code saml.authn.max.authentication.age} ago, so that an identity provider replaying a very old
     * session cannot silently log a user in. Skew is added the same way the extension added it.
     */
    private Saml2ResponseValidatorResult validateAuthenticationAge(final Assertion assertion) {
        final Instant tooOld = Instant.now().minusSeconds(maxAuthenticationAge).minus(RESPONSE_SKEW);
        return assertion.getAuthnStatements().stream()
                .map(AuthnStatement::getAuthnInstant)
                .filter(Objects::nonNull)
                .filter(authnInstant -> authnInstant.isBefore(tooOld))
                .findFirst()
                .map(authnInstant -> Saml2ResponseValidatorResult.failure(new Saml2Error(
                        Saml2ErrorCodes.INVALID_ASSERTION,
                        "Authentication statement is too old to be used, authenticated at " + authnInstant)))
                .orElseGet(Saml2ResponseValidatorResult::success);
    }

    /**
     * Maps the validated assertion onto an NGB user.
     *
     * <p>This is where {@link SamlUserDetailsService} is called. Spring Security 6 has no
     * {@code SAMLUserDetailsService} hook to plug it into, so the default converter runs first to get a
     * parsed principal and the {@link UserContext} it produces replaces it - which is what
     * {@code SAMLAuthenticationProvider.setUserDetails} did. The principal's attributes, session indexes
     * and registration id are copied across because single logout needs them: the
     * {@code <LogoutRequest>} carries the session indexes, and the registration id says which identity
     * provider to send it to.
     */
    private Converter<ResponseToken, Saml2Authentication> responseAuthenticationConverter() {
        final Converter<ResponseToken, Saml2Authentication> delegate =
                OpenSaml4AuthenticationProvider.createDefaultResponseAuthenticationConverter();
        return responseToken -> {
            final Saml2Authentication authentication = Objects.requireNonNull(delegate.convert(responseToken));
            final Saml2AuthenticatedPrincipal assertion =
                    (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            final UserContext user = samlUserDetailsService.loadUserBySAML(assertion);
            user.setAttributes(assertion.getAttributes());
            user.setSessionIndexes(assertion.getSessionIndexes());
            user.setRelyingPartyRegistrationId(assertion.getRelyingPartyRegistrationId());
            return new Saml2Authentication(user, authentication.getSaml2Response(), user.getAuthorities());
        };
    }

    /**
     * Sends the user back to whatever they asked for before being bounced into SSO, or to {@code /}.
     *
     * <p>This replaces {@code CustomAwareAuthenticationSuccessHandler}, which was Spring's own
     * {@link SavedRequestAwareAuthenticationSuccessHandler} copied out verbatim with one addition: it
     * rewrote the literal string {@code 8443} to {@code 8080} in the saved redirect URL. That was a
     * workaround for a deployment that terminated TLS on one port and served the application on
     * another, and it broke any deployment that legitimately used 8443. Deleted, not reimplemented.
     */
    private SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
        final SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successRedirectHandler.setDefaultTargetUrl(LOGOUT_SUCCESS_URL);
        return successRedirectHandler;
    }

    private SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        final SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setDefaultFailureUrl(loginFailureRedirect);
        return failureHandler;
    }

    /**
     * Reads the identity provider's metadata. {@code server.ssl.metadata} is a filesystem path - that is
     * what it was when the value went to {@code new FilesystemMetadataProvider(new File(...))} - so it is
     * resolved by {@link #resource(String)} rather than by
     * {@code RelyingPartyRegistrations.fromMetadataLocation}, which would look on the classpath instead.
     */
    private RelyingPartyRegistration.Builder readAssertingPartyMetadata() {
        try (InputStream source = resource(federationMetadataFile).getInputStream()) {
            return RelyingPartyRegistrations.fromMetadata(source);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to read the identity provider metadata from " + federationMetadataFile, e);
        }
    }

    /**
     * Resolves a keystore or metadata location the way Spring Boot resolves {@code server.ssl.key-store}:
     * a prefixed location ({@code file:}, {@code classpath:}, any URL) through a resource loader, and
     * anything else as a filesystem path. Note that a plain {@link FileSystemResourceLoader} is not enough
     * on its own - it strips the leading slash off an absolute path and resolves it against the working
     * directory, which is how {@code /secrets/idp-metadata.xml} became a {@code FileNotFoundException}.
     */
    private static Resource resource(final String location) {
        return ResourceUtils.isUrl(location)
                ? new FileSystemResourceLoader().getResource(location)
                : new FileSystemResource(location);
    }

    private Saml2MessageBinding singleSignOnServiceBinding() {
        final Saml2MessageBinding binding = Saml2MessageBinding.from(authnRequestBinding);
        if (binding == null) {
            throw new IllegalArgumentException("saml.authn.request.binding must be one of '"
                    + Saml2MessageBinding.POST.getUrn() + "' or '" + Saml2MessageBinding.REDIRECT.getUrn()
                    + "', got '" + authnRequestBinding + "'");
        }
        return binding;
    }

    private KeyStore loadKeyStore() {
        try (InputStream source = resource(keyStore).getInputStream()) {
            final KeyStore store = KeyStore.getInstance(KEY_STORE_TYPE);
            store.load(source, keyStorePassword.toCharArray());
            return store;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to load the key store " + keyStore, e);
        }
    }

    private Saml2X509Credential signingCredential(final KeyStore store, final String alias) {
        return Saml2X509Credential.signing(privateKey(store, alias), certificate(store, alias));
    }

    private Saml2X509Credential decryptionCredential(final KeyStore store, final String alias) {
        return Saml2X509Credential.decryption(privateKey(store, alias), certificate(store, alias));
    }

    private PrivateKey privateKey(final KeyStore store, final String alias) {
        final Key key;
        try {
            key = store.getKey(alias, keyStorePassword.toCharArray());
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to read key '" + alias + "' from " + keyStore, e);
        }
        if (!(key instanceof PrivateKey)) {
            throw new IllegalArgumentException("No private key entry under alias '" + alias + "' in " + keyStore);
        }
        return (PrivateKey) key;
    }

    private X509Certificate certificate(final KeyStore store, final String alias) {
        final Certificate certificate;
        try {
            certificate = store.getCertificate(alias);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to read certificate '" + alias + "' from " + keyStore, e);
        }
        if (!(certificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("No X.509 certificate under alias '" + alias + "' in " + keyStore);
        }
        return (X509Certificate) certificate;
    }
}
