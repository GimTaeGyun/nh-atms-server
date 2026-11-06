# =====================================================================
# nh-atms-web | React + TypeScript + Vite 프론트엔드
# Build context : Frontend/nh-atms-web/
# Build command : docker build -t nh-atms-web .
# =====================================================================

# ─────────────────────────────────────────────────────────────────────
# Stage 1 : deps
#   목적  : npm 패키지 의존성만 먼저 설치하여 Docker 레이어 캐싱
#   원리  : package.json과 package-lock.json만 복사 후 npm ci 실행
#           → 소스(.tsx)가 바뀌어도 package.json이 안 바뀌면 캐시 HIT
#           → node_modules 재설치(수백 MB) 생략
# ─────────────────────────────────────────────────────────────────────
FROM node:22-alpine AS deps
WORKDIR /app

# package.json과 package-lock.json만 먼저 복사
# package.json      : 패키지 목록과 버전 범위 정의
# package-lock.json : 실제 설치된 정확한 버전 잠금 파일
COPY package.json package-lock.json ./

# npm ci (clean install) 선택 이유:
#   npm install : package.json 기준으로 최신 패키지 설치 → 버전이 달라질 수 있음
#   npm ci      : package-lock.json을 엄격히 따름 → 항상 동일한 버전 보장
#                 CI/CD 환경에서 재현성을 위해 반드시 npm ci 사용
#   --ignore-scripts : postinstall 등 실행 방지 (보안 + 속도)
RUN npm ci --ignore-scripts


# ─────────────────────────────────────────────────────────────────────
# Stage 2 : builder
#   목적  : TypeScript 컴파일 + Vite 빌드 → dist/ 생성
#   원리  : Stage 1의 node_modules를 이어받아 소스만 추가 후 빌드
# ─────────────────────────────────────────────────────────────────────
FROM deps AS builder

# 소스 코드 전체 복사
# .dockerignore에 의해 node_modules/, dist/는 제외됨
COPY . .

# TypeScript 컴파일 + Vite 번들링 실행
# package.json의 "build": "tsc -b && vite build"
#   tsc -b   : TypeScript 타입 검사 + 컴파일 (오류 시 빌드 실패)
#   vite build : JS/CSS 번들링, 트리 쉐이킹, 해시 파일명 생성
# 결과물: dist/ 디렉터리
#   dist/index.html
#   dist/assets/index-[hash].js   ← 해시가 포함되어 장기 캐싱 가능
#   dist/assets/index-[hash].css
RUN npm run build


# ─────────────────────────────────────────────────────────────────────
# Stage 3 : runtime
#   목적  : nginx로 정적 파일 서빙 + API 프록시
#   원리  : Node.js 불필요, nginx만 있으면 됨 → 이미지 크기 대폭 절감
#           node:22-alpine (~200MB) → nginx:alpine (~50MB)
# ─────────────────────────────────────────────────────────────────────
FROM nginx:alpine
# nginx:alpine 선택 이유:
#   - 공식 nginx 이미지 기반 (안정성 보장)
#   - Alpine Linux로 경량 (~50MB)
#   - 기본 nginx 설정을 교체해서 커스텀 설정 적용

# 기본 nginx 설정 제거 후 커스텀 설정 복사
# /etc/nginx/conf.d/default.conf : nginx가 기본으로 로드하는 서버 블록 설정
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Stage 2 빌드 결과물(dist/)을 nginx 서빙 루트로 복사
# /usr/share/nginx/html : nginx:alpine의 기본 문서 루트
COPY --from=builder /app/dist /usr/share/nginx/html

# 포트 선언 (실제 개방은 docker run -p 또는 docker-compose ports로)
EXPOSE 80

# nginx 프로세스를 포그라운드(foreground)로 실행
# daemon off : 기본적으로 nginx는 백그라운드 데몬으로 실행되는데,
#              Docker 컨테이너는 PID 1 프로세스가 종료되면 컨테이너도 종료됨
#              → nginx를 포그라운드로 실행해야 컨테이너가 살아있음
CMD ["nginx", "-g", "daemon off;"]
