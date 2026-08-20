#!/usr/bin/env python3
"""End-to-end SAML SSO check: browser-less login through Keycloak into NGB.

Walks the whole web SSO profile the way a browser would - SP-initiated redirect to
the IdP, form login, auto-submitted SAMLResponse back to /saml/SSO - and then calls
an authenticated REST endpoint to prove the session and the authorities landed.

    python3 saml-login-test.py ngbadmin@ngb.dev.local admin

Run it from inside the `ngb` docker network (the Makefile does: `make smoke-saml`),
because SAML matches URLs byte-for-byte and NGB's entity id is the dev hostname.
Stdlib only, and TLS verification is off on purpose: the cert is self-signed.
"""
import html
import http.cookiejar
import json
import os
import re
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("NGB_BASE_URL", "https://ngb.dev.local:9443/catgenome")

ctx = ssl._create_unverified_context()
jar = http.cookiejar.CookieJar()


class LoggingRedirects(urllib.request.HTTPRedirectHandler):
    """The redirect chain is the interesting part when SAML goes wrong."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        print(f"      {code} -> {newurl.split('?')[0]}")
        return super().redirect_request(req, fp, code, msg, headers, newurl)


opener = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(jar),
    urllib.request.HTTPSHandler(context=ctx),
    LoggingRedirects(),
)
opener.addheaders = [("User-Agent", "ngb-saml-smoke/1.0")]


def step(n, msg):
    print(f"[{n}] {msg}")


def fail(msg):
    print(f"FAILED: {msg}", file=sys.stderr)
    sys.exit(1)


def get(url, data=None):
    body = urllib.parse.urlencode(data).encode() if data else None
    try:
        with opener.open(url, body, timeout=60) as r:
            return r.geturl(), r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.geturl(), e.read().decode("utf-8", "replace")


def form_action(page, url):
    m = re.search(r'<form[^>]+action="([^"]+)"', page, re.I)
    if not m:
        return None
    return urllib.parse.urljoin(url, html.unescape(m.group(1)))


def hidden(page, name):
    m = re.search(
        rf'<input[^>]+name="{name}"[^>]+value="([^"]*)"', page, re.I
    ) or re.search(rf'<input[^>]+value="([^"]*)"[^>]+name="{name}"', page, re.I)
    return html.unescape(m.group(1)) if m else None


user, password = (sys.argv + [None, None])[1:3]
if not user or not password:
    fail("usage: saml-login-test.py <username> <password>")

# 1. Unauthenticated request -> SP-initiated AuthnRequest -> IdP login page.
step(1, f"GET {BASE}/ (expect redirect to the IdP)")
url, page = get(f"{BASE}/")
if "/realms/ngb/" not in url:
    fail(f"not redirected to Keycloak, landed on {url}\n{page[:400]}")
step(1, f"    -> IdP login page at {url.split('?')[0]}")

# 2. Post the credentials to the login form.
action = form_action(page, url)
if not action:
    fail(f"no login form on the IdP page:\n{page[:800]}")
step(2, f"POST credentials for {user}")
url, page = get(action, {"username": user, "password": password, "credentialId": ""})
if "SAMLResponse" not in page:
    if "Invalid username or password" in page:
        fail("Keycloak rejected the credentials")
    fail(f"no SAMLResponse after login, landed on {url}\n{page[:800]}")

# 3. Auto-submit the assertion to NGB's assertion consumer service.
acs = form_action(page, url)
saml_response = hidden(page, "SAMLResponse")
relay_state = hidden(page, "RelayState")
if not acs or not saml_response:
    fail("could not extract the SAMLResponse form")
step(3, f"POST SAMLResponse to {acs}")
payload = {"SAMLResponse": saml_response}
if relay_state is not None:
    payload["RelayState"] = relay_state
url, page = get(acs, payload)
if "/realms/ngb/" in url:
    fail(f"NGB bounced back to the IdP - assertion rejected. Landed on {url}")
step(3, f"    -> {url}")

cookies = sorted(c.name for c in jar)
if not any(c.upper().startswith("JSESSIONID") for c in cookies):
    fail(f"no servlet session cookie was set (cookies: {cookies})")
step(3, f"    session cookies: {cookies}")

# 4. Prove the session works and check what authorities NGB derived.
step(4, "GET /restapi/user/current")
url, page = get(f"{BASE}/restapi/user/current")
try:
    payload = json.loads(page).get("payload") or {}
except json.JSONDecodeError:
    fail(f"/restapi/user/current did not return JSON:\n{page[:400]}")

# NGB upper-cases the SAML name id, and reports both its own roles and the raw
# Keycloak group names as authorities.
username = payload.get("username")
authorities = [a.get("authority") for a in payload.get("authorities") or []]
print()
print(f"  username    : {username}")
print(f"  authorities : {authorities}")
print()
if not username:
    fail("authenticated, but NGB returned no user - check saml.user.attributes")
if "ROLE_ADMIN" in authorities:
    print("  (admin - security.default.admin or NGB_ADMINS membership took effect)")
print("SAML SSO OK")
