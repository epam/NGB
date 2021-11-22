package com.epam.catgenome.app;

import java.io.PrintStream;

import com.epam.catgenome.util.NgbSeekableStreamFactory;
import com.epam.catgenome.util.aws.S3Client;
import com.epam.catgenome.util.azure.AzureBlobClient;
import htsjdk.samtools.seekablestream.ISeekableStreamFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Main entry point for Spring Boot Application
 */
@Import(AppConfiguration.class)
@EnableWebSecurity
@EnableScheduling
@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        FallbackWebSecurityAutoConfiguration.class,
        OAuth2AutoConfiguration.class})
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
    public AzureBlobClient azureBlobClient(@Value("${azure.storage.account:}") final String storageAccount,
                                           @Value("${azure.storage.key:}") final String storageKey) {
        if (StringUtils.isEmpty(storageAccount) || StringUtils.isEmpty(storageKey)) {
            log.debug("Azure authentication is not configured");
            return new AzureBlobClient();
        }
        return new AzureBlobClient(storageAccount, storageKey);
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
