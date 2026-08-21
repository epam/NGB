# Appended to catgenome.properties when AUTH_MODE=saml.
#
# Note on the mode combination: SAML guards the browser and JWT guards /restapi/**, and both are
# switched on here. They are independent as of migration Phase 4 - the JWT chain no longer needs
# any SAML bean, only the URL to redirect /restapi/navigate to - so jwt.security.enable=true with
# saml.security.enable=false now starts, and returns 401 instead of redirecting. Enabling both is
# still what the docs recommend, so that ngb-cli keeps working while the UI uses SSO.
#
# SamlUserDetailsService is @ConditionalOnProperty(security.acl.enable=true) and
# SAMLSecurityConfiguration requires it, so ACL has to be on whenever SAML is.

saml.security.enable=true
security.acl.enable=true
security.default.admin=${DEFAULT_ADMIN}

# HTTPS - required by SAML. The port is 8443, set by HTTPS_PORT in docker-compose.yml; the app
# exposes a single connector, so 8080 does not answer in this mode.
server.ssl.ciphers=HIGH:!RC4:!aNULL:!MD5:!kEDH
server.ssl.key-store=file:/secrets/ngb-keystore.jks
server.ssl.key-store-type=JKS
server.ssl.key-store-password=${KEYSTORE_PASS}
server.ssl.keyAlias=${HTTPS_KEY_ALIAS}

# SAML service provider identity.
#
# saml.sign.key signs outgoing <AuthnRequest>s and, since Phase 4, the published SP metadata as
# well - Spring Security keeps one credential list for both, where the OpenSAML 2 extension signed
# messages with this key but signed and advertised metadata with server.ssl.keyAlias. That key is
# now used only for decryption, and the metadata at /catgenome/saml/metadata advertises ngb-saml,
# which is the key an IdP actually needs to validate what NGB sends it.
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
