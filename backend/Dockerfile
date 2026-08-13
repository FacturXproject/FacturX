FROM python:3.12-alpine

WORKDIR /app

# Copy backend code
COPY . .

# Expose port
EXPOSE 5000

# Start server (accessible from outside container)
CMD ["python3", "app.py"]
