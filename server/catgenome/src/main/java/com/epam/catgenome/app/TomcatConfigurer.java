package com.epam.catgenome.app;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

/**
 * Enables configuration of Tomcat container, should be used only with embedded container
 */
public interface TomcatConfigurer {
    void configure(TomcatServletWebServerFactory tomcat, int cacheSize,
                   int tomcatCacheSize);
}
