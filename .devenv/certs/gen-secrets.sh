#!/usr/bin/env bash
# Generates the local dev secrets NGB needs for SAML + JWT:
#
#   /secrets/ngb-keystore.jks   two RSA key entries: HTTPS cert and SAML signing cert
#   /secrets/ngb-saml-cert.pem  the SAML signing cert; `make saml-verify-signing` uploads it
#                               to Keycloak and turns "client signature required" on
#   /secrets/jwt-private.b64    PKCS#8 DER, base64, single line  -> jwt.key.private
#   /secrets/jwt-public.b64     X.509 SPKI DER, base64, one line -> jwt.key.public
#
# The base64 shapes are dictated by the code: JwtTokenGenerator uses PKCS8EncodedKeySpec
# and JwtTokenVerifier uses X509EncodedKeySpec, both over Base64.getDecoder().
#
# Idempotent: existing files are kept. Set FORCE=1 to regenerate everything.
set -euo pipefail

SECRETS_DIR=/secrets
KEYSTORE="$SECRETS_DIR/ngb-keystore.jks"
KEYSTORE_PASS="${KEYSTORE_PASS:-changeit}"
HTTPS_KEY_ALIAS="${HTTPS_KEY_ALIAS:-ngb-https}"
SAML_SIGN_KEY="${SAML_SIGN_KEY:-ngb-saml}"
SAN_HOSTS="${SAN_HOSTS:-ngb.dev.local,ngb-pg.dev.local,localhost}"
FORCE="${FORCE:-0}"

# JKS (not PKCS12) because server.ssl.key-store-type=JKS in ngb/auth-saml.properties.tpl, which
# SAMLSecurityConfiguration also reads to load the SAML signing and decryption keys. Either format
# would work now that JKSKeyManager is gone; changing it means changing both places.
#
# The image's default JDK, whichever it is, rather than a hardcoded path. Its keytool writes JKS
# perfectly well but prints one warning per invocation recommending PKCS12 - that warning is
# expected, and is the only thing on stderr here.
KEYTOOL="${JAVA_HOME:-/opt/java/openjdk}/bin/keytool"

log() { echo "[certs] $*"; }

mkdir -p "$SECRETS_DIR"

if [[ "$FORCE" == "1" ]]; then
  log "FORCE=1 - removing existing secrets"
  rm -f "$KEYSTORE" "$SECRETS_DIR"/ngb-saml-cert.pem \
        "$SECRETS_DIR"/jwt-private.pem "$SECRETS_DIR"/jwt-public.pem \
        "$SECRETS_DIR"/jwt-private.b64 "$SECRETS_DIR"/jwt-public.b64
fi

# --- keystore ---------------------------------------------------------------
san="$(echo "$SAN_HOSTS" | tr ',' '\n' | sed 's/^/dns:/' | paste -sd, -),ip:127.0.0.1"
primary_host="${SAN_HOSTS%%,*}"

gen_key() {
  local alias="$1" cn="$2"
  log "generating key '$alias' (CN=$cn)"
  "$KEYTOOL" -genkeypair \
    -alias "$alias" \
    -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 3650 \
    -dname "CN=$cn, OU=NGB Dev Environment, O=Local, L=Local, C=US" \
    -ext "SAN=$san" \
    -keystore "$KEYSTORE" -storetype JKS \
    -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
    >/dev/null
}

if [[ -f "$KEYSTORE" ]]; then
  log "keystore already exists: $KEYSTORE"
else
  gen_key "$HTTPS_KEY_ALIAS" "$primary_host"
  gen_key "$SAML_SIGN_KEY" "$primary_host SAML signing"
  log "keystore created with aliases: $HTTPS_KEY_ALIAS (https), $SAML_SIGN_KEY (saml)"
fi

if [[ ! -f "$SECRETS_DIR/ngb-saml-cert.pem" ]]; then
  "$KEYTOOL" -exportcert -rfc \
    -alias "$SAML_SIGN_KEY" -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" \
    -file "$SECRETS_DIR/ngb-saml-cert.pem" >/dev/null
  log "exported SAML signing certificate to ngb-saml-cert.pem"
fi

# --- JWT keypair ------------------------------------------------------------
if [[ -f "$SECRETS_DIR/jwt-private.b64" && -f "$SECRETS_DIR/jwt-public.b64" ]]; then
  log "JWT keys already exist"
else
  log "generating JWT RSA keypair"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
    -out "$SECRETS_DIR/jwt-private.pem" 2>/dev/null
  openssl pkey -in "$SECRETS_DIR/jwt-private.pem" -pubout \
    -out "$SECRETS_DIR/jwt-public.pem" 2>/dev/null

  grep -v -- "-----" "$SECRETS_DIR/jwt-private.pem" | tr -d '\n' \
    > "$SECRETS_DIR/jwt-private.b64"
  grep -v -- "-----" "$SECRETS_DIR/jwt-public.pem" | tr -d '\n' \
    > "$SECRETS_DIR/jwt-public.b64"
  log "JWT keys written (jwt-private.b64 / jwt-public.b64)"
fi

chmod 0644 "$SECRETS_DIR"/* 2>/dev/null || true
log "done"
