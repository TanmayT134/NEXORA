# NEXORA Backend

Java 21 + Spring Boot backend for NEXORA. The project includes authentication/JWT, candidate profiles, resume upload and AI resume analysis, interview configuration, adaptive interviews (with TEXT, VOICE, and HYBRID session-mode selection stored), and AI evaluation.

## Quick start

1. Create the MySQL database `nexora_db`.
2. Import this folder into Eclipse as an Existing Maven Project.
3. Configure Java 21.
4. Set the environment variables `DB_PASSWORD`, `JWT_SECRET`, and `GEMINI_API_KEY` in the Eclipse Run Configuration. See [ADAPTIVE_INTERVIEW_GUIDE.md](ADAPTIVE_INTERVIEW_GUIDE.md) for setup details and the adaptive interview API sequence.
5. Run `NexoraBackendApplication.java`.

Create a Base64 JWT secret (at least 32 random bytes), for example with `openssl rand -base64 32`. Do not commit real secrets.

## Gemini quota handling

Set `GEMINI_MODEL` to a model enabled for your Google AI Studio project. The source default is `gemini-3.5-flash`; the environment variable overrides it. Gemini limits are project/model specific and can include requests per minute, input tokens per minute, and requests per day. NEXORA now maps exhausted daily quotas to HTTP 429 with a clear message and does not blindly retry them. Temporary rate limiting is surfaced separately.

For sustained development or production use, link an active billing account to the Google AI Studio / Google Cloud project and check the model's live limits in AI Studio. Changing API keys inside the same project does not create a fresh quota. A paid tier generally increases available limits, but exact limits depend on project, model, and account standing.

To reduce consumption: avoid repeatedly starting the same session, keep prompts concise, retain only the recent conversation context (already capped in the adaptive interview service), and avoid parallel requests from multiple tabs. Use a lower-cost Flash / Flash-Lite model only after confirming it is available for your project and compatible with the configured Spring AI integration.

## Frontend speech modes

The frontend supports TEXT, VOICE, and HYBRID. VOICE uses browser SpeechSynthesis and SpeechRecognition for turn-based speech; it is not Gemini Live streaming audio. Browser speech support and microphone permissions vary by browser.

The application uses Hibernate `ddl-auto=update` to add new adaptive-interview columns. Back up your database before first launch of this upgraded version.

## Adaptive interview

A new session starts with one AI-generated question. After the candidate answers, call `POST /api/v1/candidate/interview-sessions/{sessionId}/next-question`. Gemini uses the conversation so far to return a contextual follow-up, a new topic, or an end-of-interview decision. The configured question count is the maximum number of main topics; up to two follow-ups are allowed per topic. Session creation accepts `mode` values `TEXT`, `VOICE`, or `HYBRID` (default `TEXT`). The frontend implements browser-based turn-by-turn speech for VOICE mode; it is not a Gemini Live streaming audio integration.

Import `NEXORA_Adaptive_Interview.postman_collection.json` into Postman for the API workflow.

## Security

No database password, JWT secret, or Gemini key is stored in this source project. Supply your own values through environment variables. If credentials were ever committed to an earlier copy or shared repository, rotate them before using this project.

## Verification note

The source has been reviewed and updated, but a Maven package build could not be completed in the preparation environment because Maven was not installed and the Maven Wrapper could not download its distribution (network access unavailable). Importing into Eclipse with Maven connectivity is required to perform the final compile and runtime verification.

## Integrity event policy

All supported integrity event types share one combined counter. The third recorded event completes the active session with termination reason `INTEGRITY_REVIEW`; the frontend then requests evaluation. A single tab-hidden or window-blur event does not immediately terminate the interview. The resulting flag is an automated review signal, not proof of misconduct.
