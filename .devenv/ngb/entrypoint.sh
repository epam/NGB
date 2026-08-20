#!/usr/bin/env bash
# Starts an NGB server inside the dev environment.
#
#   JAVA_VERSION=8|17|21  which JDK to run the jar on (the whole point of this env)
#   AUTH_MODE=none|saml   no security, or Keycloak SAML SSO + JWT for the CLI
#
# Config is rendered into /opt/ngb/config/catgenome.properties, which the app picks up
# via both --conf and CATGENOME_CONF_DIR.
set -euo pipefail

AUTH_MODE="${AUTH_MODE:-none}"
JAVA_VERSION="${JAVA_VERSION:-8}"
NGB_HEAP="${NGB_HEAP:-2g}"
NGB_JAR="${NGB_JAR:-/dist/catgenome-h2.jar}"
NGB_HOSTNAME="${NGB_HOSTNAME:-localhost}"
HTTP_PORT="${HTTP_PORT:-8080}"
HTTPS_PORT="${HTTPS_PORT:-9443}"
CONF_DIR="/opt/ngb/config"
BIN_DIR="/opt/ngb/bin"

log() { echo "[ngb-entrypoint] $*"; }
die() { echo "[ngb-entrypoint] ERROR: $*" >&2; exit 1; }

# --- JDK selection ----------------------------------------------------------
case "$JAVA_VERSION" in
  8)  export JAVA_HOME="${JAVA_HOME_8}" ;;
  17) export JAVA_HOME="${JAVA_HOME_17}" ;;
  21) export JAVA_HOME="${JAVA_HOME_21}" ;;
  *)  die "JAVA_VERSION must be 8, 17 or 21 (got '$JAVA_VERSION')" ;;
esac
export PATH="$JAVA_HOME/bin:$PATH"
log "using JDK $JAVA_VERSION -> $JAVA_HOME"
java -version 2>&1 | sed 's/^/[ngb-entrypoint]   /'

# --- the jar ----------------------------------------------------------------
if [[ ! -f "$NGB_JAR" ]]; then
  die "$NGB_JAR not found. Build it first:
    cd .devenv && make jar        # H2 flavour   -> dist/catgenome-h2.jar
    cd .devenv && make jar-pg     # PostgreSQL   -> dist/catgenome-psql.jar"
fi

# --- wait for the database, if external -------------------------------------
if [[ -n "${DB_WAIT_HOST:-}" ]]; then
  log "waiting for database at ${DB_WAIT_HOST}:5432 ..."
  for _ in $(seq 1 60); do
    pg_isready -h "$DB_WAIT_HOST" -p 5432 -U "${DB_USER:-catgenome}" >/dev/null 2>&1 && break
    sleep 2
  done
  pg_isready -h "$DB_WAIT_HOST" -p 5432 -U "${DB_USER:-catgenome}" >/dev/null 2>&1 \
    || die "database at ${DB_WAIT_HOST}:5432 never became ready"
fi

mkdir -p /opt/ngb/contents /opt/ngb/download /opt/ngb/H2 "$CONF_DIR"

# --- render configuration ---------------------------------------------------
if [[ "$AUTH_MODE" == "saml" ]]; then
  export SERVER_PORT="$HTTPS_PORT"
  export NGB_BASE_URL="https://${NGB_HOSTNAME}:${HTTPS_PORT}/catgenome"
else
  export SERVER_PORT="$HTTP_PORT"
  export NGB_BASE_URL="http://${NGB_HOSTNAME}:${HTTP_PORT}/catgenome"
fi
export DB_DRIVER DB_URL DB_USER
export DB_PASSWORD="${DB_PASSWORD:-}"

envsubst '$SERVER_PORT $DB_DRIVER $DB_URL $DB_USER $DB_PASSWORD $NGB_BASE_URL' \
  < "$BIN_DIR/catgenome.properties.tpl" > "$CONF_DIR/catgenome.properties"

if [[ "$AUTH_MODE" == "saml" ]]; then
  [[ -f /secrets/ngb-keystore.jks ]] \
    || die "/secrets/ngb-keystore.jks missing - run 'make certs'"
  [[ -f /secrets/idp-metadata.xml ]] \
    || die "/secrets/idp-metadata.xml missing - start the IdP first: 'make up-saml'"
  [[ -f /secrets/jwt-public.b64 && -f /secrets/jwt-private.b64 ]] \
    || die "JWT keys missing in /secrets - run 'make certs'"

  export ENDPOINT_ID="$NGB_BASE_URL"
  export DEFAULT_ADMIN="${DEFAULT_ADMIN:-ngbadmin@ngb.dev.local}"
  export KEYSTORE_PASS="${KEYSTORE_PASS:-changeit}"
  export HTTPS_KEY_ALIAS="${HTTPS_KEY_ALIAS:-ngb-https}"
  export SAML_SIGN_KEY="${SAML_SIGN_KEY:-ngb-saml}"
  export JWT_PUBLIC="$(cat /secrets/jwt-public.b64)"
  export JWT_PRIVATE="$(cat /secrets/jwt-private.b64)"

  envsubst '$ENDPOINT_ID $DEFAULT_ADMIN $KEYSTORE_PASS $HTTPS_KEY_ALIAS $SAML_SIGN_KEY $JWT_PUBLIC $JWT_PRIVATE' \
    < "$BIN_DIR/auth-saml.properties.tpl" >> "$CONF_DIR/catgenome.properties"
  log "auth mode: SAML SSO via Keycloak (+ JWT for ngb-cli)"
  log "entity id / base url: $ENDPOINT_ID"
else
  {
    echo ""
    echo "# AUTH_MODE=none"
    echo "saml.security.enable=false"
    echo "jwt.security.enable=false"
    echo "security.acl.enable=false"
  } >> "$CONF_DIR/catgenome.properties"
  log "auth mode: none (unauthenticated)"
fi

# user-supplied overrides win - drop a file at .devenv/ngb/override.properties
if [[ -f "$BIN_DIR/override.properties" ]]; then
  log "appending override.properties"
  { echo ""; echo "# --- override.properties ---"; cat "$BIN_DIR/override.properties"; } \
    >> "$CONF_DIR/catgenome.properties"
fi

log "URL: ${NGB_BASE_URL}"

exec java \
  -Xmx"${NGB_HEAP}" \
  -Djava.security.egd=file:/dev/./urandom \
  ${JAVA_EXTRA_OPTS:-} \
  -jar "$NGB_JAR" \
  --conf="$CONF_DIR"
