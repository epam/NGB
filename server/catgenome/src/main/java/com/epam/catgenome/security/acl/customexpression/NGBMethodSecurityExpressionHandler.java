package com.epam.catgenome.security.acl.customexpression;

import java.util.function.Supplier;

import com.epam.catgenome.security.acl.PermissionHelper;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.util.function.SingletonSupplier;

public class NGBMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private AuthenticationTrustResolver trustResolver =
            new AuthenticationTrustResolverImpl();

    /**
     * Both collaborators arrive as suppliers rather than as values. Spring Security 6 asks for this
     * handler as a bean, and the method-security interceptors that consume it are built during
     * infrastructure setup - so whatever the handler holds at construction time is built then too,
     * and the {@link PermissionEvaluator} in particular pulls the whole ACL service and its
     * DataSource up with it. Resolving them on first use instead moves that to the first secured
     * method call, by which time the context is running. Memoised, so the lookup happens once.
     */
    private final Supplier<PermissionEvaluator> permissionEvaluator;

    private final Supplier<PermissionHelper> permissionHelper;

    public NGBMethodSecurityExpressionHandler(final Supplier<PermissionEvaluator> permissionEvaluator,
                                              final Supplier<PermissionHelper> permissionHelper) {
        this.permissionEvaluator = SingletonSupplier.of(permissionEvaluator);
        this.permissionHelper = SingletonSupplier.of(permissionHelper);
    }

    /**
     * The reason this override exists at all. Spring Security 6 added a second, {@link Supplier}-based
     * {@code createEvaluationContext} to {@link DefaultMethodSecurityExpressionHandler} and made it
     * what the method-security interceptors call - and it builds its root object through a *private*
     * {@code createSecurityExpressionRoot(Supplier, MethodInvocation)}, so the protected override
     * below is never reached. Left alone, every NGB expression fails at runtime with
     * {@code EL1004E: Method call: Method isAllowed(...) cannot be found on type
     * MethodSecurityExpressionRoot} - the 16 ACL test failures that found this.
     *
     * <p>Delegating to the {@code Authentication}-based overload (still public, still non-deprecated
     * in 6.5, and the only path that goes through {@code createSecurityExpressionRoot}) restores
     * exactly the Security 5 behaviour. The one thing given up is the laziness the supplier was
     * introduced for: the {@code Authentication} is resolved for every secured invocation rather than
     * only for expressions that read it. Every expression in this application reads it.
     */
    @Override
    public EvaluationContext createEvaluationContext(final Supplier<Authentication> authentication,
                                                     final MethodInvocation invocation) {
        return createEvaluationContext(authentication.get(), invocation);
    }

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication, MethodInvocation invocation) {
        NGBMethodSecurityExpressionRoot root =
                new NGBMethodSecurityExpressionRoot(authentication);
        root.setPermissionEvaluator(permissionEvaluator.get());
        root.setTrustResolver(this.trustResolver);
        root.setRoleHierarchy(getRoleHierarchy());
        root.setPermissionHelper(permissionHelper.get());
        return root;
    }

}
