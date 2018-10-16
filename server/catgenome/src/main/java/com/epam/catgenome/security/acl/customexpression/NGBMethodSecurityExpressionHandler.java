package com.epam.catgenome.security.acl.customexpression;

import com.epam.catgenome.security.acl.JdbcMutableAclServiceImpl;
import com.epam.catgenome.security.acl.PermissionHelper;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

public class NGBMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private AuthenticationTrustResolver trustResolver =
            new AuthenticationTrustResolverImpl();

    private PermissionHelper permissionHelper;
    private JdbcMutableAclServiceImpl aclService;

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        NGBMethodSecurityExpressionRoot root =
                new NGBMethodSecurityExpressionRoot(authentication);
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(this.trustResolver);
        root.setRoleHierarchy(getRoleHierarchy());
        root.setAclService(aclService);
        root.setPermissionHelper(permissionHelper);
        return root;
    }

    public void setPermissionHelper(PermissionHelper permissionHelper) {
        this.permissionHelper = permissionHelper;
    }

    public void setAclService(JdbcMutableAclServiceImpl aclService) {
        this.aclService = aclService;
    }
}
