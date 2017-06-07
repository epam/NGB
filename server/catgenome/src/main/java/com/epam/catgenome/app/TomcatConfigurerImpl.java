package com.epam.catgenome.app;

import org.apache.catalina.webresources.StandardRoot;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

public class TomcatConfigurerImpl implements TomcatConfigurer {

    public TomcatConfigurerImpl() {
        // no op
    }

    @Override
    public void configure(TomcatEmbeddedServletContainerFactory tomcat, int cacheSize,
            int tomcatCacheSize) {
        tomcat.addContextCustomizers((context) -> {
            StandardRoot standardRoot = new StandardRoot(context);
            standardRoot.setCachingAllowed(true);
            standardRoot.setCacheMaxSize(cacheSize);
            standardRoot.setCacheTtl(tomcatCacheSize);
            context.setResources(standardRoot);
        });
    }
}
