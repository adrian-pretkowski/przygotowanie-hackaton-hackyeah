# Pokaż i zapytaj: image recognition demo

Pre-HackYeah spike: we check whether a multimodal model can accurately point at elements in a photo.
Project description: [docs/pokaz_zapytaj.md](docs/pokaz_zapytaj.md).
Demo plan and hypotheses: [docs/plan_demo_rozpoznawanie_obrazu.md](docs/plan_demo_rozpoznawanie_obrazu.md).

| Directory | Contents |
|-----------|----------|
| `backend/` | Java 21 + Spring Boot 4.1, Anthropic Java SDK (`com.anthropic:anthropic-java`) |
| `frontend/` | Angular 20 (standalone, zoneless, signals) |
| `docs/` | project description and demo plan |

## Running

### 1. Backend (port 8080)

Requires an API key. It is kept in the local `backend/.env` file, which is in `.gitignore` and never committed:

```powershell
cd backend
cp .env.example .env    # first time only; then set ANTHROPIC_API_KEY in .env
./mvnw spring-boot:run
```

`backend/.env.example` is the committed template with empty values. When you add a new variable, add it there too.
The `ANTHROPIC_API_KEY` environment variable also works if `.env` has no key.
The `.env` file is looked up in the working directory (`.env`) and in `backend/.env`, so both `./mvnw` from `backend/` and running from IntelliJ opened at the repo root work.

Settings in `backend/src/main/resources/application.yml` (`vision` section):

| Key | Default | Description |
|-----|---------|-------------|
| `model` | `claude-opus-5-5` | for comparisons e.g. `claude-sonnet-5-5`, `claude-haiku-4-5` |
| `effort` | `low` | `low`…`max`; empty = model default (must be empty for Haiku) |
| `max-image-side` | `1568` | longer side of the image sent to the model |
| `refusal-fallback` | `true` | server-side fallback when the model refuses; set to `false` for Haiku |

They can be overridden without editing the file: in `backend/.env` (`VISION_MODEL`, `VISION_EFFORT`, `VISION_REFUSAL_FALLBACK`)
or with an argument, e.g. `./mvnw spring-boot:run -Dspring-boot.run.arguments="--vision.model=claude-sonnet-5-5"`.

Prompts live in `backend/src/main/resources/prompts/` (`system.txt`, `locate.txt`, `compare.txt`). They are in Polish on purpose, because the app answers users in Polish.

### 2. Frontend (port 4200)

```powershell
cd frontend
npm install   # first time only
npm start     # http://localhost:4200, /api is proxied to :8080
```

**On a phone** the camera requires HTTPS:

```powershell
npm run start:phone   # https://<computer-IP>:4200, accept the certificate warning
```

The phone and the computer must be on the same network. Alternative: a tunnel (`cloudflared tunnel --url http://localhost:4200` or ngrok).

## API

| Endpoint | Input (multipart) | Output |
|----------|-------------------|--------|
| `POST /api/vision/locate` | `image`, `question` | `LocateResult`: answer, action, `target.bbox` (0–1000), extracted text, confidence |
| `POST /api/vision/compare` | `before`, `after`, `expectedStep` | `CompareResult`: `stepCompleted`, change description, confidence |

Every response also includes metrics: `modelLatencyMs`, `totalLatencyMs`, tokens and image dimensions.

```bash
curl -X POST localhost:8080/api/vision/locate \
  -F "image=@backend/src/main/resources/images/router.jpg" \
  -F "question=Gdzie jest hasło do Wi-Fi?"
```

## Tests

```powershell
cd backend; ./mvnw test      # image preprocessing, JSON schema validation, context startup
cd frontend; npm test
```
