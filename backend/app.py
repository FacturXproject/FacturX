from http.server import HTTPServer, BaseHTTPRequestHandler
import json
from datetime import datetime
import time
import sqlite3
import os

start_time = time.time()
DB_PATH = os.environ.get('DB_PATH', '/app/database/app.db')

def get_users():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('SELECT id, name, email, role FROM users')
    rows = cursor.fetchall()
    conn.close()
    
    users = [
        {"id": row[0], "name": row[1], "email": row[2], "role": row[3]}
        for row in rows
    ]
    return users

class HealthCheckHandler(BaseHTTPRequestHandler):
    def _set_cors_headers(self):
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')

    def do_GET(self):
        try:
            if self.path == '/healthcheck':
                self.send_response(200)
                self._set_cors_headers()
                self.end_headers()
                response = {
                    "status": "ok",
                    "timestamp": datetime.utcnow().isoformat() + "Z",
                    "uptime": time.time() - start_time
                }
                self.wfile.write(json.dumps(response).encode())
                return

            if self.path == '/users':
                try:
                    users = get_users()
                    self.send_response(200)
                    self._set_cors_headers()
                    self.end_headers()
                    response = {"users": users}
                    self.wfile.write(json.dumps(response).encode())
                except Exception as e:
                    self.send_response(500)
                    self._set_cors_headers()
                    self.end_headers()
                    response = {"error": str(e)}
                    self.wfile.write(json.dumps(response).encode())
                return

            if self.path == '/':
                self.send_response(200)
                self._set_cors_headers()
                self.end_headers()
                response = {"message": "FacturX Backend API"}
                self.wfile.write(json.dumps(response).encode())
                return

            # Not found
            self.send_response(404)
            self._set_cors_headers()
            self.end_headers()
            response = {"error": "Not found"}
            self.wfile.write(json.dumps(response).encode())
        except Exception as e:
            # Fallback error handler
            self.send_response(500)
            self._set_cors_headers()
            self.end_headers()
            response = {"error": "Internal server error", "details": str(e)}
            self.wfile.write(json.dumps(response).encode())

    def do_OPTIONS(self):
        # Handle CORS preflight
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.send_header('Access-Control-Allow-Credentials', 'true')
        self.send_header('Access-Control-Max-Age', '86400')
        self.end_headers()

    def log_message(self, format, *args):
        # Simple logging
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {format % args}")

if __name__ == '__main__':
    PORT = 5000
    server = HTTPServer(('0.0.0.0', PORT), HealthCheckHandler)
    print(f"✓ Backend running on http://0.0.0.0:{PORT}")
    print(f"  GET http://localhost:{PORT}/healthcheck")
    print(f"  GET http://localhost:{PORT}/users")
    server.serve_forever()
