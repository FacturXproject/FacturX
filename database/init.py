import sqlite3
import os

db_path = '/home/lasmodis/42_project/trans/database/app.db'

# Remove old database if exists
if os.path.exists(db_path):
    os.remove(db_path)

# Create connection
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Create users table
cursor.execute('''
    CREATE TABLE users (
        id INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        email TEXT NOT NULL,
        role TEXT NOT NULL
    )
''')

# Insert sample data
cursor.execute("INSERT INTO users VALUES (1, 'Alice Dupont', 'alice@example.com', 'accountant')")
cursor.execute("INSERT INTO users VALUES (2, 'Bob Martin', 'bob@example.com', 'client')")
cursor.execute("INSERT INTO users VALUES (3, 'Charlie Leblanc', 'charlie@example.com', 'admin')")

conn.commit()
conn.close()

print("✓ Database created at:", db_path)
