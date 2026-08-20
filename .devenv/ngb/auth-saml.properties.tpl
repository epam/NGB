# Appended to catgenome.properties when AUTH_MODE=saml.
#
# Note on the mode combination: JWTSecurityConfiguration @Autowires SAMLAuthenticationProvider
# and SAMLEntryPoint, which only exist when saml.security.enable=true. JWT-only is therefore
# not a startable configuration in this codebase - SAML mode enables both, which is also what
# the docs recommend so that ngb-cli keeps working.
#
# SAMLUserDetailsServiceImpl is @ConditionalOnProperty(security.acl.enable=true), so ACL must
# be on as well, otherwise SAMLSecurityConfiguration fails to find a SAMLUserDetailsService.

saml.security.enable=true
security.acl.enable=true
security.default.admin=${DEFAULT_ADMIN}

# HTTPS - required by SAML.
#
# The HTTPS port is 9443, NOT 8443, and that is deliberate: after a successful
# assertion, CustomAwareAuthenticationSuccessHandler does
#     StringUtils.replace(savedRequest.getRedirectUrl(), "8443", "8080")
# i.e. it rewrites the literal string "8443" to "8080" in the post-login target URL.
# Spring Boot 1.5 only exposes one connector, so nothing listens on 8080 in SAML mode
# and login would dead-end. Any port without "8443" in it sidesteps the rewrite.
# Removing that hardcoded swap is a migration item, not something this env patches.
server.ssl.ciphers=HIGH:!RC4:!aNULL:!MD5:!kEDH
server.ssl.key-store=file:/secrets/ngb-keystore.jks
server.ssl.key-store-type=JKS
server.ssl.key-store-password=${KEYSTORE_PASS}
server.ssl.keyAlias=${HTTPS_KEY_ALIAS}

# SAML service provider identity
saml.sign.key=${SAML_SIGN_KEY}
server.ssl.endpoint.id=${ENDPOINT_ID}
saml.base.url=${ENDPOINT_ID}
server.ssl.metadata=/secrets/idp-metadata.xml
saml.authn.request.binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect
saml.authn.max.authentication.age=93600

# attribute names must match the Keycloak protocol mappers in keycloak/realm-ngb.json
saml.authorities.attribute.names=http://schemas.xmlsoap.org/ws/2005/05/identity/claims/tokenGroups
saml.user.attributes=Email=http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress,Name=http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name

# AUTO so any Keycloak user can sign in without pre-registration (dev convenience)
saml.user.auto.create=AUTO
session.expiration.behavior=CONFIRM
security.acl.cache.period=86400

# self-signed cert -> allow the UI to be framed and skip HSTS pinning during dev
security.frame-options.disable=true

# JWT, so ngb-cli can still reach the API while SAML guards the browser
jwt.security.enable=true
jwt.key.public=${JWT_PUBLIC}
jwt.key.private=${JWT_PRIVATE}
jwt.token.expiration.seconds=2592000
jwt.required.claims=
