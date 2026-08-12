FROM python:3.12-slim

# Install system dependencies including FFmpeg
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy requirements and install (cached layer)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy transcoder source code
COPY transcoder.py .

# Run the Python Kafka consumer
CMD ["python", "transcoder.py"]
