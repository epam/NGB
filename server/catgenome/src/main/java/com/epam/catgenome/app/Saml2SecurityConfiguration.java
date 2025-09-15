package com.epam.catgenome.app;

import com.epam.catgenome.security.saml2.CustomResponseAuthenticationConverter;
import com.epam.catgenome.security.saml2.LbAwareRelyingPartyRegistrationResolver;
import com.epam.catgenome.security.saml2.LbConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.*;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Order(2)
@ConditionalOnProperty(value = "saml.security.enable", havingValue = "true")
public class Saml2SecurityConfiguration {

    @Value("${files.root.directory.path:/opt/ngb}")
    private String rootDirectoryPath;

    @Value("${server.ssl.endpoint.id}")
    private String entityId;

    @Value("${saml.lb.enabled:false}")
    private boolean loadBalancerEnabled;

    @Value("${saml.lb.scheme}")
    private String loadBalancerScheme;

    @Value("${saml.lb.server.name}")
    private String loadBalancerServerName;

    @Value("${saml.lb.include.port.in.request:false}")
    private boolean loadBalancerIncludeServerPortInRequestURL;

    @Value("${saml.lb.server.port:443}")
    private int loadBalancerServerPort;

    @Value("${saml.lb.context.path}")
    private String loadBalancerContextPath;

    @Value("${security.frame-options.disable:false}")
    private boolean frameOptionsDisable;

    @Value("${saml.login.failure.redirect:/error-401.html}")
    private String loginFailureRedirect;

    @Value("${server.ssl.metadata}")
    private String metadataLocation;

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws Exception {

        // Load keystore
        KeyStore ks = loadKeyStore();
        PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, keyStorePassword.toCharArray());

        // Retrieve the certificate from the keystore
        X509Certificate certificate = (X509Certificate) ks.getCertificate(keyAlias);

        // Build the SP signing credential
        Saml2X509Credential signingCredential = Saml2X509Credential.signing(privateKey, certificate);
        Saml2X509Credential decryptCredential = Saml2X509Credential.decryption(privateKey, certificate);

        // Create registration from metadata
        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(String.format("file:%s/%s", rootDirectoryPath, metadataLocation))
                .registrationId("catgenome")
                .entityId(entityId)
                .singleLogoutServiceBinding(Saml2MessageBinding.REDIRECT)
                .signingX509Credentials(c -> c.add(signingCredential))
                .decryptionX509Credentials(c -> c.add(decryptCredential))
                .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    private KeyStore loadKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = new FileInputStream(String.format("%s/%s", rootDirectoryPath, keyStorePath))) {
            keyStore.load(is, keyStorePassword.toCharArray());
        }
        return keyStore;
    }

    @Bean
    public OpenSaml4AuthenticationProvider saml2AuthenticationProvider(
            CustomResponseAuthenticationConverter customResponseAuthenticationConverter) {

        OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
        authenticationProvider.setResponseAuthenticationConverter(customResponseAuthenticationConverter);

        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain saml2SecurityFilterChain(HttpSecurity http,
                                                        LbAwareRelyingPartyRegistrationResolver lbAwareResolver, OpenSaml4AuthenticationProvider saml2AuthenticationProvider) throws Exception {

        http.securityMatcher(getSecuredRequestMatcher())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(getUnsecuredResources()).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(withDefaults());

        // Updated configuration with correct failure handler method
        http.saml2Login(saml2 -> saml2
                .authenticationManager(new ProviderManager(saml2AuthenticationProvider))
                .failureHandler(authenticationFailureHandler()) // Use failureHandler instead of authenticationFailureHandler
        );

        // Updated logout configuration
        http.saml2Logout(withDefaults());

        Saml2MetadataFilter filter = new Saml2MetadataFilter(
                lbAwareResolver,
                new OpenSamlMetadataResolver()
        );
        http.addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class);

        if (frameOptionsDisable) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        }

        return http.build();
    }

    @Bean
    public LbAwareRelyingPartyRegistrationResolver relyingPartyRegistrationResolver() throws Exception {

        LbConfig lbConfig = new LbConfig();
        lbConfig.setEnabled(loadBalancerEnabled);
        lbConfig.setScheme(loadBalancerScheme);
        lbConfig.setServerName(loadBalancerServerName);
        lbConfig.setServerPort(loadBalancerServerPort);
        lbConfig.setContextPath(loadBalancerContextPath);
        lbConfig.setIncludeServerPortInRequestURL(loadBalancerIncludeServerPortInRequestURL);
        lbConfig.validate();

        return new LbAwareRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository(), lbConfig, entityId);
    }

    @Bean
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setDefaultFailureUrl(loginFailureRedirect);
        return failureHandler;
    }

    private OrRequestMatcher getSecuredRequestMatcher() {
        List<AntPathRequestMatcher> matchers = Arrays.stream(getSecuredResourcesRoot())
                .map(AntPathRequestMatcher::new)
                .toList();
        return new OrRequestMatcher(matchers.toArray(new AntPathRequestMatcher[0]));
    }

    public String[] getUnsecuredResources() {
        return new String[]{"/saml2/web/**", "/swagger-ui/**", "/api-docs/**", "/error-401.html"};
    }

    public String[] getSecuredResourcesRoot() {
        return new String[]{"/**"};
    }
}
