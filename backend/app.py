from http.server import HTTPServer, BaseHTTPRequestHandler
import json
from datetime import datetime
import time
import sqlite3

start_time = time.time()
DB_PATH = '/home/lasmodis/42_project/trans/database/app.db'

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
    def do_GET(self):
        # Enable CORS headers
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        if self.path == '/healthcheck':
            response = {
                "status": "ok",
                "timestamp": datetime.utcnow().isoformat() + "Z",
                "uptime": time.time() - start_time
            }
            self.wfile.write(json.dumps(response).encode())
        elif self.path == '/users':
            response = {"users": get_users()}
            self.wfile.write(json.dumps(response).encode())
        elif self.path == '/':
            response = {"message": "FacturX Backend API"}
            self.wfile.write(json.dumps(response).encode())
        else:
            self.send_response(404)
            response = {"error": "Not found"}
            self.wfile.write(json.dumps(response).encode())

    def do_OPTIONS(self):
        # Handle CORS preflight
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def log_message(self, format, *args):
        # Simple logging
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {format % args}")

if __name__ == '__main__':
    PORT = 5000
    server = HTTPServer(('localhost', PORT), HealthCheckHandler)
    print(f"✓ Backend running on http://localhost:{PORT}")
    print(f"  GET http://localhost:{PORT}/healthcheck")
    print(f"  GET http://localhost:{PORT}/")
    server.serve_forever()
