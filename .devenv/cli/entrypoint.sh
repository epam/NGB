#!/usr/bin/env bash
# Unpacks ngb-cli from dist/ and points it at the running server.
#
#   docker-compose exec cli ngb reg_ref /ngs/grch38/Homo_sapiens.GRCh38.fa --name GRCh38
#
# In SAML mode the CLI needs a JWT: get one from the UI (user menu -> "Generate access
# token"), then either set CLI_TOKEN in .env or run `ngb set_token <token>` in here.
#
#   JAVA_VERSION=8|17|21  which JDK to run the CLI on. Defaults to 17: from migration Phase 2
#   the CLI is compiled by a JDK 17 toolchain, so the image's default JDK 8 only gets as far as
#   UnsupportedClassVersionError. Kept as a variable, not hardcoded, so a jar built by an older
#   checkout can still be driven from here.
set -euo pipefail

CLI_HOME=/opt/ngb-cli
TARBALL=/dist/ngb-cli.tar.gz
JAVA_VERSION="${JAVA_VERSION:-17}"

log() { echo "[cli] $*"; }

case "$JAVA_VERSION" in
  8)  export JAVA_HOME="${JAVA_HOME_8}" ;;
  17) export JAVA_HOME="${JAVA_HOME_17}" ;;
  21) export JAVA_HOME="${JAVA_HOME_21}" ;;
  *)  echo "[cli] ERROR: JAVA_VERSION must be 8, 17 or 21 (got '$JAVA_VERSION')" >&2; exit 1 ;;
esac
export PATH="$JAVA_HOME/bin:$PATH"
log "using JDK $JAVA_VERSION -> $JAVA_HOME"

if [[ ! -f "$TARBALL" ]]; then
  echo "[cli] ERROR: $TARBALL not found. Build it: cd .devenv && make cli-build" >&2
  exit 1
fi

if [[ ! -x "$CLI_HOME/bin/ngb" ]]; then
  log "unpacking $TARBALL"
  mkdir -p "$CLI_HOME"
  tar -zxf "$TARBALL" -C /opt
  # the tarball unpacks as ngb-cli/
  [[ -x "$CLI_HOME/bin/ngb" ]] || { echo "[cli] ERROR: unexpected tarball layout" >&2; ls -R /opt/ngb-cli >&2; exit 1; }
fi

export PATH="$CLI_HOME/bin:$PATH"
# So `docker-compose exec cli ngb ...` works too, not just this shell. A wrapper rather than a
# symlink because `docker exec` starts from the image environment - it does not see the JAVA_HOME
# this script exported, and the image's default is JDK 8.
printf '#!/bin/sh\nexport JAVA_HOME="%s"\nexport PATH="$JAVA_HOME/bin:$PATH"\nexec %s "$@"\n' \
  "$JAVA_HOME" "$CLI_HOME/bin/ngb" > /usr/local/bin/ngb
chmod +x /usr/local/bin/ngb

if [[ -n "${NGB_SERVER_URL:-}" ]]; then
  log "set_srv $NGB_SERVER_URL"
  ngb set_srv "$NGB_SERVER_URL" || log "WARNING: set_srv failed (is the server up?)"
fi

if [[ -n "${NGB_TOKEN:-}" ]]; then
  log "set_token <provided>"
  ngb set_token "$NGB_TOKEN" || log "WARNING: set_token failed"
fi

log "ngb-cli ready ($CLI_HOME/bin/ngb)"
exec "$@"
