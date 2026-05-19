# Bendeoji

Spring Boot + SQLite 기반 두더지 잡기 게임입니다. 화면은 Spring Boot 정적 리소스로 서빙되고, 점수는 서버 세션에서 계산한 뒤 SQLite 리더보드에 저장합니다.

## 실행

```bash
./gradlew bootRun
```

Gradle이 설치되어 있다면 다음처럼 실행해도 됩니다.

```bash
gradle bootRun
```

접속 경로:

- 게임: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- SQLite 파일: `data/bendeoji.sqlite`

## Docker 실행

```bash
docker build -t bendeoji .
docker run --rm -p 8080:8080 -v "$(pwd)/data:/app/data" bendeoji
```

컨테이너 안에서는 기본적으로 SQLite 파일을 `/app/data/bendeoji.sqlite`에 저장합니다. 다른 경로를 쓰고 싶으면 환경변수로 바꿀 수 있습니다.

```bash
docker run --rm -p 8080:8080 \
  -e APP_DATABASE_PATH=/app/data/bendeoji.sqlite \
  -v "$(pwd)/data:/app/data" \
  bendeoji
```

## Railway 배포

이 프로젝트는 루트의 `Dockerfile`과 `railway.toml`을 기준으로 Railway에 배포합니다. Railway는 배포 시 `PORT` 환경변수를 주입하고, 앱은 `server.port=${PORT:8080}` 설정으로 그 포트를 사용합니다.

### 1. GitHub에 올려서 배포

1. 이 프로젝트를 GitHub 저장소에 push합니다.
2. Railway 대시보드에서 `New Project`를 누릅니다.
3. `Deploy from GitHub repo`를 선택하고 저장소를 연결합니다.
4. Railway가 루트의 `Dockerfile`을 감지해서 빌드합니다.
5. 서비스가 만들어지면 `Settings` 또는 `Volumes`에서 Volume을 추가합니다.
6. Volume mount path는 `/app/data`로 설정합니다.
7. 서비스 `Variables`에 아래 값을 추가합니다.

```text
APP_DATABASE_PATH=/app/data/bendeoji.sqlite
```

8. `Networking`에서 `Generate Domain`을 눌러 공개 URL을 만듭니다.

### 2. CLI로 배포

```bash
brew install railway
railway login
railway init
railway up
```

배포 후 Railway 대시보드에서 Volume을 `/app/data`에 붙이고, `APP_DATABASE_PATH=/app/data/bendeoji.sqlite` 변수를 설정합니다.

헬스체크 경로는 `railway.toml`에 `/api/health`로 설정되어 있습니다.

## Fly.io 배포

`fly.toml.example`을 복사해서 앱 이름을 본인 Fly 앱 이름으로 바꿉니다.

```bash
cp fly.toml.example fly.toml
```

`fly.toml`에서 이 값을 수정합니다.

```toml
app = "본인-fly-app-name"
primary_region = "nrt"
```

처음 배포할 때는 앱과 볼륨을 만든 뒤 deploy 합니다.

```bash
fly apps create 본인-fly-app-name
fly volumes create bendeoji_data --region nrt --size 1
fly deploy
```

Fly Volume은 `/app/data`에 붙고, 앱은 `APP_DATABASE_PATH=/app/data/bendeoji.sqlite`를 사용합니다. 이 볼륨이 있어야 리더보드 SQLite 파일이 배포/재시작 후에도 유지됩니다.

## Tailwind CSS

스타일 원본은 `src/main/resources/static/styles/input.css`입니다. 수정 후 CSS를 다시 만들려면:

```bash
npm install
npm run build:css
```

빌드 결과는 `src/main/resources/static/assets/app.css`에 저장됩니다. 현재 저장소에는 바로 실행해도 보이도록 빌드된 CSS 파일도 같이 들어 있습니다.

## 이미지/사운드 교체

게임 에셋 위치:

- 두더지: `src/main/resources/static/assets/mole.svg`
- 기본 뿅망치 커서: `src/main/resources/static/assets/mallet-cursor.svg`
- 클릭 중 뿅망치 커서: `src/main/resources/static/assets/mallet-cursor-down.svg`
- 타격음: `src/main/resources/static/assets/hit.wav`

같은 파일명으로 교체하면 코드 수정 없이 반영됩니다. PNG/JPG/WebP 파일로 바꿔도 괜찮습니다. 파일 확장자를 바꾸는 경우에는 `GameService.config()`의 `AssetPaths` 값 또는 `/api/game/config` 응답을 사용하는 프론트 로직을 같이 바꾸면 됩니다. `hit.wav`가 없으면 브라우저 Web Audio로 만든 기본 타격음이 재생됩니다.

## API 요약

- `GET /api/game/config`: 게임판, 시간, 점수, 이미지 경로 조회
- `POST /api/games`: 게임 세션 생성 및 랜덤 스폰 스케줄 발급
- `GET /api/games/{sessionId}`: 현재 점수 상태 조회
- `POST /api/games/{sessionId}/hits/{eventId}`: 보이는 두더지 hit 처리
- `POST /api/games/{sessionId}/finish`: 게임 종료 및 리더보드 저장
- `GET /api/leaderboard?limit=10`: 리더보드 조회
- `GET /api/health`: 배포 헬스체크

## 구현 메모

- 구멍 배치는 위에서 아래로 `8 / 7 / 8`개입니다.
- 클라이언트가 점수를 직접 제출하지 않고, 서버가 세션별 스폰 이벤트와 hit 여부로 점수를 계산합니다.
- 리더보드는 닉네임별 최고 점수만 저장합니다. 같은 닉네임으로 더 낮은 점수를 기록하면 기존 최고 점수를 유지하고, 더 높은 점수일 때만 업데이트합니다.
- 인증이 없는 캐주얼 게임 기준의 검증입니다. 실제 서비스에서 강한 치팅 방지가 필요하면 로그인, 서버 권위형 실시간 이벤트 검증, 요청 서명 또는 WebSocket 기반 판정이 필요합니다.
