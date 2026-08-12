# FacturX App - Local Setup Guide

This guide explains how to run the FacturX application locally with the frontend, backend, and database all connected.

## Project Structure

```
trans/
├── frontend/          # React + Vite application
│   ├── src/
│   │   ├── pages/     # React pages
│   │   ├── services/  # API services
│   │   └── App.jsx    # Main routing
│   ├── package.json
│   └── vite.config.js
├── backend/           # Python HTTP server
│   └── app.py         # Main backend server
└── database/          # SQLite database
    ├── app.db         # Database file
    └── init.py        # Database initialization script
```

## Prerequisites

Make sure you have installed:
- **Node.js** (v16+) and **npm**
- **Python** (v3.8+)

Check versions:
```bash
node --version
npm --version
python3 --version
```

## Step 1: Initialize the Database

The database contains sample users for testing.

```bash
cd trans/database
python3 init.py
```

You should see:
```
✓ Database created at: /path/to/trans/database/app.db
```

## Step 2: Start the Backend Server

The backend runs on **http://localhost:5000** and provides APIs.

```bash
cd trans/backend
python3 app.py
```

You should see:
```
✓ Backend running on http://localhost:5000
  GET http://localhost:5000/healthcheck
  GET http://localhost:5000/users
```

**Keep this terminal open.**

## Step 3: Install Frontend Dependencies

In a **new terminal**, install npm packages:

```bash
cd trans/frontend
npm install
```

## Step 4: Start the Frontend Dev Server

In the **same terminal**, start Vite:

```bash
npm run dev
```

You should see:
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
```

**Keep this terminal open.**

## Step 5: Access the Application

Open your browser and visit:

### Test Pages

1. **Health Check** (Backend Status)
   ```
   http://localhost:5173/healthcheck
   ```
   Shows if backend is running and responds with status, timestamp, uptime.

2. **Users** (Full Stack Demo)
   ```
   http://localhost:5173/users
   ```
   Displays all users from the SQLite database. This proves:
   - ✓ Frontend can reach backend
   - ✓ Backend can query database
   - ✓ Data flows: Database → Backend → Frontend

## Architecture

```
┌─────────────────────────────┐
│     Browser                 │
│   http://localhost:5173     │
└──────────────┬──────────────┘
               │
               │ HTTP fetch()
               ↓
┌─────────────────────────────┐
│   Vite Frontend Server      │
│   http://localhost:5173     │
│   (React + Tailwind)        │
└──────────────┬──────────────┘
               │
               │ HTTP GET requests
               ↓
┌─────────────────────────────┐
│   Python Backend Server     │
│   http://localhost:5000     │
│   (HTTP + SQLite queries)   │
└──────────────┬──────────────┘
               │
               │ SQL queries
               ↓
┌─────────────────────────────┐
│   SQLite Database           │
│   trans/database/app.db     │
└─────────────────────────────┘
```

## Available Backend Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | API info |
| `/healthcheck` | GET | Backend status |
| `/users` | GET | List all users from database |

Example with curl:
```bash
curl http://localhost:5000/healthcheck
curl http://localhost:5000/users
```

## Database Schema

Currently, the database has one table:

### `users` table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    role TEXT NOT NULL
)
```

Sample data:
- Alice Dupont (accountant@example.com) - accountant
- Bob Martin (bob@example.com) - client
- Charlie Leblanc (charlie@example.com) - admin

To reset the database:
```bash
cd trans/database
python3 init.py  # Recreates app.db with fresh data
```

## Stopping the Servers

1. **Backend**: Press `Ctrl+C` in the terminal running `python3 app.py`
2. **Frontend**: Press `Ctrl+C` in the terminal running `npm run dev`

## Troubleshooting

### Port Already in Use

If you see "Address already in use" for port 5000 or 5173:

**For port 5000 (Backend):**
```bash
lsof -i :5000  # Find process using port 5000
kill <PID>     # Kill the process
```

**For port 5173 (Frontend):**
```bash
lsof -i :5173  # Find process using port 5173
kill <PID>     # Kill the process
```

### Backend not responding from Frontend

1. Make sure backend is running: `python3 app.py`
2. Check if it's listening: `curl http://localhost:5000/healthcheck`
3. Check browser console for CORS errors (should have `Access-Control-Allow-Origin` headers)

### Database file not found

Run the initialization script:
```bash
cd trans/database
python3 init.py
```

## Next Steps

- Add more database tables for invoices, verification reports, etc.
- Implement user authentication
- Add more backend endpoints for invoice processing
- Expand frontend with additional pages and components

---

**Last Updated**: 12 August 2026
