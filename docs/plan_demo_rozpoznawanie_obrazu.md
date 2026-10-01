# Demo plan: image recognition for "Pokaż i zapytaj" (Show and Ask)

> Goal: **before HackYeah**, check whether the riskiest part of the project works: can a multimodal model
> accurately **point at an element in a photo** (coordinates), read stickers and detect screen changes,
> within a response time that is acceptable in a conversation.
> Voice, the step state machine, RAG over PDFs and a polished UI are **out of scope for the demo**.
>
> Stack: **Java 21 + Spring Boot 4.1 + Angular 20** (Spring Initializr no longer offers the 3.x line).
>
> **Status (2026-10-01):** the skeleton from stages 0, 2 and 3 is ready and the first live call works. How to run it: [README](../README.md).
> Next: stage 1 (test set), then stage 4 (evaluation).

---

## 1. Hypotheses to verify (go / no-go criteria)

| # | Hypothesis | How we measure | "GO" threshold |
|---|------------|----------------|----------------|
| H1 | The model points at the right element (button, field, knob, port) | The centre of the returned box lies inside the hand-labelled reference box | ≥ 70% hits on the test set |
| H2 | The model reads text from the router sticker (address, SSID, password) | Exact field match | ≥ 80% of fields correct, phone photos |
| H3 | The model recognises that a step was completed (screen / device state change) | Before / after frame pair → `stepCompleted: true/false` | ≥ 85% correct decisions |
| H4 | Latency is acceptable | Time from sending the frame to drawing the overlay (p50 / p90) | p50 ≤ 3 s, p90 ≤ 6 s |
| H5 | The `confidence` field is meaningful | Are low-confidence answers actually wrong more often? | Clear correlation; a "please show it closer" threshold can be set |

**Decision after the demo:**
- H1 + H4 met → go with Project 3 including the overlay.
- H1 not met, but H2/H3 met → Project 3 with **plan B** (verbal description + grid / numbered regions, section 8).
- H4 clearly not met, or everything weak → switch to Project 2.

---

## 2. Demo scope

**In scope:**
1. Camera preview in Angular (laptop and phone) and a "Take a photo and ask" button with a question text field.
2. Sending the frame (JPEG) and the question to the Spring Boot backend.
3. The backend calls the multimodal model and returns structured JSON.
4. The frontend draws a box or an arrow on a `<canvas>` over the captured frame.
5. "Compare steps" mode: two frames (before / after) + the expected step → was the step completed.
6. **Batch evaluation mode**: an endpoint / runner that runs the whole test set and computes the H1–H4 metrics.
7. Logging the duration and raw model response of every call.

**Out of scope:** speech recognition / synthesis, step state machine, RAG over PDFs, auth, polished design, deployment.

---

## 3. Demo architecture

```
┌──────────────────────── Angular ─────────────────────────────┐
│ CameraComponent         (getUserMedia, <video>, capture)     │
│ AnnotatedFrameComponent (<canvas> on the frozen frame, bbox) │
│ VisionDemoComponent     (question, buttons, JSON, metrics)   │
│ EvalPage                (batch results table - planned)      │
└──────────────┬───────────────────────────────────────────────┘
               │ POST /api/vision/locate   (multipart: image, question)
               │ POST /api/vision/compare  (multipart: before, after, expectedStep)
               │ POST /api/eval/run        (runs the test set - planned)
┌──────────────▼──────────── Spring Boot ──────────────────────┐
│ VisionController                                             │
│ VisionService ── VisionProvider (interface)                  │
│                   ├─ ClaudeVisionProvider                    │
│                   └─ (optional) GeminiVisionProvider         │
│ ImagePreprocessor (resize, compression, EXIF rotation)       │
│ PromptTemplates   (prompt templates in resources/prompts)    │
│ EvalRunner        (test set → metrics → CSV/JSON - planned)  │
└──────────────────────────────────────────────────────────────┘
```

**Why the `VisionProvider` interface:** coordinate accuracy depends heavily on the model.
It is worth comparing at least 2 models / providers on the same test set and picking the better one before the hackathon.
Candidates:
- Claude: `claude-opus-5-5` (quality) vs `claude-haiku-4-5` (speed). Official SDK: `com.anthropic:anthropic-java`.
- Optionally a second provider with native bounding-box support (e.g. Gemini, which returns `box_2d` on a 0–1000 scale) as a baseline.

API keys go in `backend/.env` or environment variables (`ANTHROPIC_API_KEY` etc.), never in the repo.

---

## 4. Response contract (JSON)

### `/api/vision/locate`

