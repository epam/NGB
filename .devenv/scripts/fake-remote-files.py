#!/usr/bin/env python3
"""A range-serving static file server that answers HEAD with 403 when addressed as S3.

It exists to exercise the one piece of NGB's htsjdk fork that no unit test covers:
EnhancedUrlHelper. Its S3Helper is there because a pre-signed S3 GET URL answers HEAD with 403,
and htsjdk's stock HTTPHelper.exists() treats anything but 200 as "not there" - so without it,
every pre-signed-URL track is refused before a byte is read. There are no S3 credentials in the
dev environment, so the situation is staged instead:

  * the container running this gets two network aliases on the ngb-dev network, `p7-files` and
    `s3.amazonaws.com`; only the second matches EnhancedUrlHelper's S3_PATTERN;
  * HEAD is answered 403 for the S3 alias and 200 for the plain one, decided from the Host
    header - so the same file, served by the same process, goes down both code paths;
  * GET honours Range, which is what htsjdk needs for a Tabix seek and what python's
    http.server does not do.

Every request is logged to stdout, so `docker logs` is the evidence of which path was taken.

  fake-remote-files.py [port] [directory]
"""

import os
import re
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

S3_HOST = re.compile(r".*s3.*\.amazonaws\.com", re.IGNORECASE)
ROOT = os.path.abspath(sys.argv[2] if len(sys.argv) > 2 else ".")
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8000


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "p7-fake-remote/1.0"

    def resolve(self):
        path = self.path.split("?", 1)[0].split("#", 1)[0]
        target = os.path.abspath(os.path.join(ROOT, path.lstrip("/")))
        if not target.startswith(ROOT + os.sep) or not os.path.isfile(target):
            return None
        return target

    def looks_like_s3(self):
        return bool(S3_HOST.match((self.headers.get("Host") or "").split(":")[0]))

    def refuse(self, code, note):
        self.log_message("%s %s -> %d (%s)", self.command, self.path, code, note)
        self.send_response(code)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_HEAD(self):
        target = self.resolve()
        if target is None:
            self.refuse(404, "no such file")
        elif self.looks_like_s3():
            # What a pre-signed S3 URL does, and the whole reason S3Helper exists.
            self.refuse(403, "S3 alias: HEAD is not signed for")
        else:
            self.log_message("HEAD %s -> 200 (%d bytes)", self.path, os.path.getsize(target))
            self.send_response(200)
            self.send_header("Content-Length", str(os.path.getsize(target)))
            self.send_header("Accept-Ranges", "bytes")
            self.end_headers()

    def do_GET(self):
        target = self.resolve()
        if target is None:
            self.refuse(404, "no such file")
            return

        size = os.path.getsize(target)
        start, end = 0, size - 1
        partial = False
        requested = self.headers.get("Range")
        if requested and requested.startswith("bytes="):
            first, _, last = requested[len("bytes="):].partition("-")
            start = int(first) if first else 0
            end = int(last) if last else size - 1
            end = min(end, size - 1)
            partial = True
            if start > end:
                self.log_message("GET %s -> 416 (%s of %d)", self.path, requested, size)
                self.send_response(416)
                self.send_header("Content-Range", "bytes */%d" % size)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return

        length = end - start + 1
        self.log_message("GET %s%s -> %d (%d bytes%s)", self.path,
                         " " + requested if requested else "",
                         206 if partial else 200, length,
                         ", host %s" % self.headers.get("Host") if partial else "")
        self.send_response(206 if partial else 200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Length", str(length))
        self.send_header("Accept-Ranges", "bytes")
        if partial:
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, size))
        self.end_headers()
        try:
            with open(target, "rb") as handle:
                handle.seek(start)
                remaining = length
                while remaining > 0:
                    chunk = handle.read(min(65536, remaining))
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    remaining -= len(chunk)
        except (BrokenPipeError, ConnectionResetError):
            # Expected, and worth seeing rather than a traceback: a length probe disconnects as
            # soon as it has the headers, and a reader that has what it wanted drops the rest.
            self.log_message("GET %s -> client closed after %d of %d bytes", self.path,
                             length - remaining, length)
            self.close_connection = True


if __name__ == "__main__":
    print("serving %s on :%d (HEAD -> 403 for any s3*.amazonaws.com host)" % (ROOT, PORT),
          flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
