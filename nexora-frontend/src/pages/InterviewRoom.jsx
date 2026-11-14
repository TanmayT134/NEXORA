import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  CheckCircle2,
  Send,
  Volume2,
  Mic,
  LoaderCircle,
  Sparkles,
  MessageSquareText,
  Headphones,
  AlertCircle,
  Ban,
  Maximize2,
  Code2,
  ShieldCheck,
} from "lucide-react";
import api from "../api/axiosInstance";
import { getErrorMessage } from "../utils/errors";
import IntegrityMonitor from "../components/IntegrityMonitor";

const getSpeechRecognition = () => {
  if (typeof window === "undefined") return null;
  return window.SpeechRecognition || window.webkitSpeechRecognition || null;
};

export default function InterviewRoom() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const [session, setSession] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [current, setCurrent] = useState(null);
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [finished, setFinished] = useState(false);
  const [error, setError] = useState("");
  const [answerSaved, setAnswerSaved] = useState(false);
  const [integrityWarning, setIntegrityWarning] = useState("");
  const [cancelBusy, setCancelBusy] = useState(false);
  const [codeLanguage, setCodeLanguage] = useState("javascript");
  const [codeDraft, setCodeDraft] = useState("function solution(input) {\n  // Write your solution here\n  return input;\n}");
  const [isFullscreen, setIsFullscreen] = useState(Boolean(document.fullscreenElement));
  const [focusReady, setFocusReady] = useState(false);
  const [awaitingStart, setAwaitingStart] = useState(false);
  const [focusWarning, setFocusWarning] = useState("");
  const lastFocusEventRef = useRef(0);
  const integritySubmissionRef = useRef(false);
  const wasFullscreenRef = useRef(false);

  // Voice mode is a hands-free, turn-based speech conversation: the interviewer
  // speaks each generated question automatically, then microphone recognition
  // captures the candidate's spoken answer. No audio replay controls are shown.
  const [voiceStarted, setVoiceStarted] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState("idle");
  const [transcript, setTranscript] = useState("");
  const [voiceError, setVoiceError] = useState("");
  const recognitionRef = useRef(null);
  const shouldListenRef = useRef(false);
  const pendingVoiceSubmitRef = useRef(false);
  const transcriptRef = useRef("");
  const busyRef = useRef(false);
  const voiceModeRef = useRef(false);
  const silenceTimerRef = useRef(null);
  const answerLimitTimerRef = useRef(null);
  const firstSpeechAtRef = useRef(0);

  const isCodingQuestion = current?.questionType === "CODING";
  const isVoiceMode = session?.mode === "VOICE" && !isCodingQuestion;
  voiceModeRef.current = isVoiceMode;
  busyRef.current = busy;

  const loadQuestions = useCallback(async () => {
    const { data } = await api.get(
      `/candidate/interview-sessions/${sessionId}/questions`
    );
    setQuestions(data || []);
    const latest = (data || []).at(-1);
    if (latest) setCurrent(latest);
    return data || [];
  }, [sessionId]);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const { data } = await api.get(
          `/candidate/interview-sessions/${sessionId}`
        );
        if (!alive) return;
        setSession(data);
        if (data.status === "CREATED") {
          // Do not start until a real user gesture can request browser fullscreen.
          if (alive) setAwaitingStart(true);
        } else if (data.status === "IN_PROGRESS") {
          // A refreshed active session must re-enter fullscreen through a user gesture.
          const qs = await loadQuestions();
          if (alive && !qs.length) setError("The opening question was not returned. Check the message below and retry when the backend is available.");
        } else if (data.status === "COMPLETED") {
          if (alive) setFinished(true);
        }
      } catch (e) {
        if (alive) setError(getErrorMessage(e));
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [sessionId, loadQuestions]);

  const advanceToNextQuestion = useCallback(async () => {
    const { data } = await api.post(
      `/candidate/interview-sessions/${sessionId}/next-question`
    );
    if (data.action === "INTERVIEW_FINISHED") {
      setFinished(true);
      if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
      setCurrent(null);
      setSession((s) => ({ ...s, status: "COMPLETED" }));
      setVoiceStatus("finished");
      shouldListenRef.current = false;
      window.speechSynthesis?.cancel();
    } else {
      setCurrent(data.question);
      setQuestions((q) => [...q, data.question]);
    }
    setAnswer("");
    setCodeDraft("function solution(input) {\n  // Write your solution here\n  return input;\n}");
    setAnswerSaved(false);
    transcriptRef.current = "";
    setTranscript("");
    firstSpeechAtRef.current = 0;
    window.clearTimeout(silenceTimerRef.current);
    window.clearTimeout(answerLimitTimerRef.current);
  }, [sessionId]);

  const cancelInterview = async () => {
    if (cancelBusy || !sessionId) return;
    const confirmed = window.confirm("Cancel this interview? You will not be able to resume it.");
    if (!confirmed) return;
    setCancelBusy(true);
    try {
      await api.patch(`/candidate/interview-sessions/${sessionId}/cancel`);
      shouldListenRef.current = false;
      window.clearTimeout(silenceTimerRef.current);
      window.clearTimeout(answerLimitTimerRef.current);
      if (recognitionRef.current) {
        recognitionRef.current.onend = null;
        recognitionRef.current.stop();
        recognitionRef.current = null;
      }
      window.speechSynthesis?.cancel();
      if (document.fullscreenElement) await document.exitFullscreen().catch(() => {});
      navigate("/app/sessions", { replace: true });
    } catch (e) {
      setError(getErrorMessage(e));
      setCancelBusy(false);
    }
  };

  const retryNextQuestion = async () => {
    setBusy(true);
    setError("");
    try {
      await advanceToNextQuestion();
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const submitAnswer = useCallback(
    async (answerText) => {
      if (!current || !answerText.trim() || busyRef.current) return;
      setBusy(true);
      busyRef.current = true;
      setError("");
      try {
        await api.post(`/candidate/interview-questions/${current.id}/answer`, {
          answerText: answerText.trim(),
        });
        setAnswerSaved(true);
        await advanceToNextQuestion();
      } catch (e) {
        setError(getErrorMessage(e));
        setVoiceStatus(isVoiceMode ? "ready" : "idle");
      } finally {
        setBusy(false);
        busyRef.current = false;
      }
    },
    [current, advanceToNextQuestion, isVoiceMode]
  );

  const startListening = useCallback(() => {
    const Recognition = getSpeechRecognition();
    if (!Recognition) {
      setVoiceError("Speech recognition is unavailable. Use the latest desktop Chrome or Edge, or switch to Hybrid mode.");
      setVoiceStatus("error");
      return;
    }
    if (!shouldListenRef.current || pendingVoiceSubmitRef.current || recognitionRef.current) return;

    try {
      const recognition = new Recognition();
      recognition.lang = "en-IN";
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;

      recognition.onstart = () => {
        setVoiceStatus("listening");
        setVoiceError("");
      };
      // Do not clear errors merely because Chrome opened an audio stream.
      // onaudiostart does not prove that speech is actually being detected.
      recognition.onaudiostart = () => {
        console.info("[NEXORA voice] Browser audio capture started.");
      };
      recognition.onspeechstart = () => {
        setVoiceError("");
        setVoiceStatus("listening");
      };
      recognition.onresult = (event) => {
        // Speech events reset the silence timer. A 2-second pause submits automatically.
        if (!firstSpeechAtRef.current) {
          firstSpeechAtRef.current = Date.now();
          answerLimitTimerRef.current = window.setTimeout(() => {
            if (transcriptRef.current.trim() && !pendingVoiceSubmitRef.current) {
              pendingVoiceSubmitRef.current = true;
              shouldListenRef.current = false;
              recognitionRef.current?.stop();
            }
          }, 120000);
        }
        window.clearTimeout(silenceTimerRef.current);
        silenceTimerRef.current = window.setTimeout(() => {
          if (transcriptRef.current.trim() && !pendingVoiceSubmitRef.current) {
            pendingVoiceSubmitRef.current = true;
            shouldListenRef.current = false;
            recognitionRef.current?.stop();
          }
        }, 2000);
        // Keep final text across result events and append the current interim text.
        let finalText = "";
        let interimText = "";
        for (let i = 0; i < event.results.length; i += 1) {
          const result = event.results[i];
          const phrase = result?.[0]?.transcript || "";
          if (result.isFinal) finalText += `${phrase} `;
          else interimText += `${phrase} `;
        }
        const cleaned = `${finalText}${interimText}`.trim();
        transcriptRef.current = cleaned;
        setTranscript(cleaned);
      };
      recognition.onerror = (event) => {
        const messages = {
          "not-allowed": "Chrome blocked microphone access. Allow Microphone for localhost in the address-bar site settings, then try again.",
          "service-not-allowed": "Chrome's speech recognition service is blocked. Check Chrome microphone and speech-service permissions, or use Hybrid mode.",
          "audio-capture": "Chrome cannot access a microphone. Check that the correct microphone is connected and selected in Windows.",
          network: "Chrome's speech recognition service could not connect. Check your internet connection, then try again.",
          "no-speech": "No speech was detected. Move closer to the microphone and speak a little louder.",
          aborted: "Speech recognition was interrupted. Start the microphone again if you still need to answer."
        };
        const message = messages[event.error] || `Speech recognition failed (${event.error || "unknown error"}). Check your microphone and try again.`;
        setVoiceError(message);
        // no-speech must also stop the automatic restart loop. Otherwise the
        // next audio-start event clears the message almost immediately and
        // the user cannot tell why the transcript remains empty.
        if (["not-allowed", "service-not-allowed", "audio-capture", "network", "no-speech"].includes(event.error)) {
          shouldListenRef.current = false;
          setVoiceStatus("error");
        }
      };
      recognition.onend = () => {
        if (recognitionRef.current === recognition) recognitionRef.current = null;
        if (pendingVoiceSubmitRef.current) {
          pendingVoiceSubmitRef.current = false;
          window.clearTimeout(silenceTimerRef.current);
          window.clearTimeout(answerLimitTimerRef.current);
          firstSpeechAtRef.current = 0;
          const spokenAnswer = transcriptRef.current;
          if (spokenAnswer.trim()) {
            setVoiceStatus("processing");
            submitAnswer(spokenAnswer);
          } else {
            setVoiceStatus("ready");
            setVoiceError("I didn't catch an answer. Press Start speaking and try again.");
          }
          return;
        }
        if (shouldListenRef.current && !busyRef.current) {
          // Chrome can end a recognition segment after a pause. Restart only
          // while the current answer is still active and no hard error occurred.
          window.setTimeout(() => {
            if (shouldListenRef.current && !recognitionRef.current && !pendingVoiceSubmitRef.current) {
              startListening();
            }
          }, 350);
        }
      };

      recognitionRef.current = recognition;
      recognition.start();
      setVoiceStatus("ready"); // onstart changes this to listening once Chrome actually starts.
      setVoiceError("");
    } catch (e) {
      recognitionRef.current = null;
      shouldListenRef.current = false;
      setVoiceError(e?.name === "InvalidStateError"
        ? "The microphone is already starting. Wait a moment and try again."
        : e?.message || "Chrome could not start speech recognition. Check microphone permissions and try again.");
      setVoiceStatus("error");
    }
  }, [submitAnswer]);

  // Automatically speak each AI-generated question. The first question is
  // started by the user's Start voice interview click to satisfy browser
  // autoplay/user-activation requirements.
  useEffect(() => {
    if (!isVoiceMode || !voiceStarted || !current?.questionText || finished) return;
    const utterance = new SpeechSynthesisUtterance(current.questionText);
    utterance.lang = "en-IN";
    utterance.rate = 0.96;
    utterance.pitch = 1;
    utterance.onstart = () => setVoiceStatus("speaking");
    utterance.onend = () => {
      if (voiceModeRef.current && !finished) {
        shouldListenRef.current = true;
        setVoiceStatus("listening");
        startListening();
      }
    };
    utterance.onerror = () => {
      setVoiceError("The interviewer voice could not start. Check your browser's speech settings.");
      setVoiceStatus("error");
    };
    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
    return () => {
      utterance.onend = null;
      window.speechSynthesis.cancel();
    };
  }, [current?.id, isVoiceMode, voiceStarted, finished, startListening]);

  useEffect(() => {
    return () => {
      shouldListenRef.current = false;
      pendingVoiceSubmitRef.current = false;
      window.clearTimeout(silenceTimerRef.current);
      window.clearTimeout(answerLimitTimerRef.current);
      if (recognitionRef.current) {
        recognitionRef.current.onend = null;
        recognitionRef.current.stop();
        recognitionRef.current = null;
      }
      if (typeof window !== "undefined") window.speechSynthesis?.cancel();
    };
  }, []);

  const beginVoiceInterview = async () => {
    if (!("speechSynthesis" in window) || !getSpeechRecognition()) {
      setVoiceError("This browser does not provide both speech output and speech recognition. Please use the latest desktop Chrome or Edge browser.");
      setVoiceStatus("error");
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      setVoiceError("This page cannot request microphone access. Open NEXORA on http://localhost:5173 or HTTPS and allow microphone access.");
      setVoiceStatus("error");
      return;
    }

    // Trigger the browser permission prompt directly from the user's click.
    // Stop the temporary stream before Web Speech API opens its own audio input.
    try {
      setVoiceError("");
      setVoiceStatus("ready");
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
      });
      stream.getTracks().forEach((track) => track.stop());
      setVoiceStarted(true);
    } catch (e) {
      const messages = {
        NotAllowedError: "Microphone permission was denied. Click the site icon beside Chrome's address bar, set Microphone to Allow, then reload NEXORA.",
        NotFoundError: "No microphone was found. Connect a microphone and select it in Chrome settings.",
        NotReadableError: "The microphone is busy or unavailable. Close other apps using it (such as Teams or Zoom) and try again.",
        OverconstrainedError: "The selected microphone does not support the requested audio settings. Check your Chrome microphone selection."
      };
      setVoiceError(messages[e?.name] || `Could not access the microphone (${e?.name || "unknown error"}). Check Chrome and Windows microphone permissions.`);
      setVoiceStatus("error");
    }
  };

  const finishVoiceAnswer = () => {
    if (!transcriptRef.current.trim()) {
      setVoiceError("Speak your answer before finishing this turn.");
      return;
    }
    shouldListenRef.current = false;
    pendingVoiceSubmitRef.current = true;
    setVoiceStatus("processing");
    if (recognitionRef.current) {
      recognitionRef.current.stop();
    } else {
      pendingVoiceSubmitRef.current = false;
      submitAnswer(transcriptRef.current);
    }
  };

  const resumeVoiceAnswer = () => {
    setVoiceError("");
    shouldListenRef.current = true;
    startListening();
  };

  const submitTypedAnswer = async (e) => {
    e.preventDefault();
    const response = isCodingQuestion
      ? `[LANGUAGE: ${codeLanguage}]\n[CODE SUBMISSION]\n${codeDraft}`
      : answer;
    await submitAnswer(response);
  };

  const handleIntegrityTermination = useCallback(async (eventData = {}) => {
    if (integritySubmissionRef.current) return;
    integritySubmissionRef.current = true;
    shouldListenRef.current = false;
    window.clearTimeout(silenceTimerRef.current);
    window.clearTimeout(answerLimitTimerRef.current);
    if (recognitionRef.current) {
      recognitionRef.current.onend = null;
      try { recognitionRef.current.stop(); } catch { /* recognition may already be stopped */ }
      recognitionRef.current = null;
    }
    window.speechSynthesis?.cancel();
    setFinished(true);
    setSession((previous) => ({ ...previous, status: "COMPLETED", terminationReason: eventData?.terminationReason || "INTEGRITY_REVIEW" }));
    setVoiceStatus("finished");
    setIntegrityWarning("The combined limit of three integrity events was reached. Interview submitted; preparing your evaluation…");
    if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
    setBusy(true);
    try {
      await api.post(`/candidate/interview-sessions/${sessionId}/evaluate`);
      navigate(`/app/evaluation/${sessionId}`);
    } catch (evaluationError) {
      setError(`The interview was submitted, but evaluation could not be generated: ${getErrorMessage(evaluationError)}. Use Generate evaluation to retry.`);
    } finally {
      setBusy(false);
    }
  }, [sessionId, navigate]);

  const enterFullscreen = async () => {
    setBusy(true);
    setError("");
    try {
      if (!document.fullscreenElement) {
        if (!document.documentElement.requestFullscreen) throw new Error("Fullscreen is not supported by this browser.");
        await document.documentElement.requestFullscreen();
      }
      if (!document.fullscreenElement) throw new Error("The browser did not enter fullscreen. Allow fullscreen to continue.");
      setIsFullscreen(true);
      setFocusReady(true);
      setFocusWarning("");
      if (awaitingStart || session?.status === "CREATED") {
        const { data } = await api.patch(`/candidate/interview-sessions/${sessionId}/start`);
        setSession(data);
        setAwaitingStart(false);
        const qs = await loadQuestions();
        if (!qs.length) setError("Fullscreen is active, but the opening question was not returned. Retry starting the interview.");
      }
    } catch (error) {
      setFocusReady(false);
      setFocusWarning(error?.message || "Fullscreen is required to begin this interview. Please allow fullscreen and try again.");
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    const recordWarning = async (eventType, details) => {
      if (finished || session?.status !== "IN_PROGRESS" || integritySubmissionRef.current) return;
      try {
        const { data } = await api.post(`/candidate/interview-sessions/${sessionId}/integrity-events`, {
          eventType, severity: "HIGH", details
        });
        if (data?.terminated) await handleIntegrityTermination(data);
        else setFocusWarning(`Integrity warning ${data?.eventCount || ""} of ${data?.warningLimit || 3}. Three warnings across all types will automatically submit this interview.`);
      } catch (eventError) {
        setFocusWarning("Focus interruption detected, but the server could not record it. Check your connection immediately.");
        console.error("Could not record interview focus event", eventError);
      }
    };
    const onFullscreen = () => {
      const active = Boolean(document.fullscreenElement);
      setIsFullscreen(active);
      if (wasFullscreenRef.current && !active && !finished && session?.status === "IN_PROGRESS") {
        setFocusReady(false);
        setFocusWarning("Fullscreen was exited. This counts as an integrity warning. Re-enter fullscreen to continue. Three warnings across all event types will automatically submit this interview.");
        recordWarning("FULLSCREEN_EXIT", "Fullscreen was exited during an active interview.");
      }
      wasFullscreenRef.current = active;
    };
    const submitOnFocusLoss = (eventType, details) => {
      if (finished || session?.status !== "IN_PROGRESS" || integritySubmissionRef.current) return;
      setFocusWarning("Focus interruption recorded as an integrity warning. Three warnings across all event types will automatically submit this interview.");
      recordWarning(eventType, details);
    };
    const onVisibility = () => {
      if (document.visibilityState === "hidden") {
        submitOnFocusLoss("TAB_HIDDEN", "The interview document became hidden. This event counts toward the combined three-event integrity limit.");
      }
    };
    const onWindowBlur = () => {
      submitOnFocusLoss("WINDOW_BLUR", "The interview window lost browser focus. This event counts toward the combined three-event integrity limit.");
    };
    document.addEventListener("fullscreenchange", onFullscreen);
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("blur", onWindowBlur);
    return () => {
      document.removeEventListener("fullscreenchange", onFullscreen);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("blur", onWindowBlur);
    };
  }, [sessionId, session?.status, finished, handleIntegrityTermination]);

  const blockClipboard = (event) => {
    event.preventDefault();
    setFocusWarning("Copying, cutting, and pasting are disabled inside the interview workspace.");
  };
  const blockInterviewKeys = (event) => {
    if ((event.ctrlKey || event.metaKey) && ["c", "v", "x"].includes(event.key.toLowerCase())) blockClipboard(event);
  };

  const retryStart = async () => enterFullscreen();

  const evaluate = async () => {
    setBusy(true);
    try {
      await api.post(`/candidate/interview-sessions/${sessionId}/evaluate`);
      navigate(`/app/evaluation/${sessionId}`);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-page">
        <LoaderCircle className="spin" /> Preparing your interview…
      </div>
    );
  }

  if (!finished && (awaitingStart || session?.status === "IN_PROGRESS") && !focusReady) {
    return (
      <div className="interview-focus-gate">
        <div className="focus-gate-orbit"><div className="focus-gate-core"><ShieldCheck size={34}/></div></div>
        <div className="eyebrow">NEXORA SECURE INTERVIEW SPACE</div>
        <h1>{awaitingStart ? "Ready to begin?" : "Resume your interview"}</h1>
        <p>Fullscreen is required before the interview can start or continue. Your session timer and questions will remain untouched until you enter focus mode.</p>
        <div className="focus-gate-checks"><span><ShieldCheck size={15}/> Fullscreen required</span><span><Ban size={15}/> Clipboard restrictions active</span><span><Sparkles size={15}/> AI interviewer ready</span></div>
        {focusWarning && <div className="form-error" role="alert">{focusWarning}</div>}
        {error && <div className="form-error" role="alert">{error}</div>}
        <button className="btn btn-primary focus-gate-start" onClick={enterFullscreen} disabled={busy}>
          {busy ? <LoaderCircle size={17} className="spin"/> : <Maximize2 size={17}/>}
          {busy ? "Entering focus mode…" : awaitingStart ? "Enter fullscreen & start interview" : "Re-enter fullscreen & continue"}
        </button>
        <button className="focus-gate-cancel" onClick={cancelInterview} disabled={cancelBusy}>Cancel interview instead</button>
        <small>Note: browsers allow users to exit fullscreen or switch applications. NEXORA detects and records focus interruptions but cannot lock down your entire device.</small>
      </div>
    );
  }

  return (
    <div className="content-stack interview-content interview-lockdown" onCopy={blockClipboard} onCut={blockClipboard} onPaste={blockClipboard} onContextMenu={blockClipboard} onKeyDown={blockInterviewKeys}>
      <div className="interview-topline">
        <div>
          <div className="eyebrow">LIVE PRACTICE SESSION</div>
          <h1>{session?.targetRole || "Interview session"}</h1>
          <p>
            {session?.interviewType} · {session?.difficulty} · {session?.mode}
          </p>
        </div>
        <div className="interview-header-actions">
          <span className={`status-chip status-${(finished ? "COMPLETED" : session?.status || "CREATED").toLowerCase()}`}>
            {finished ? "COMPLETED" : session?.status || "CREATED"}
          </span>
          {!finished && session?.status === "IN_PROGRESS" && <span className="focus-mode-active"><ShieldCheck size={15}/> {isFullscreen ? "Fullscreen focus active" : "Focus mode"}</span>}
          {!finished && session?.status === "IN_PROGRESS" && <button className="btn btn-danger interview-cancel-button" onClick={cancelInterview} disabled={cancelBusy}>{cancelBusy ? <LoaderCircle size={15} className="spin"/> : <Ban size={15}/>} {cancelBusy ? "Cancelling…" : "Cancel session"}</button>}
        </div>
      </div>

      {focusWarning && !finished && <div className="integrity-top-warning" role="alert"><ShieldCheck size={18}/><span>{focusWarning}</span></div>}
      {integrityWarning && !finished && (
        <div className="integrity-top-warning" role="alert" aria-live="assertive">
          <AlertCircle size={18} />
          <span>{integrityWarning}</span>
        </div>
      )}

      <div className="interview-progress">
        <div>
          <span>Questions generated</span>
          <strong>
            {questions.length} <small>/ {session?.numberOfQuestions || "—"} main topics</small>
          </strong>
        </div>
        <div className="progress-track">
          <span
            style={{
              width: `${Math.min(
                100,
                (questions.length / (session?.numberOfQuestions || 10)) * 100
              )}%`,
            }}
          />
        </div>
      </div>

      {error && (
        <div className="form-error">
          <div>{error}</div>
          {answerSaved ? (
            <button className="btn btn-secondary btn-small" onClick={retryNextQuestion} disabled={busy}>
              {busy ? "Retrying…" : "Retry generating next question"}
            </button>
          ) : session?.status === "CREATED" ? (
            <button className="btn btn-secondary btn-small" onClick={retryStart} disabled={busy}>
              {busy ? "Retrying…" : "Retry starting interview"}
            </button>
          ) : null}
        </div>
      )}

      {finished ? (
        <section className="panel finish-panel">
          <div className="finish-icon"><CheckCircle2 size={32} /></div>
          <div className="eyebrow">SESSION COMPLETE</div>
          <h2>You've completed this interview.</h2>
          <p>
            Your answers are ready for AI evaluation. Generate your feedback to
            review strengths and improvement areas.
          </p>
          <button className="btn btn-primary" onClick={evaluate} disabled={busy}>
            {busy ? <LoaderCircle className="spin" size={16} /> : <Sparkles size={16} />}
            Generate evaluation
          </button>
        </section>
      ) : (
        <section className={`panel question-panel ${isVoiceMode ? "voice-question-panel" : ""}`}>
          <div className="question-top">
            <span className="question-label">
              <MessageSquareText size={16} /> QUESTION {current?.sequenceNumber || questions.length || 1}
            </span>
            {current?.followUp && (
              <span className="followup-chip"><Sparkles size={13} /> Contextual follow-up</span>
            )}
          </div>
          <h2>{current?.questionText || "Loading next question…"}</h2>
          <div className="question-meta">
            <span>{current?.skillName || "General"}</span>
            <span>{current?.questionType || "INTERVIEW"}</span>
            {current?.followUp && <span>Follow-up depth {current.followUpDepth}</span>}
          </div>

          {answerSaved ? (
            <div className="voice-interaction">
              <div className="voice-start-card">
                <div className="voice-orb"><LoaderCircle size={24} /></div>
                <div>
                  <strong>Your answer has been saved.</strong>
                  <p>We are waiting for the AI interviewer to generate the next question. Use the retry button above when the AI service is available; your answer will not be submitted twice.</p>
                </div>
              </div>
            </div>
          ) : isVoiceMode ? (
            <div className="voice-interaction">
              {!voiceStarted ? (
                <div className="voice-start-card">
                  <div className="voice-orb"><Headphones size={25} /></div>
                  <div>
                    <strong>Voice interview</strong>
                    <p>The interviewer will speak each question. When you click Start, Chrome will request microphone access before listening. No replay or typing controls.</p>
                  </div>
                  <button className="btn btn-primary" onClick={beginVoiceInterview}>
                    <Mic size={16} /> Start voice interview
                  </button>
                </div>
              ) : (
                <div className="voice-live-card">
                  <div className="voice-live-header">
                    <div className={`voice-orb ${voiceStatus === "speaking" ? "is-speaking" : voiceStatus === "listening" ? "is-listening" : ""}`}>
                      {voiceStatus === "speaking" ? <Volume2 size={24} /> : <Mic size={24} />}
                    </div>
                    <div className="voice-live-copy">
                      <strong>
                        {voiceStatus === "speaking" && "AI interviewer is speaking"}
                        {voiceStatus === "listening" && "Listening to your answer"}
                        {voiceStatus === "processing" && "Submitting your answer"}
                        {voiceStatus === "ready" && "Ready for your answer"}
                        {voiceStatus === "error" && "Voice input needs attention"}
                        {voiceStatus === "finished" && "Interview finished"}
                        {voiceStatus === "idle" && "Preparing voice interview"}
                      </strong>
                      <span>{voiceStatus === "listening" ? "Speak naturally. Your words will appear below." : voiceStatus === "speaking" ? "Listen to the question, then your microphone will turn on." : "Your answer is transcribed for submission to the interviewer."}</span>
                    </div>
                    <span className={`voice-state-dot ${voiceStatus}`} />
                  </div>

                  <div className="voice-transcript" aria-live="polite">
                    <div className="voice-transcript-label">LIVE TRANSCRIPT</div>
                    <p>{transcript || (voiceStatus === "listening" ? "I'm listening…" : "Your spoken answer will appear here.")}</p>
                  </div>

                  {voiceError && <div className="voice-error"><AlertCircle size={15} /> {voiceError}</div>}

                  <div className="voice-actions">
                    {voiceStatus === "error" && (
                      <button className="btn btn-secondary" onClick={resumeVoiceAnswer}>
                        <Mic size={15} /> Try microphone again
                      </button>
                    )}
                    {voiceStatus === "listening" && (
                      <span className="voice-processing">Microphone starts automatically when the question finishes. Your answer submits after 2 seconds of silence (maximum 2 minutes).</span>
                    )}
                    {voiceStatus === "ready" && (
                      <button className="btn btn-primary" onClick={resumeVoiceAnswer}>
                        <Mic size={15} /> Start speaking
                      </button>
                    )}
                    {voiceStatus === "processing" && (
                      <span className="voice-processing"><LoaderCircle className="spin" size={15} /> Processing your spoken answer…</span>
                    )}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <>
              {session?.mode === "HYBRID" && (
                <div className="question-tools">
                  <button
                    className="btn btn-secondary btn-small"
                    onClick={() => {
                      if (!("speechSynthesis" in window)) {
                        setError("Speech output is not supported in this browser.");
                        return;
                      }
                      window.speechSynthesis.cancel();
                      const utterance = new SpeechSynthesisUtterance(current?.questionText || "");
                      utterance.lang = "en-IN";
                      window.speechSynthesis.speak(utterance);
                    }}
                  >
                    <Volume2 size={15} /> Read question aloud
                  </button>
                  <span className="voice-note"><Mic size={14} /> You can type your answer in Hybrid mode.</span>
                </div>
              )}
              <form onSubmit={submitTypedAnswer} className={`answer-form ${isCodingQuestion ? "coding-answer-form" : ""}`}>
                {isCodingQuestion ? <>
                  <div className="coding-editor-heading"><div><span className="eyebrow"><Code2 size={14}/> LIVE CODING CHALLENGE</span><h3>Implement your solution</h3><p>Write readable code. Your submission will be included in the AI interview evaluation.</p></div><select value={codeLanguage} onChange={(event) => setCodeLanguage(event.target.value)} aria-label="Programming language"><option value="javascript">JavaScript</option><option value="python">Python 3</option><option value="java">Java 21</option><option value="cpp">C++17</option></select></div>
                  <div className="coding-editor-shell"><div className="coding-editor-bar"><span><i/> main.{codeLanguage === "javascript" ? "js" : codeLanguage === "python" ? "py" : codeLanguage === "java" ? "java" : "cpp"}</span><span>CODE EDITOR · LOCAL DRAFT</span></div><textarea id="answer" className="coding-editor" maxLength={10000} spellCheck="false" autoCapitalize="off" autoCorrect="off" value={codeDraft} onChange={(event) => setCodeDraft(event.target.value)} aria-label="Write code solution" required /></div>
                  <div className="coding-test-note"><ShieldCheck size={15}/> The AI interviewer will review your approach, correctness, complexity and edge cases. Automated execution requires a configured secure code runner.</div>
                </> : <>
                  <label htmlFor="answer">Your answer</label>
                  <textarea id="answer" rows="8" maxLength={10000} value={answer} onChange={(e) => setAnswer(e.target.value)} placeholder="Structure your answer clearly. Explain the concept, give an example, and mention trade-offs where relevant…" required />
                </>}
                <div className="answer-footer">
                  <span>{(isCodingQuestion ? codeDraft : answer).length.toLocaleString()} / 10,000 characters</span>
                  <button className="btn btn-primary" disabled={busy || !(isCodingQuestion ? codeDraft.trim() : answer.trim())}>
                    {busy ? <LoaderCircle className="spin" size={16} /> : <Send size={15} />}
                    Submit answer
                  </button>
                </div>
              </form>
            </>
          )}
        </section>
      )}

      {!finished && session?.status === "IN_PROGRESS" && (
        <IntegrityMonitor
          sessionId={sessionId}
          onWarning={setIntegrityWarning}
          onTerminated={handleIntegrityTermination}
        />
      )}

      <div className="interview-footnote">
        <Sparkles size={15} /> NEXORA adapts the next question based on your latest answer. Follow-ups may be generated before moving to another topic.
      </div>
    </div>
  );
}