```json
{
  "deviceType": "router",
  "answer": "Na spodzie routera jest naklejka z adresem i hasłem. Proszę obrócić urządzenie.",
  "action": "ROTATE",
  "target": {
    "visible": true,
    "label": "naklejka z danymi logowania",
    "bbox": { "x": 412, "y": 610, "w": 180, "h": 95 }
  },
  "extractedText": { "url": "192.168.0.1", "ssid": "...", "password": "...", "other": "" },
  "confidence": 0.82,
  "needsBetterView": false,
  "safetyWarning": ""
}
```

User-facing values (`answer`, `label`) are in Polish, because that is the language of the app's users.

- `action`: `CLICK | PRESS | TURN | ROTATE | READ | TYPE | NONE`; determines the overlay type (box / rotation arrow).
- `bbox` on a **normalised 0–1000 scale** relative to the image sent, independent of resolution. The frontend converts it to pixels.
- When the element is not visible: `target.visible = false` and `needsBetterView: true`. A flag is used instead of `null` because the structured outputs schema is simpler without nullable fields.
- `safetyWarning`: non-empty for mains power, gas and similar. This path is tested too.
- The API wraps the result in `VisionResponse`: `result`, `model`, `modelLatencyMs`, `totalLatencyMs`, tokens, image dimensions.

### `/api/vision/compare`

```json
{ "stepCompleted": true, "observedChange": "Otworzył się ekran ustawień Wi-Fi", "confidence": 0.9 }
```

The JSON schema is generated from Java records (`LocateResult`, `CompareResult`) and enforced by the API's **structured outputs**
(`output_config.format`), so the response always matches the schema and no retries on parse errors are needed.
Errors only occur when the model refuses (`stop_reason=refusal`) or the response is truncated (`max_tokens`). We count how often that happens.

---

## 5. Implementation stages

Estimates for 1–2 people. About 2–3 evenings of work in total.

### Stage 0: Setup (0.5 h)
- [x] Repo with two directories: `backend/` (Spring Boot, Maven) and `frontend/` (Angular CLI).
- [x] API key (`ANTHROPIC_API_KEY` in `backend/.env`) and a "hello world" test: `curl -F image=@photo.jpg -F question=... localhost:8080/api/vision/locate`.

### Stage 1: Test set (1–1.5 h). **The most important step, do it first**
- [ ] Take **30–40 photos** of your own items with a phone (the same props we will bring to the hackathon):
  - router: front, back (ports), bottom with the sticker. Different angles, lighting, distance.
  - laptop screen photographed with a phone: router admin panel (login page, menu, Wi-Fi settings). Glare, skew.
  - coffee machine / washing machine / remote: buttons and knobs.
  - paper form: "where do I sign", "where do I enter my PESEL number".
- [ ] 8–10 **before / after pairs** for H3 (screen before and after a click, router before and after turning it over).
- [ ] A `dataset/labels.json` file: for each photo the question, the expected element, a **hand-labelled bbox** (0–1000 scale) and the expected text to read.
  For labelling, a simple Angular mode ("click and drag" → save coordinates) or a free tool such as makesense.ai is enough.

### Stage 2: Backend: model call (2–3 h)
- [x] `ImagePreprocessor`: EXIF orientation correction, scaling of the longer side (`vision.max-image-side`, default 1568 px), JPEG q=0.8. Measure the effect of resolution on H1 and H4.
- [x] `ClaudeVisionProvider`: image as base64 in the message, structured outputs with a schema generated from Java records.
  Reasoning depth is controlled with the `effort` parameter (`vision.effort`, default `low` for latency), not `temperature`, which newer models do not accept.
- [x] Prompts in `resources/prompts/` (`system.txt`, `locate.txt`, `compare.txt`) contain:
  - the role: "an assistant helping seniors operate a device",
  - the **image dimensions** and a description of the coordinate system (0–1000, origin in the top-left corner),
  - the rule: if the element is not visible / uncertain → `target.visible: false`, `needsBetterView: true`,
  - safety rules,
  - answers in Polish, short, simple sentences.
- [x] `/locate` and `/compare` endpoints + logging (DEBUG): duration, model, `stop_reason`, tokens.

### Stage 3: Frontend: camera and overlay (2–3 h)
- [x] `CameraComponent`: `getUserMedia({ video: { facingMode: 'environment' } })`, rear camera on phones.
- [x] Frame capture: `canvas.drawImage(video)` → `toBlob('image/jpeg')`, scaled to 1568 px already on the frontend.
  Plus an "Upload photo" button to test photos from the test set without a camera.
- [x] `AnnotatedFrameComponent`: the overlay is drawn on the **frozen frame**, not over `<video>`. The canvas has the image's
  resolution 1:1 and is scaled by CSS, so the `object-fit` letterbox problem does not occur at all:
  `px = bbox.x / 1000 * imageWidth`.
