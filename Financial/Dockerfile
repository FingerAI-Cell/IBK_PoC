# Node.js 최신 버전 (로컬과 맞추기)
FROM node:22-alpine

# 컨테이너 내부 작업 디렉토리 설정
WORKDIR /app

# package.json과 package-lock.json 복사 후 의존성 설치
COPY package*.json ./
RUN npm install

# 소스 코드 전체 복사
COPY . .

# 포트 설정 (Express가 사용하는 포트)
EXPOSE 3005

# 실행 명령어
CMD ["node", "financialAPI.js"]