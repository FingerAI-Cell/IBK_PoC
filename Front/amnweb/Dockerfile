# 1. Node.js가 포함된 베이스 이미지 선택
FROM node:16

# 2. 컨테이너 작업 디렉토리 설정
WORKDIR /usr/src/app

# 3. 의존성 설치를 위해 필요한 파일 복사
COPY package*.json ./

# 4. 의존성 설치
RUN npm install

# 5. 전체 소스코드 복사
COPY . .

# 6. 빌드 실행 (Next.js는 빌드 과정 필요)
RUN npm run build

# 7. 컨테이너 실행 시 사용될 포트 노출
EXPOSE 3000

# 8. 실행 명령어 (Next.js 애플리케이션 시작)
CMD ["npm", "start"]
