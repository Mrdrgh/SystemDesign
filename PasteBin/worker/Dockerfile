FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY worker.py .

# Run Python unbuffered so stdout logs stream immediately into docker logs
CMD ["python", "-u", "worker.py"]
