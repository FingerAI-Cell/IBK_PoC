# 1. Python 3.9 이미지 기반 (원하는 버전 선택 가능)
FROM python:3.9-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 필요한 시스템 패키지 설치
# - psycopg2를 위한 PostgreSQL 헤더 파일 설치 포함
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# 4. 의존성 파일 복사
COPY requirements.txt /app/

# 5. Python 패키지 설치
RUN pip install --no-cache-dir -r requirements.txt
