#!/usr/bin/env bash
# Unpacks ngb-cli from dist/ and points it at the running server.
#
#   docker-compose exec cli ngb reg_ref /ngs/grch38/Homo_sapiens.GRCh38.fa --name GRCh38
#
# In SAML mode the CLI needs a JWT: get one from the UI (user menu -> "Generate access
# token"), then either set CLI_TOKEN in .env or run `ngb set_token <token>` in here.
set -euo pipefail

CLI_HOME=/opt/ngb-cli
TARBALL=/dist/ngb-cli.tar.gz

log() { echo "[cli] $*"; }

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
# so `docker-compose exec cli ngb ...` works too, not just this shell
ln -sf "$CLI_HOME/bin/ngb" /usr/local/bin/ngb

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
