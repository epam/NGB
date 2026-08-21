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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import com.epam.catgenome.security.acl.customexpression.NGBMethodSecurityExpressionHandler;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.security.acl.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Method-security and ACL wiring. Reworked for Spring Security 6 in Phase 3 of the Java 21
 * migration; three things about the shape of this class are consequences of that and not choices:
 *
 * <ul>
 *   <li>{@code GlobalMethodSecurityConfiguration} and {@code @EnableGlobalMethodSecurity} were
 *       removed. The replacement, {@code @EnableMethodSecurity}, has no {@code createExpressionHandler()}
 *       hook: it picks up a {@link MethodSecurityExpressionHandler} <em>bean</em> instead. That bean is
 *       consumed by the method-security interceptors, which are {@code ROLE_INFRASTRUCTURE} beans
 *       built before any application bean exists, so it is declared {@code static} - a non-static
 *       {@code @Bean} would drag this whole configuration class, its {@code DataSource} and the ACL
 *       service into that early phase.</li>
 *   <li>Because the handler bean is static it cannot call {@code roleHierarchy()}, so
 *       {@link RoleHierarchy} is injected into it as a method parameter, and
 *       {@code sidRetrievalStrategy} takes it the same way rather than calling the {@code @Bean}
 *       method. {@code prePostEnabled} is not passed to {@code @EnableMethodSecurity}: it defaults to
 *       true there.</li>
 *   <li>{@code EhCacheBasedAclCache} is gone with the rest of Spring's EhCache 2 support. The ACL
 *       cache is now {@link SpringCacheBasedAclCache} over a Caffeine region, and the EhCache
 *       configuration it replaces is reproduced in {@link #aclCacheManager()}.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
@EnableMethodSecurity(securedEnabled = true)
@ComponentScan(basePackages = "com.epam.catgenome.security.acl")
@ImportResource("classpath*:conf/catgenome/acl-dao.xml")
public class AclSecurityConfiguration {

    private static final String ACL_CACHE = "aclCache";

    /**
     * EhCache's failsafe defaults, which is what the {@code aclCache} region got whenever
     * {@code security.acl.cache.period} was unset or non-positive: 10,000 entries, 120 s
     * time-to-live and time-to-idle. Spelled out here because nothing else carries them now that
     * ehcache.xml is deleted - and the region was never in ehcache.xml to begin with, it was built
     * by an {@code EhCacheFactoryBean} against a default {@code CacheManager}.
     */
    private static final long DEFAULT_MAX_ENTRIES = 10_000;
    private static final long DEFAULT_CACHE_PERIOD_SECONDS = 120;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PermissionFactory permissionFactory;

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(final ApplicationContext context,
                                                                          final RoleHierarchy roleHierarchy) {
        // PermissionEvaluator and PermissionHelper are looked up through suppliers rather than
        // injected: this bean is created during infrastructure setup, and resolving either of them
        // here would pull the ACL service - and with it the DataSource and the acl-dao.xml beans - up
        // with it. The suppliers are called from createSecurityExpressionRoot, i.e. inside a request.
        NGBMethodSecurityExpressionHandler expressionHandler = new NGBMethodSecurityExpressionHandler(
                () -> context.getBean(PermissionEvaluator.class),
                () -> context.getBean(PermissionHelper.class));
        expressionHandler.setRoleHierarchy(roleHierarchy);
        expressionHandler.setApplicationContext(context);
        return expressionHandler;
    }

    @Bean
    public SidRetrievalStrategy sidRetrievalStrategy(final RoleHierarchy roleHierarchy) {
        return new SidRetrievalStrategyImpl(roleHierarchy);
    }

    /**
     * {@code ROLE_ADMIN} implies {@code ROLE_USER} and every per-format manager role, and each manager
     * role implies {@code ROLE_USER}.
     *
     * <p>This is a behaviour change, and a bug fix. The previous version called
     * {@code RoleHierarchyImpl.setHierarchy} sixteen times, once per edge - but {@code setHierarchy}
     * <em>replaces</em> the hierarchy rather than adding to it, so fifteen of the sixteen calls were
     * immediately discarded and the only edge that ever took effect was the last one written,
     * {@code ROLE_SEG_MANAGER > ROLE_USER}. What changes now that all fifteen intended edges are
     * actually in place: a principal holding {@code ROLE_ADMIN} passes checks written against
     * {@code ROLE_USER} or a manager role, a manager passes checks written against {@code ROLE_USER},
     * and - because {@link SidRetrievalStrategyImpl} expands authorities through this hierarchy too -
     * ACL entries granted to {@code ROLE_USER} now also apply to admins and managers. Users get
     * {@code ROLE_USER} from {@code RoleManager.getDefaultRolesIds} on registration, so in practice the
     * effective new grant is {@code ROLE_ADMIN} over the manager roles.
     *
     * <p>One edge from the old code is deliberately <em>not</em> reinstated: the manager roles were
     * joined with {@code " == "} in an attempt to make them mutually equivalent. No version of
     * {@code RoleHierarchyImpl} has ever parsed {@code ==} - the grammar is {@code A > B}, one edge per
     * line - so that call only ever wiped the map, and the relation it describes would let, say, a BAM
     * manager act as a VCF manager, which is the opposite of why the roles are separate.
     *
     * <p>Built with {@link RoleHierarchyImpl#fromHierarchy(String)} because {@code setHierarchy} is
     * deprecated in Spring Security 6.3 precisely for inviting the mistake above.
     */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        final List<DefaultRoles> managerRoles = Arrays.asList(ROLE_REFERENCE_MANAGER, ROLE_BAM_MANAGER,
                ROLE_VCF_MANAGER, ROLE_GENE_MANAGER, ROLE_BED_MANAGER, ROLE_WIG_MANAGER, ROLE_SEG_MANAGER);

        final List<String> hierarchy = new ArrayList<>();
        hierarchy.add(ROLE_ADMIN.getName() + " > " + ROLE_USER.getName());
        managerRoles.forEach(role -> {
            hierarchy.add(ROLE_ADMIN.getName() + " > " + role.getName());
            hierarchy.add(role.getName() + " > " + ROLE_USER.getName());
        });

        return RoleHierarchyImpl.fromHierarchy(String.join("\n", hierarchy));
    }

    /**
     * Takes the ACL service as a method parameter rather than an {@code @Autowired} field, because
     * the two are mutually dependent: the {@code jdbcMutableAclService} bean from the imported
     * conf/catgenome/acl-dao.xml is {@code autowire="constructor"} over {@link LookupStrategy} and
     * {@code aclCache()}, both defined here - so it cannot be
     * built until this configuration class exists, while a field would have required it to exist
     * before this class could be instantiated. Boot 2.6 turned that cycle from a warning into a
     * startup failure ({@code spring.main.allow-circular-references} defaults to false), and as a
     * parameter it is only resolved when this bean is actually created, by which time
     * {@code lookupStrategy()} and {@code aclCache()} are available. Only reachable with
     * security.acl.enable=true, which is why the unauthenticated profile never saw it.
     */
    @Bean
    public PermissionEvaluator permissionEvaluator(final JdbcMutableAclService jdbcMutableAclService) {
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
        return new SpringCacheBasedAclCache(aclCacheManager().getCache(ACL_CACHE),
                permissionGrantingStrategy(), aclAuthorizationStrategy());
    }

    /**
     * A cache manager of its own, not the application's {@code cacheManager} from
     * conf/catgenome/applicationContext-cache.xml: the ACL region is configured from
     * {@code security.acl.cache.period} at startup, and the EhCache setup this replaces likewise
     * built it in a separate, privately named {@code CacheManager} ("aclCacheManager").
     *
     * <p>What the EhCache version did, and what is reproduced here: with a positive
     * {@code security.acl.cache.period} the region was unbounded in entries and expired
     * {@code period} seconds after write and after access; otherwise it fell through to EhCache's
     * failsafe defaults of 10,000 entries and 120 s. The one thing not carried over is
     * {@code pinning(LOCALMEMORY)}, which told EhCache not to spill this region to disk - Caffeine is
     * heap-only, so it is already true by construction.
     */
    @Bean
    public CaffeineCacheManager aclCacheManager() {
        final Environment environment = context.getEnvironment();
        final int period = environment.getProperty("security.acl.cache.period", Integer.class, -1);

        final Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        if (period > 0) {
            caffeine.expireAfterWrite(period, TimeUnit.SECONDS)
                    .expireAfterAccess(period, TimeUnit.SECONDS);
        } else {
            caffeine.maximumSize(DEFAULT_MAX_ENTRIES)
                    .expireAfterWrite(DEFAULT_CACHE_PERIOD_SECONDS, TimeUnit.SECONDS)
                    .expireAfterAccess(DEFAULT_CACHE_PERIOD_SECONDS, TimeUnit.SECONDS);
        }

        final CaffeineCacheManager cacheManager = new CaffeineCacheManager(ACL_CACHE);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
