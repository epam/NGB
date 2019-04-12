/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2019 EPAM Systems
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package com.epam.catgenome.app;

import com.epam.catgenome.security.acl.LookupStrategyImpl;
import com.epam.catgenome.security.acl.PermissionGrantingStrategyImpl;
import net.sf.ehcache.config.PinningConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;

import static com.epam.catgenome.entity.user.DefaultRoles.*;

@Configuration
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
public class AclCacheConfiguration {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PermissionFactory permissionFactory;

    @Value("${security.acl.cache.period:-1}")
    private int aclSecurityCachePeriodInSeconds;

    @Bean
    public LookupStrategy lookupStrategy() {
        return new LookupStrategyImpl(dataSource, aclCache(), aclAuthorizationStrategy(),
                                      auditLogger(), permissionFactory, permissionGrantingStrategy());
    }

    @Bean
    public AuditLogger auditLogger() {
        return new ConsoleAuditLogger();
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(new SimpleGrantedAuthority(ROLE_ADMIN.getName()));
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy() {
        return new PermissionGrantingStrategyImpl(auditLogger());
    }

    @Bean
    public AclCache aclCache() {
        return new EhCacheBasedAclCache(ehCacheFactoryBean().getObject(),
                permissionGrantingStrategy(), aclAuthorizationStrategy());
    }

    @Bean
    public EhCacheFactoryBean ehCacheFactoryBean() {
        System.err.println(aclSecurityCachePeriodInSeconds);
        EhCacheFactoryBean factoryBean = new EhCacheFactoryBean();
        factoryBean.setCacheManager(ehCacheManagerFactoryBean().getObject());
        factoryBean.setCacheName("aclCache");
        if (aclSecurityCachePeriodInSeconds > 0) {
            factoryBean.maxEntriesLocalHeap(Integer.MAX_VALUE - 1);
            factoryBean.setTimeToLive(aclSecurityCachePeriodInSeconds);
            factoryBean.setTimeToIdle(aclSecurityCachePeriodInSeconds);
            factoryBean.pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALMEMORY));
        }
        return factoryBean;
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheManagerFactoryBean() {
        EhCacheManagerFactoryBean factoryBean = new EhCacheManagerFactoryBean();
        factoryBean.setCacheManagerName("aclCacheManager");
        return factoryBean;
    }
}
