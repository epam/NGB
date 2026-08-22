/*
 * MIT License
 *
 * Copyright (c) 2016-2026 EPAM Systems
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

package com.epam.catgenome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Replaces {@code SwaggerConfig}, which drove {@code com.mangofactory:swagger-springmvc} 1.0.2 and
 * had to go: that plugin cannot work past Boot 2.6, whose {@code PathPatternParser} it does not
 * understand. springdoc-openapi needs no plugin bean: it discovers the controllers by
 * itself and serves the document at {@code /v3/api-docs} and the UI at {@code /swagger-ui/index.html}.
 * All this class still has to say is the descriptive metadata that used to come out of
 * {@code SwaggerConfig.apiInfo()}, carried over verbatim.
 *
 * <p>Two things the old plugin configured explicitly and springdoc does by default, so they are not
 * repeated here: {@code includePatterns(".*")} (springdoc documents every mapped handler unless told
 * otherwise) and {@code useDefaultResponseMessages(false)} (springdoc never invents response entries
 * that the code does not declare). The {@code pathProvider} override, which prefixed the document
 * paths with the servlet context path, is likewise unnecessary: springdoc resolves the context path
 * from the request.
 */
@Configuration
public class OpenApiConfiguration {

    private static final String API_VERSION = "1.0";

    @Bean
    public OpenAPI catgenomeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CATGenome Browser REST API")
                .description("CATGenome Browser API description.")
                .version(API_VERSION)
                .termsOfService("API TOS")
                .contact(new Contact().email("Denis_Medvedev@epam.com"))
                .license(new License().name("API License").url("API License URL")));
    }
}
