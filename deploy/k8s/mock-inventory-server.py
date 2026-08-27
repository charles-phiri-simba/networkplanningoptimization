"""HTTPS inventory mock for Phase 10 lab. Serves classpath-equivalent fixture JSON."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
import ssl
import sys
import base64

USER = os.environ.get("MOCK_BASIC_USER", "")
PASSWORD = os.environ.get("MOCK_BASIC_PASSWORD", "")
INVENTORY_PATH = os.environ.get("MOCK_INVENTORY_PATH", "/inventory.json")
CERT = os.environ.get("MOCK_TLS_CERT", "/tls/tls.crt")
KEY = os.environ.get("MOCK_TLS_KEY", "/tls/tls.key")


class Handler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        sys.stderr.write("%s %s\n" % (self.command, self.path))

    def do_GET(self):
        if self.path != "/inventory":
            self.send_response(404)
            self.end_headers()
            return
        expected = "Basic " + base64.b64encode(f"{USER}:{PASSWORD}".encode("utf-8")).decode("ascii")
        auth = self.headers.get("Authorization")
        if auth != expected:
            self.send_response(401)
            self.end_headers()
            return
        with open(INVENTORY_PATH, "rb") as handle:
            body = handle.read()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        self.send_response(403)
        self.end_headers()


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8443), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(CERT, KEY)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    server.serve_forever()
