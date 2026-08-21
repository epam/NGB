#!/usr/bin/env python3
"""Logs in over SAML and prints the JWT that /restapi/user/token issues for that session.

In SAML mode the CLI authenticates with a JWT, and the only way to obtain one is to already be
logged in - the endpoint signs a token for whoever calls it. So this reuses the browser-less SSO
login and then asks for a token:

    python3 jwt-token.py [username] [password]     # prints the token and nothing else

Which is what `make cli-token` does. Feed it to the CLI with `ngb set_token <token>`, or put it in
CLI_TOKEN in .env.
"""
import contextlib
import importlib.util
import io
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
USER = (sys.argv + [None, None])[1] or "ngbadmin@ngb.dev.local"
PASSWORD = (sys.argv + [None, None])[2] or "admin"

# saml-login-test.py is a script rather than a module, and it logs in as a side effect of being
# executed - which is exactly what is wanted here. SAML_KEEP_SESSION stops it before its logout step,
# where it exits: hence the SystemExit, and hence redirecting its progress output to the bin so that
# this script's stdout is only ever the token.
spec = importlib.util.spec_from_file_location(
    "saml_login_test", os.path.join(HERE, "saml-login-test.py"))
login = importlib.util.module_from_spec(spec)
sys.argv = [sys.argv[0], USER, PASSWORD]
os.environ["SAML_KEEP_SESSION"] = "1"
progress = io.StringIO()
try:
    with contextlib.redirect_stdout(progress):
        spec.loader.exec_module(login)
except SystemExit as exit_code:
    if exit_code.code:
        sys.stderr.write(progress.getvalue())
        raise

url, page = login.get(f"{login.BASE}/restapi/user/token")
try:
    token = (json.loads(page).get("payload") or {}).get("token")
except json.JSONDecodeError:
    sys.stderr.write(f"FAILED: /restapi/user/token did not return JSON:\n{page[:400]}\n")
    sys.exit(1)
if not token:
    sys.stderr.write(f"FAILED: no token in the response:\n{page[:400]}\n")
    sys.exit(1)

print(token)
