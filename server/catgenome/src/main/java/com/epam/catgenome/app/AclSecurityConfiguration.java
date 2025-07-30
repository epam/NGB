/*
 * MIT License
 *
 * Copyright (c) 2019-2021 EPAM Systems
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

import static com.epam.catgenome.entity.user.DefaultRoles.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.cache.CacheManager;
import javax.sql.DataSource;

import com.epam.catgenome.security.acl.customexpression.NGBMethodSecurityExpressionHandler;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.security.acl.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ComponentScan(basePackages = "com.epam.catgenome.security.acl")
@ImportResource("classpath*:conf/catgenome/acl-dao.xml")
public class AclSecurityConfiguration extends GlobalMethodSecurityConfiguration {

    private static final int UNLIMITED_NUMBER_OF_ENTITIES = 0;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PermissionFactory permissionFactory;

    @Autowired
    private JdbcMutableAclService jdbcMutableAclService;

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        NGBMethodSecurityExpressionHandler expressionHandler =
            new NGBMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator());
        expressionHandler.setRoleHierarchy(roleHierarchy());
        expressionHandler.setApplicationContext(context);
        expressionHandler.setPermissionHelper(context.getBean(PermissionHelper.class));
        return expressionHandler;
    }

    @Bean
    public SidRetrievalStrategy sidRetrievalStrategy() {
        return new SidRetrievalStrategyImpl(roleHierarchy());
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(ROLE_ADMIN.getName() + " > " +
                ROLE_USER.getName());

        List<DefaultRoles> managerRoles = Arrays.asList(ROLE_REFERENCE_MANAGER, ROLE_BAM_MANAGER, ROLE_VCF_MANAGER,
                ROLE_GENE_MANAGER, ROLE_BED_MANAGER, ROLE_WIG_MANAGER, ROLE_SEG_MANAGER);

        managerRoles.forEach(role -> roleHierarchy.setHierarchy(ROLE_ADMIN.getName() + " > " + role.getName()));
        roleHierarchy.setHierarchy(managerRoles.stream().map(DefaultRoles::getName)
                .collect(Collectors.joining(" == ")));
        managerRoles.forEach(role -> roleHierarchy.setHierarchy(role.getName() + " > " + ROLE_USER.getName()));

        return roleHierarchy;
    }

    @Bean
    public PermissionEvaluator permissionEvaluator() {
        AclPermissionEvaluator evaluator = new AclPermissionEvaluator(jdbcMutableAclService);
        evaluator.setPermissionFactory(permissionFactory);
        return evaluator;
    }

    /*@Bean
    public JdbcMutableAclService jdbcMutableAclService() {
        return new JdbcMutableAclServiceImpl(dataSource, lookupStrategy(), aclCache());
    }*/

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
        return new SpringCacheBasedAclCache(cache((ehCacheManagerFactoryBean())),
                permissionGrantingStrategy(), aclAuthorizationStrategy());
    }

    public Cache cache(JCacheManagerFactoryBean ehCacheManagerFactoryBean) {
        int aclSecurityCachePeriodInSeconds = context.getEnvironment()
                .getProperty("security.acl.cache.period", Integer.class, -1);
        CacheManager object = ehCacheManagerFactoryBean.getObject();

        CacheConfigurationBuilder<Object, Object> objectObjectCacheConfigurationBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100, EntryUnit.ENTRIES).build());
        CacheConfiguration<Object, Object> build = objectObjectCacheConfigurationBuilder
                .build();


        CacheConfigurationBuilder<Object, Object> configuration =
                CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder
                                .heap(100))
                        .withExpiry(new ExpiryPolicy<Object, Object>() {
                            @Override
                            public Duration getExpiryForCreation(Object key, Object value) {
                                return Duration.ofSeconds(aclSecurityCachePeriodInSeconds);
                            }

                            @Override
                            public Duration getExpiryForAccess(Object key, Supplier<? extends Object> value) {
                                return Duration.ofSeconds(aclSecurityCachePeriodInSeconds);
                            }

                            @Override
                            public Duration getExpiryForUpdate(Object key, Supplier<? extends Object> oldValue, Object newValue) {
                                return Duration.ofSeconds(aclSecurityCachePeriodInSeconds);  // Keeping the existing expiry
                            }
                        });

        return new JCacheCache(object.createCache("aclCache", Eh107Configuration.fromEhcacheCacheConfiguration(configuration) ));
    }

    @Bean
    public JCacheManagerFactoryBean ehCacheManagerFactoryBean() {
        JCacheManagerFactoryBean factoryBean = new JCacheManagerFactoryBean();
        return factoryBean;
    }
}
