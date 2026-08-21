#!/usr/bin/env python3
"""Turn Keycloak's `saml.client.signature` on (or off) for the NGB dev client.

With the flag off - which is how `keycloak/realm-ngb.json` ships, because the SP certificate is
generated per machine by `make certs` and so cannot be inlined in the realm - Keycloak accepts an
unsigned `<AuthnRequest>` and never looks at NGB's signature. That means a broken signing
configuration is indistinguishable from a working one. This script uploads
`secrets/ngb-saml-cert.pem` to the client and flips the flag, so the next `make smoke-saml` only
passes if NGB really did sign the request with the key its metadata advertises.

    python3 saml-client-signature.py on|off

Run it from inside the `ngb` docker network (the Makefile does: `make saml-verify-signing`).
The change lasts until the IdP restarts - `start-dev --import-realm` rebuilds the realm from
the JSON every time.
"""
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

IDP = os.environ.get("IDP_URL", "http://idp.dev.local:8081")
REALM = os.environ.get("KC_REALM", "ngb")
ADMIN_USER = os.environ.get("KC_ADMIN_USER", "admin")
ADMIN_PASSWORD = os.environ.get("KC_ADMIN_PASSWORD", "admin")
CLIENT_ID = os.environ.get("NGB_BASE_URL", "https://ngb.dev.local:8443/catgenome")
CERT_FILE = os.environ.get("SAML_CERT_FILE", "/workspace/.devenv/secrets/ngb-saml-cert.pem")


def fail(msg):
    print(f"FAILED: {msg}", file=sys.stderr)
    sys.exit(1)


def call(method, url, body=None, token=None, form=False):
    headers = {}
    data = None
    if body is not None:
        if form:
            data = urllib.parse.urlencode(body).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        else:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            payload = response.read()
            return json.loads(payload) if payload else None
    except urllib.error.HTTPError as e:
        fail(f"{method} {url} -> {e.code} {e.read().decode('utf-8', 'replace')[:400]}")
    except urllib.error.URLError as e:
        fail(f"{method} {url} -> {e.reason}. Is the IdP up? Try 'make up-saml'.")


def der_base64(path):
    """Keycloak wants the certificate as bare base64 DER, i.e. the PEM body on one line."""
    try:
        with open(path) as f:
            lines = f.read().splitlines()
    except OSError as e:
        fail(f"{e}. Run 'make certs' first.")
    body = [line.strip() for line in lines if line.strip() and "-----" not in line]
    if not body:
        fail(f"{path} contains no certificate body")
    return "".join(body)


mode = (sys.argv + [None])[1]
if mode not in ("on", "off"):
    fail("usage: saml-client-signature.py on|off")
enable = mode == "on"

token = call(
    "POST",
    f"{IDP}/realms/master/protocol/openid-connect/token",
    {
        "client_id": "admin-cli",
        "username": ADMIN_USER,
        "password": ADMIN_PASSWORD,
        "grant_type": "password",
    },
    form=True,
)["access_token"]

query = urllib.parse.urlencode({"clientId": CLIENT_ID})
found = call("GET", f"{IDP}/admin/realms/{REALM}/clients?{query}", token=token)
if not found:
    fail(f"no SAML client '{CLIENT_ID}' in realm '{REALM}'")
client = found[0]

attributes = dict(client.get("attributes") or {})
attributes["saml.client.signature"] = "true" if enable else "false"
if enable:
    attributes["saml.signing.certificate"] = der_base64(CERT_FILE)
else:
    attributes.pop("saml.signing.certificate", None)

call(
    "PUT",
    f"{IDP}/admin/realms/{REALM}/clients/{client['id']}",
    {**client, "attributes": attributes},
    token=token,
)

print(f"saml.client.signature = {attributes['saml.client.signature']}  ({CLIENT_ID})")
if enable:
    print(f"signing certificate    = {CERT_FILE}")
    print("Keycloak will now reject an <AuthnRequest> NGB has not signed with that key.")
