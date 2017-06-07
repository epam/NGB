package com.epam.catgenome.app;

import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

/**
 * Enables configuration of Tomcat container, should be used only with embedded container
 */
public interface TomcatConfigurer {
    void configure(TomcatEmbeddedServletContainerFactory tomcat, int cacheSize,
            int tomcatCacheSize);
}
