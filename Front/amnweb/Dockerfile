# Node.js 22 이미지 사용
FROM node:22-alpine AS base

WORKDIR /app

# libc6-compat 설치 (필요한 경우)
RUN apk add --no-cache libc6-compat

# 의존성 설치
COPY package.json yarn.lock* package-lock.json* pnpm-lock.yaml* .npmrc* ./
RUN \
  if [ -f yarn.lock ] then yarn --frozen-lockfile; \
  elif [ -f package-lock.json ]; then npm ci; \
  elif [ -f pnpm-lock.yaml ]; then corepack enable pnpm && pnpm i --frozen-lockfile; \
  else echo "Lockfile not found." && exit 1; \
  fi

# 소스 코드 복사 및 빌드
COPY . .
RUN npm run build

# 실행 이미지
FROM node:22-alpine AS runner

WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# 빌드된 파일 복사
COPY --from=base /app/.next/standalone ./
COPY --from=base /app/.next/static ./.next/static
COPY --from=base /app/public ./public
COPY --from=base /app/package.json ./

EXPOSE 3000

# 아래가 중요 서버 도커파일임
# # Node.js 22 이미지 사용
# FROM ibk-poc-front:latest

# # 작업 디렉토리 설정
# WORKDIR /app

# # 패키지 파일 복사
# COPY package*.json ./

# # npm 버전 업그레이드 (10.9.0 맞추기)
# RUN npm install -g npm@10.9.0

# # 의존성 설치
# RUN npm install

# # 소스 코드 복사
# COPY . .

# # Next.js 프로젝트 빌드
# RUN npm run build

# # 포트 설정
# EXPOSE 3000