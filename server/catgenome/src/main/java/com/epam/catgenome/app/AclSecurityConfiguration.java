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

import com.epam.catgenome.entity.user.DefaultRoles;
import com.epam.catgenome.security.acl.JdbcMutableAclServiceImpl;
import com.epam.catgenome.security.acl.LookupStrategyImpl;
import com.epam.catgenome.security.acl.PermissionGrantingStrategyImpl;
import com.epam.catgenome.security.acl.PermissionHelper;
import com.epam.catgenome.security.acl.customexpression.NGBMethodSecurityExpressionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.catgenome.entity.user.DefaultRoles.*;

@Configuration
@ConditionalOnProperty(value = "security.acl.enable", havingValue = "true")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackages = "com.epam.catgenome.security.acl")
public class AclSecurityConfiguration {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PermissionFactory permissionFactory;

    @Autowired
    private CacheManager cacheManager;

    @Bean
    public JdbcMutableAclServiceImpl jdbcMutableAclService() {
        JdbcMutableAclServiceImpl service = new JdbcMutableAclServiceImpl(dataSource, lookupStrategy(), aclCache());

        service.setClassIdentityQuery("SELECT currval('catgenome.acl_class_id_seq')");
        service.setSidIdentityQuery("SELECT currval('catgenome.acl_sid_id_seq')");
        service.setSidPrimaryKeyQuery("select id from catgenome.acl_sid where principal=? and sid=?");
        service.setInsertSidSql("insert into catgenome.acl_sid (principal, sid) values (?, ?)");
        service.setClassPrimaryKeyQuery("select id from catgenome.acl_class where class=?");
        service.setDeleteEntryByObjectIdentityForeignKeySql("delete from catgenome.acl_entry where acl_object_identity=?");
        service.setDeleteObjectIdentityByPrimaryKeySql("delete from catgenome.acl_object_identity where id=?");
        service.setFindChildrenQuery("select obj.object_id_identity as obj_id, class.class as class " +
                "from catgenome.acl_object_identity obj, catgenome.acl_object_identity parent, catgenome.acl_class class " +
                "where obj.parent_object = parent.id " +
                "and obj.object_id_class = class.id " +
                "and parent.object_id_identity = cast(? as bigint) " +
                "and parent.object_id_class = ( " +
                "    select id FROM catgenome.acl_class where acl_class.class = ? " +
                ")");
        service.setInsertClassSql("insert into catgenome.acl_class (class) values (?)");
        service.setInsertEntrySql("insert into catgenome.acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure) values (?, ?, ?, ?, ?, ?, ?)");
        service.setInsertObjectIdentitySql("insert into catgenome.acl_object_identity (object_id_class, object_id_identity, owner_sid, entries_inheriting) values (?, cast(? as bigint), ?, ?)");
        service.setObjectIdentityPrimaryKeyQuery("select acl_object_identity.id " +
                "from catgenome.acl_object_identity, catgenome.acl_class " +
                "where acl_object_identity.object_id_class = acl_class.id and acl_class.class=? " +
                "and acl_object_identity.object_id_identity = cast(? as bigint)");
        service.setUpdateObjectIdentity("update catgenome.acl_object_identity set parent_object = ?, owner_sid = ?, entries_inheriting = ? where id = ?");

        // Set custom queries for JdbcMutableAclServiceImpl
        service.setDeleteEntriesBySidQuery("delete from catgenome.acl_entry where sid=?");
        service.setDeleteSidByIdQuery("delete from catgenome.acl_sid where id=?");
        service.setLoadEntriesBySidsCountQuery("SELECT count(*) FROM catgenome.acl_entry where sid IN (@in@)");

        return service;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        NGBMethodSecurityExpressionHandler expressionHandler = new NGBMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator());
        expressionHandler.setRoleHierarchy(roleHierarchy());
        expressionHandler.setApplicationContext(context);
        expressionHandler.setPermissionHelper(context.getBean(PermissionHelper.class));
        return expressionHandler;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(ROLE_ADMIN.getName() + " > " + ROLE_USER.getName());

        List<DefaultRoles> managerRoles = Arrays.asList(
                ROLE_REFERENCE_MANAGER, ROLE_BAM_MANAGER, ROLE_VCF_MANAGER,
                ROLE_GENE_MANAGER, ROLE_BED_MANAGER, ROLE_WIG_MANAGER, ROLE_SEG_MANAGER
        );

        for (DefaultRoles role : managerRoles) {
            roleHierarchy.setHierarchy(ROLE_ADMIN.getName() + " > " + role.getName());
        }

        // All manager roles are equivalent
        roleHierarchy.setHierarchy(managerRoles.stream()
                .map(DefaultRoles::getName)
                .collect(Collectors.joining(" == ")));

        for (DefaultRoles role : managerRoles) {
            roleHierarchy.setHierarchy(role.getName() + " > " + ROLE_USER.getName());
        }

        return roleHierarchy;
    }

    @Bean
    public PermissionEvaluator permissionEvaluator() {
        AclPermissionEvaluator evaluator = new AclPermissionEvaluator(jdbcMutableAclService());
        evaluator.setPermissionFactory(permissionFactory);
        return evaluator;
    }

    @Bean
    public SidRetrievalStrategy sidRetrievalStrategy() {
        return new SidRetrievalStrategyImpl(roleHierarchy());
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
    public LookupStrategy lookupStrategy() {
        return new LookupStrategyImpl(dataSource, aclCache(), aclAuthorizationStrategy(),
                auditLogger(), permissionFactory, permissionGrantingStrategy());
    }

    @Bean
    public AclCache aclCache() {
        org.springframework.cache.Cache springCache = cacheManager.getCache("aclCache");
        if (springCache == null) {
            throw new IllegalStateException("Cache 'aclCache' not found in CacheManager");
        }
        return new SpringCacheBasedAclCache(springCache, permissionGrantingStrategy(), aclAuthorizationStrategy());
    }
}
