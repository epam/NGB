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

import java.util.List;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.epam.catgenome.config.SwaggerConfig;
import com.epam.catgenome.controller.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class provides MVC Configuration for Spring Boot application
 */
@Configuration
@Import(SwaggerConfig.class)
@ComponentScan(basePackages = {"com.epam.catgenome.config", "com.epam.catgenome.controller"})
public class AppMVCConfiguration extends WebMvcConfigurerAdapter {

    private static final int MILLISECONDS = 1000;
    private static final int CACHE_SIZE = 1024 * 1024 * 100;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("#{catgenome['use.embedded.tomcat']}")
    private String useEmbedded;

    @Value("#{catgenome['request.async.timeout'] ?: 10000}")
    private long asyncTimeout;

    @Value("#{catgenome['static.resources.cache.period'] ?: 86400}")
    private int staticResourcesCachePeriod;

    @Bean
    public TomcatConfigurer tomcatConfigurerImpl() {
        if (useEmbeddedContainer()) {
            return new TomcatConfigurerImpl();
        } else {
            return null;
        }
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(asyncTimeout);
        configurer.setTaskExecutor(new TaskExecutorAdapter(Executors.newCachedThreadPool()));
    }

    @Bean
    @ConditionalOnClass({EmbeddedServletContainerFactory.class })
    public EmbeddedServletContainerCustomizer tomcatContainerCustomizer() {
        return container -> {
            TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
            tomcat.setTldSkip("*.jar");
            if (useEmbeddedContainer() && staticResourcesCachePeriod > 0) {
                TomcatConfigurer configurer = applicationContext.getBean(TomcatConfigurer.class);
                configurer.configure(tomcat, CACHE_SIZE, staticResourcesCachePeriod * MILLISECONDS);
            }
        };
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("/swagger-ui/", "classpath:/static/swagger-ui/",
                        "classpath:/META-INF/resources/webjars/swagger-ui/2.0.24/");
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(staticResourcesCachePeriod);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        converters.add(converter);
        super.configureMessageConverters(converters);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        super.configurePathMatch(configurer);
        configurer.setUseSuffixPatternMatch(false);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new JsonMapper();
    }

    @Bean
    public SwaggerConfig swaggerConfig() {
        return new SwaggerConfig();
    }

    /*@Bean //TODO: may be useful if we'll need to move swagger back to restapi
    public ServletRegistrationBean dispatcherRegistration(DispatcherServlet dispatcherServlet) {
        ServletRegistrationBean bean =
                new ServletRegistrationBean(dispatcherServlet, "/restapi/*");
        bean.setAsyncSupported(true);
        bean.setName("catgenome");
        bean.setLoadOnStartup(1);
        return bean;
    }*/

    private boolean useEmbeddedContainer() {
        return useEmbedded != null && "true".equals(useEmbedded);
    }
}