- [x] Drawing: pulsing box + label + dimmed background. A curved arrow for `ROTATE` / `TURN`.
- [x] Panel with the raw JSON, response time, tokens and session p50/p90 latency.
- [x] "Was the step completed?" mode: before / after photos + expected step → `/compare`.
- [ ] Phone test: `npm run start:phone` (`ng serve --host 0.0.0.0 --ssl`). **The camera requires HTTPS** outside localhost. Alternatively a tunnel (ngrok / cloudflared).

### Stage 4: Batch evaluation (1.5–2 h)
- [ ] `EvalRunner`: for each entry in `labels.json` calls the provider, stores the result and computes:
  - **H1**: hit rate (box centre inside the reference box) + mean IoU,
  - **H2**: text field accuracy,
  - **H3**: accuracy for before / after pairs,
  - **H4**: p50 / p90 latency,
  - **H5**: hit rate per `confidence` bucket (<0.5, 0.5–0.8, >0.8),
  - share of refusals / truncated responses.
- [ ] Output to `eval-results/<model>-<date>.csv` + a simple Angular page: photo, reference box (green), model box (red).
  A visual review of the errors is the fastest way to see *why* the model is wrong.
- [ ] Run for 2–3 configurations (model × resolution) and compare.

### Stage 5: Experiments to improve accuracy (1–2 h, if H1 is weak)
Every experiment is measured on the same set:
1. **Grid on the image**: the backend overlays a numbered grid (e.g. 6×6) before sending, the model returns a cell number.
2. **Set-of-Marks**: pre-number candidates (e.g. rectangles from edge detection / OCR), the model picks a number.
3. **Two-stage "zoom-in"**: first a coarse region, then a higher-resolution crop of that region → precise bbox.
4. Different input resolution and a shorter, stricter prompt.

### Stage 6: Live test (1 h)
- [ ] Walk through Ms Halina's scenario with a real router and laptop: 3–4 questions, note time and accuracy.
- [ ] Automatic sampling every 2 s (optional): are cost and latency acceptable, or do we stay with the button.
- [ ] Record a short demo video (also useful as a backup on stage).

---

## 6. Repository structure

```
/backend
  src/main/java/.../vision/    VisionController, VisionService, VisionProvider, ClaudeVisionProvider, ImagePreprocessor
  src/main/java/.../eval/      EvalRunner, Metrics (planned)
  src/main/resources/prompts/  system.txt, locate.txt, compare.txt
  src/main/resources/application.yml   model, effort, max-image-side
  .env.example                 local configuration template (.env is not committed)
/frontend
  src/app/vision/              camera, annotated-frame, vision-demo, vision.service
/dataset                       (planned)
  images/  pairs/  labels.json
/eval-results                  (planned)
/docs
```

---

## 7. Technical pitfalls (good to know up front)

- **Phone photo orientation (EXIF)**: without correction the coordinates end up rotated by 90°.
- **Resolution**: models downscale images anyway. Sending 4K adds time and cost with no benefit. Scale on the frontend before sending.
- **Coordinate system**: always tell the model the image size and the scale you want the result in. Normalising to 0–1000 simplifies the frontend.
- **Letterbox in `<video>`**: the most common cause of "the box is next to the element" even though the model answered correctly. Check on the test set (static photos) first, only then on the camera.
- **Screen photographed with a phone**: glare, moiré, skew. Include it in the test set, because it is the key "wow" moment.
- **HTTPS for the camera** on the phone, and CORS between `ng serve` and Spring Boot (proxy in `proxy.conf.json`).
- **API limits / costs**: set a budget for the evaluation. With automatic sampling every 1–2 s, the cost grows quickly.

---

## 8. Plan B, if pointing is inaccurate

1. **Grid / numbered regions** (stage 5): less impressive, but usually much more reliable.
2. **Verbal description + area highlight** (e.g. "bottom-right corner, round button") with a large, blurred area instead of a precise box.
3. **Handle the laptop screen differently**: instead of photographing the screen, the user shares it via `getDisplayMedia()`.
   The image is then sharp, without glare, and pointing is much easier. The camera stays for physical devices.

---

## 9. Checklist of results to bring to the hackathon

- [ ] H1–H5 metrics table for the compared models + go / no-go decision.
- [ ] Chosen model, resolution and final prompt (in the repo).
- [x] Working Angular + Spring Boot skeleton with camera and overlay (reusable as the project base).
- [ ] List of cases where the model fails, and how the demo scenario works around them.
- [ ] Props (router, form, possibly a coffee machine) checked in the demo.
- [ ] Short video of a successful run as a backup.
