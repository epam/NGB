/*
 * MIT License
 *
 * Copyright (c) 2018 EPAM Systems
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

import static com.epam.catgenome.security.DefaultRoles.ROLE_ADMIN;
import static com.epam.catgenome.security.DefaultRoles.ROLE_BAM_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_BED_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_CYTOBANDS_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_GENE_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_MAF_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_REFERENCE_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_SEG_MANAGER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_USER;
import static com.epam.catgenome.security.DefaultRoles.ROLE_VCF_MANAGER;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.PermissionFactory;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

import com.epam.catgenome.security.DefaultRoles;
import com.epam.catgenome.security.acl.AclPermission;

@Configuration
@ConditionalOnProperty(value = "acl.security.enable", havingValue = "true")
@ComponentScan(basePackages = "com.epam.catgenome.security.acl")
public class AclSecurityConfiguration extends GlobalMethodSecurityConfiguration {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcMutableAclService jdbcMutableAclService;

    @Bean
    public PermissionFactory permissionFactory() {
        return new DefaultPermissionFactory(AclPermission.class);
    }

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler =
            new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator());
        expressionHandler.setRoleHierarchy(roleHierarchy());
        expressionHandler.setApplicationContext(context);
        return expressionHandler;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy(ROLE_ADMIN.getName() + " > " +
                                   ROLE_USER.getName());

        Stream<DefaultRoles> managerRoles = Stream.of(ROLE_REFERENCE_MANAGER, ROLE_BAM_MANAGER, ROLE_VCF_MANAGER,
                                                      ROLE_GENE_MANAGER, ROLE_BED_MANAGER, ROLE_CYTOBANDS_MANAGER,
                                                      ROLE_MAF_MANAGER, ROLE_SEG_MANAGER);
        managerRoles.forEach(role -> roleHierarchy.setHierarchy(ROLE_ADMIN.getName() + " > " + role.getName()));
        roleHierarchy.setHierarchy(managerRoles.map(DefaultRoles::getName).collect(Collectors.joining(" == ")));
        managerRoles.forEach(role -> roleHierarchy.setHierarchy(role.getName() + " > " + ROLE_USER.getName()));

        return roleHierarchy;
    }

    @Bean
    public SidRetrievalStrategy sidRetrievalStrategy() {
        return new SidRetrievalStrategyImpl(roleHierarchy());
    }


    @Bean
    public PermissionEvaluator permissionEvaluator() {
        AclPermissionEvaluator evaluator = new AclPermissionEvaluator(jdbcMutableAclService);
        evaluator.setPermissionFactory(permissionFactory());
        return evaluator;
    }

    @Bean
    public LookupStrategy lookupStrategy() {
        return new LookupStrategyImpl(dataSource, aclCache(), aclAuthorizationStrategy(),
                                      auditLogger(), permissionFactory, permissionGrantingStrategy());
    }
}
