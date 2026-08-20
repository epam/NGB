package com.epam.catgenome.app;

import java.io.PrintStream;

import com.epam.catgenome.util.NgbSeekableStreamFactory;
import com.epam.catgenome.util.aws.S3Client;
import com.epam.catgenome.util.azure.AzureBlobClient;
import com.epam.catgenome.util.azure.AzureCredentialConfiguration;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Main entry point for Spring Boot Application
 *
 * <p>There is deliberately no {@code @EnableScheduling} here: {@code
 * conf/catgenome/applicationContext-cache.xml} declares {@code <task:annotation-driven
 * executor="taskExecutor"/>}, which registers the very same {@code
 * internalScheduledAnnotationProcessor} bean (and the {@code @Async} one bound to {@code
 * taskExecutor} besides). Boot 1.5 let the two definitions override each other silently; from
 * Boot 2.1 that is a startup failure, and the XML declaration is the superset of the two.
 */
@Import(AppConfiguration.class)
@EnableWebSecurity
// NGB configures its own web security, so none of Boot's security auto-configuration applies.
// Boot 1.5 also listed FallbackWebSecurityAutoConfiguration here; that class is gone in Boot 2.x.
// UserDetailsServiceAutoConfiguration takes its place in the list: the default in-memory user it
// creates used to be part of SecurityAutoConfiguration, and became a top-level auto-configuration
// of its own in Boot 2.0, so excluding SecurityAutoConfiguration alone no longer suppresses it.
// TransactionAutoConfiguration is excluded for the same "we configure it ourselves" reason:
// applicationContext-database.xml declares <tx:annotation-driven transaction-manager="txManager"/>,
// and Boot's @ConditionalOnMissingBean(AbstractTransactionManagementConfiguration.class) guard
// cannot see an XML declaration, so both would register internalTransactionAdvisor. Nothing here
// injects the TransactionTemplate that this auto-configuration otherwise contributes.
//
// SecurityFilterAutoConfiguration, on the other hand, is deliberately NO LONGER excluded, even
// though Boot 1.5 excluded it. It is the auto-configuration that registers springSecurityFilterChain
// with the servlet container at order -100. Without it, that filter is picked up only by Boot's
// generic "any Filter bean is a servlet filter" adaptation, which gives it LOWEST_PRECEDENCE - and
// so do the ten Filter beans SAMLSecurityConfiguration declares (samlEntryPoint, samlIDPDiscovery,
// samlWebSSOProcessingFilter, samlFilter, ...), which are meant to be reached only through
// samlFilter inside the security chain. Ties are then broken by bean-definition order, which under
// Boot 1.5 happened to put the security chain first and under Boot 2.7 puts it last: /saml/** was
// being served by the standalone SAML filters, so the SSO login authenticated and redirected without
// SecurityContextPersistenceFilter ever storing the result, and the very next request was anonymous
// again. Letting Boot order the chain properly is the fix; nothing else in this auto-configuration
// contributes a bean (the registration is @ConditionalOnBean(name = "springSecurityFilterChain"),
// so with AUTH_MODE=none it still does nothing).
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        TransactionAutoConfiguration.class})
@Slf4j
public class Application extends SpringBootServletInitializer {

    @Autowired
    private Environment environment;

    @Value("${swift.stack.endpoint.url:}")
    private String swsEndpoint;

    @Value("${swift.stack.region:}")
    private String swsRegion;

    @Value("${swift.stack.path.style.access:false}")
    private boolean isPathStyleAccess;

    @Value("${request.logging.filter.max.payload.length:64000}")
    private int maxPayloadLength;

    @Override protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @EventListener
    public void startupLoggingListener(ApplicationReadyEvent event) {
        print(String.format("NGB Browser started on port: %s (http).",
                                environment.getProperty("local.server.port")), System.out);
    }

    private void print(String message, PrintStream stream) {
        stream.println(message);
    }

    @Bean
    ISeekableStreamFactory ngbSeekableStreamFactory() {
        return NgbSeekableStreamFactory.getInstance();
    }

    @Bean
    S3Client s3Client() {
        return S3Client.configure(swsEndpoint, swsRegion, isPathStyleAccess);
    }

    @Bean
    public AzureBlobClient azureBlobClient(@Value("${azure.storage.account:}")  String storageAccount,
                                           @Value("${azure.storage.key:}") String storageKey,
                                           @Value("${azure.storage.managed_identity_id:}") String managedIdentityId,
                                           @Value("${azure.storage.tenant_id:}") String tenantId,
                                           @Value("${azure.storage.client_id:}") String clientId,
                                           @Value("${azure.storage.client_secret:}") String clientSecret) {

        if (StringUtils.isEmpty(storageAccount)) {
            log.debug("Azure connectivity is not configured.");
            return new AzureBlobClient();
        }

        if (StringUtils.isNotEmpty(storageKey)) {
            log.debug("Creating AzureBlobClient using storage account access key credentials.");
            return new AzureBlobClient(AzureCredentialConfiguration.byAccessKey()
                    .storageAccount(storageAccount)
                    .storageKey(storageKey)
                    .build());
        }
        if (StringUtils.isNotEmpty(clientId) && StringUtils.isNotEmpty(clientSecret) &&
                StringUtils.isNotEmpty(tenantId)) {
            log.debug("Creating AzureBlobClient using service principal id: {}.", clientId);
            return new AzureBlobClient(AzureCredentialConfiguration.byServicePrincipal()
                    .storageAccount(storageAccount)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .build());
        }

        log.debug("Creating AzureBlobClient with token credentials.");
        return new AzureBlobClient(AzureCredentialConfiguration.byDefault()
                .storageAccount(storageAccount)
                .managedIdentityId(managedIdentityId)
                .tenantId(tenantId)
                .build());
    }

    @Bean
    public CustomRequestLoggingFilter requestLoggingFilter() {
        CustomRequestLoggingFilter loggingFilter = new CustomRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(maxPayloadLength);
        return loggingFilter;
    }
}
