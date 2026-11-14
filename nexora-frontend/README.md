# NEXORA Frontend

React + Vite frontend for the NEXORA Spring Boot backend.

## Requirements
- Node.js 20.19+ or 22.12+ (Node 22 LTS recommended)
- npm
- NEXORA backend running on `http://localhost:8080`

## Run locally

```bash
npm install
npm run dev
```

Open http://localhost:5173. The Vite development server proxies `/api/*` requests to `http://localhost:8080`, avoiding development CORS configuration. The default `.env` is:

```env
VITE_API_BASE_URL=/api/v1
```

If you change the backend host, update the proxy target in `vite.config.js` and restart Vite.

## Implemented screens
- Registration and login using the existing `/api/v1/auth/*` endpoints
- Protected candidate workspace and dashboard
- Candidate profile create/read/update
- PDF resume upload and AI analysis
- Interview configuration create/list/delete
- TEXT / VOICE / HYBRID session mode selection
- Adaptive interview question and answer loop
- Evaluation report and question-wise feedback

## Interview modes
- **TEXT:** The AI question appears on screen and the candidate types an answer.
- **VOICE:** The first AI question is spoken automatically after the candidate starts the voice session. Each following AI question is also spoken automatically. After the interviewer finishes speaking, the microphone starts listening and the candidate answers by speaking. The browser displays a live transcript for review; selecting **Finish answer & continue** submits that transcript to the existing backend answer endpoint. There are no replay buttons and no typed-answer field in Voice mode.
- **HYBRID:** Questions appear on screen, can optionally be spoken using the browser's speech output, and the candidate types an answer.

### Voice mode browser requirements and limitations
- Use a recent desktop version of Google Chrome or Microsoft Edge for the best support. Browser speech recognition support varies; Firefox and Safari may not provide the required API.
- Click **Start voice interview** and allow microphone access when Chrome prompts. The app now explicitly requests microphone permission before starting the interviewer, then releases that temporary stream so the browser speech-recognition service can open the microphone. Microphone access is available on `localhost` during development and on secure HTTPS origins in deployment.
- Voice mode uses the browser's SpeechSynthesis API for automatic interviewer speech and its SpeechRecognition API for microphone transcription. This is turn-based speech interaction, not a continuously streamed Gemini audio conversation. The transcript is sent as text through the existing NEXORA answer API, so the existing adaptive interview and evaluation services remain unchanged.
- Speech recognition may use the browser vendor's recognition service and its data handling terms. Tell candidates this before collecting voice input; obtain consent and provide an appropriate privacy notice before production use.
- No raw audio recording is uploaded or stored by this frontend. Only the recognized answer text is submitted to the backend. Recognition accuracy depends on the browser, microphone, accent, language and background noise.
- The default recognition language is `en-IN`. Change `recognition.lang` in `src/pages/InterviewRoom.jsx` if another language is required.
- Voice mode reports distinct errors for denied permission, missing/busy microphone, unavailable speech service, network failure, and no detected speech. If the transcript remains empty, check Chrome site permissions, Windows microphone privacy settings, the selected input device, and internet access for Chrome speech recognition.

## Backend contract
This frontend targets the supplied NEXORA backend APIs under `/api/v1`. The candidate must be authenticated and own the interview configuration used to create a session.

## Production deployment
The Vite proxy is development-only. For production, serve the frontend and backend behind the same reverse proxy (recommended), or configure a strict allowed-origin CORS policy in Spring Security and set `VITE_API_BASE_URL` to the backend API base URL. Do not use wildcard CORS with credentials.

## Security notes
- JWT is stored in `localStorage` for this MVP. For a hardened production deployment, consider an HttpOnly, Secure, SameSite cookie strategy and an appropriate CSRF design.
- No API secrets are included in the frontend. Never place Gemini API keys or JWT signing secrets in Vite environment variables.
- AI-generated scores and feedback are practice guidance, not objective hiring assessments.


## Authentication and AI-provider errors

A 401 response is shown on the current screen instead of silently clearing the local token and redirecting. The backend returns a JSON message for missing, expired, or invalid Bearer tokens, and its JWT filter logs validation outcomes without logging token contents. Sign out and sign in again if the token has expired.

If Gemini quota is exhausted, the interview remains recoverable: a failed initial start can be retried while the session is still `CREATED`; if the candidate's answer was saved but the next AI question could not be generated, the screen offers a retry for the next question without resubmitting the answer.

## Integrity event policy

All supported integrity events share one combined warning counter. Any three recorded events, regardless of type, automatically complete the interview and trigger evaluation. A single tab-hidden or window-blur event is recorded as a warning and does not immediately terminate the session. Camera-based checks are approximate and can produce false positives; an integrity flag is not proof of misconduct.
