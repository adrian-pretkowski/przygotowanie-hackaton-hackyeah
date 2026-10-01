# "Pokaż i zapytaj" (Show and Ask): a camera-based instruction assistant

## Problem

Everyone struggles with unclear manuals for devices and forms. Seniors and foreigners are hit the hardest.

## Solution

A web app running in the browser on a phone or laptop. The user points the camera at a device and asks a question by voice.
The AI guides them step by step: it marks on the image where to click or what to turn, and recognises when each step has been completed.

## Usage scenario

**Main character:** Ms Halina, 68, wants to change her Wi-Fi password.

1. She opens the page on her phone and allows access to the camera and microphone.
2. She points the camera at the router and asks: *"How do I change my internet password?"*
3. The AI says: *"There is a sticker under the device with the page address and the admin password. Please turn the router over."*
   An arrow appears on the image.
4. Ms Halina turns the router over, the AI reads the sticker and continues: *"Now please open your laptop and type in this address."*
5. **The "wow" moment:** with the camera pointed at the laptop screen, the AI draws a box around the
   "Wireless network settings" field and says: *"Please click here."* After the click it recognises the screen change and moves on to the next step.
6. At the end, Ms Halina gets a short summary of the changes.

**Second demo scenario:** a coffee machine in the room or a paper form (*"Where do I sign here?"*), to show how universal the solution is.

## MVP scope

1. Camera stream in the browser.
2. Voice question and voice answer.
3. An overlay pointing at an element on the image.
4. Step progress tracking (a "done" button or automatic change detection).
5. Optional: uploading a PDF manual as a source for answers.

## Architecture

| Layer | Proposal |
|-------|----------|
| Frontend | Angular: camera and microphone from the browser, overlay on a canvas, large and simple UI |
| Backend | Spring Boot as a proxy to the multimodal model |
| Image processing | Frame sampling every 1–2 s; JSON output: step, description, element coordinates, confidence |
| Logic | Step state machine on the backend |
| Voice | Speech recognition and synthesis (in the browser or via an API) |
| Knowledge (optional) | RAG over the PDF manual with page citations |

## Fit to the judging criteria (estimate)

| Criterion | Weight | Score | Rationale |
|-----------|-------:|------:|-----------|
| Idea & Innovation | 30% | 9 | Multimodal step-by-step guidance with an image overlay |
| Relation to Category | 20% | 9 | Vision, speech and reasoning in one flow |
| Practical Applicability | 20% | 9 | A problem everyone knows, a clear target group |
| Design | 20% | 8 | Simple interface, strong overlay effect |
| Completeness | 10% | 6 | Hard to fully polish within 24 hours |

## Risks

- **Latency:** measure from the start, limit the frame rate.
- **Pointing accuracy:** verify within the first 3–4 hours; plan B is a verbal description instead of an overlay.
- **Hallucinations:** at low confidence, respond with *"I can't see clearly, please show it closer."*
- **Safety:** no guidance for dangerous actions (mains power, gas); show a warning in such cases.
- **Demo props:** bring your own items instead of relying on what will be available on site.

## Comparison and recommendation

This section comes from a comparison of three hackathon project ideas. **Project 3 is "Pokaż i zapytaj"** (this document);
Projects 1 and 2 are described separately.

| Criterion | Weight | Project 1 | Project 2 | Project 3 |
|-----------|-------:|----------:|----------:|----------:|
| Idea & Innovation | 30% | 8 | 7 | 9 |
| Relation to Category | 20% | 9 | 9 | 9 |
| Practical Applicability | 20% | 8 | 7 | 9 |
| Design | 20% | 6 | 8 | 8 |
| Completeness | 10% | 8 | 8 | 6 |
| **Weighted score** | | **7.8** | **7.7** | **8.5** |
| Execution risk | | Low–medium | Low | High |

**Recommendation (opinion):**

- **Highest chance of winning: Project 3**, provided that in the first hours we prove the model points at elements on the image accurately.
  Otherwise, switch to Project 2.
- **Safest result: Project 2**: the easiest to deliver and strong on design, which matters without a designer on the team.
- **Highest potential after the hackathon: Project 1**: the most mature business-wise, but it needs the best-prepared story on stage.
