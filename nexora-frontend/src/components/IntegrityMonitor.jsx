import { useEffect, useRef, useState } from "react";
import { Camera, CameraOff, ShieldAlert, ShieldCheck, LoaderCircle, ChevronDown, ChevronUp } from "lucide-react";
import api from "../api/axiosInstance";

const MODEL_URL = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task";
const WASM_URL = "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm";
const OBJECT_MODEL_URLS = [
  "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite2/float32/1/efficientdet_lite2.tflite",
  "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float32/1/efficientdet_lite0.tflite"
];

/** Local-first proctoring. Frames are processed in-browser and are never uploaded. */
export default function IntegrityMonitor({ sessionId, onTerminated, onWarning }) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const landmarkerRef = useRef(null);
  const objectDetectorRef = useRef(null);
  const rafRef = useRef(0);
  const lastObjectCheckRef = useRef(0);
  const phoneDetectedStartedRef = useRef(0);
  const lastEventRef = useRef({});
  const gazeStartedRef = useRef(0);
  const faceMissingStartedRef = useRef(0);
  const terminatedRef = useRef(false);
  const [status, setStatus] = useState("starting");
  const [message, setMessage] = useState("Requesting camera access…");
  const [warning, setWarning] = useState("");
  const [attention, setAttention] = useState("Checking attention…");
  const attentionRef = useRef("");
  const [cameraEnabled, setCameraEnabled] = useState(false);
  const [phoneDetectorStatus, setPhoneDetectorStatus] = useState("loading");
  const [phoneDetectorDetail, setPhoneDetectorDetail] = useState("Loading phone-detection model…");
  const [phoneConfidence, setPhoneConfidence] = useState(0);
  const phoneWarningActiveRef = useRef(false);
  const [expanded, setExpanded] = useState(true);
  const onTerminatedRef = useRef(onTerminated);
  const onWarningRef = useRef(onWarning);
  onTerminatedRef.current = onTerminated;
  onWarningRef.current = onWarning;

  useEffect(() => {
    let active = true;
    const record = async (eventType, severity, details) => {
      if (terminatedRef.current) return;
      const now = Date.now();
      if (now - (lastEventRef.current[eventType] || 0) < 3000) return;
      lastEventRef.current[eventType] = now;
      try {
        const { data } = await api.post(`/candidate/interview-sessions/${sessionId}/integrity-events`, { eventType, severity, details });
        if (data?.terminated && !terminatedRef.current) {
          terminatedRef.current = true;
          setStatus("terminated");
          setMessage("Interview ended and flagged for integrity review.");
          onTerminatedRef.current?.(data);
        }
      } catch (error) {
        console.warn("NEXORA integrity event could not be recorded", error);
      }
    };

    const start = async () => {
      try {
        if (!navigator.mediaDevices?.getUserMedia) throw new Error("Camera access requires HTTPS or localhost.");
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user", width: { ideal: 640 }, height: { ideal: 480 } }, audio: false });
        if (!active) { stream.getTracks().forEach(t => t.stop()); return; }
        streamRef.current = stream;
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
        setCameraEnabled(true);
        setMessage("Camera active. Face and approximate gaze are monitored locally.");
        const vision = await import("@mediapipe/tasks-vision");
        const fileset = await vision.FilesetResolver.forVisionTasks(WASM_URL);
        const landmarker = await vision.FaceLandmarker.createFromOptions(fileset, {
          baseOptions: { modelAssetPath: MODEL_URL, delegate: "CPU" },
          runningMode: "VIDEO", numFaces: 2,
          outputFaceBlendshapes: false, outputFacialTransformationMatrixes: true
        });
        if (!active) { landmarker.close(); return; }
        landmarkerRef.current = landmarker;
        // Load the higher-accuracy COCO object detector first, then fall back to
        // Lite0. Detector status is shown separately from camera status so the UI
        // never implies phone detection is active when model loading failed.
        let phoneDetectionAvailable = false;
        let phoneLoadError = null;
        setPhoneDetectorStatus("loading");
        setPhoneDetectorDetail("Downloading phone-detection model…");
        for (const modelUrl of OBJECT_MODEL_URLS) {
          try {
            objectDetectorRef.current = await vision.ObjectDetector.createFromOptions(fileset, {
              baseOptions: { modelAssetPath: modelUrl, delegate: "CPU" },
              runningMode: "VIDEO", scoreThreshold: 0.30, maxResults: 20
            });
            phoneDetectionAvailable = true;
            break;
          } catch (objectError) {
            phoneLoadError = objectError;
            console.warn("Phone detector model attempt failed:", modelUrl, objectError);
            objectDetectorRef.current?.close();
            objectDetectorRef.current = null;
          }
        }
        setPhoneDetectorStatus(phoneDetectionAvailable ? "active" : "unavailable");
        setPhoneDetectorDetail(phoneDetectionAvailable
          ? "Phone detection active · local processing"
          : `Phone detection unavailable: ${phoneLoadError?.message || "model could not load"}`);
        setStatus("monitoring");
        setMessage(phoneDetectionAvailable
          ? "Camera active. Face, approximate gaze and phone-object checks are running locally."
          : "Camera active, but phone detection is unavailable. Check model/network access.");
        const detect = () => {
          if (!active || terminatedRef.current || !videoRef.current || !landmarkerRef.current) return;
          const video = videoRef.current;
          if (video.readyState >= 2) {
            const result = landmarkerRef.current.detectForVideo(video, performance.now());
            const faces = result.faceLandmarks || [];
            const now = Date.now();
            if (objectDetectorRef.current && now - lastObjectCheckRef.current >= 500) {
              lastObjectCheckRef.current = now;
              try {
                const objects = objectDetectorRef.current.detectForVideo(video, performance.now());
                const phoneMatches = (objects.detections || []).flatMap(detection =>
                  (detection.categories || [])
                    .filter(category => /cell\s*phone|mobile\s*phone|smartphone|phone/i.test(category.categoryName || ""))
                    .map(category => category.score || 0)
                );
                const bestPhoneScore = phoneMatches.length ? Math.max(...phoneMatches) : 0;
                setPhoneConfidence(bestPhoneScore);
                const phoneFound = bestPhoneScore >= 0.30;
                if (phoneFound) {
                  if (!phoneDetectedStartedRef.current) phoneDetectedStartedRef.current = now;
                  if (now - phoneDetectedStartedRef.current >= 1000) {
                    const warningText = "INTEGRITY WARNING: A possible mobile phone was detected in the camera frame. Please keep phones away during the interview.";
                    if (!phoneWarningActiveRef.current) {
                      phoneWarningActiveRef.current = true;
                      setWarning(warningText);
                      onWarningRef.current?.(warningText);
                    }
                    record("PHONE_DETECTED", "HIGH", `Local object detector classified a visible object as a cell phone (confidence ${(bestPhoneScore * 100).toFixed(0)}%). Automated detection may be incorrect.`);
                  }
                } else {
                  phoneDetectedStartedRef.current = 0;
                  phoneWarningActiveRef.current = false;
                  setPhoneConfidence(0);
                }
              } catch (objectDetectionError) {
                console.warn("Phone detection frame failed", objectDetectionError);
              }
            }
            if (faces.length === 0) {
              if (attentionRef.current !== "FACE_NOT_VISIBLE") { attentionRef.current = "FACE_NOT_VISIBLE"; setAttention("Face not visible"); }
              gazeStartedRef.current = 0;
              if (!faceMissingStartedRef.current) faceMissingStartedRef.current = now;
              if (now - faceMissingStartedRef.current > 3000) {
                const warningText = "CAMERA WARNING: Your face is not visible. Please return to the camera view.";
                setWarning(warningText);
                onWarningRef.current?.(warningText);
                record("FACE_NOT_VISIBLE", "WARNING", "No face detected for more than three seconds.");
              }
            } else {
              faceMissingStartedRef.current = 0;
              if (faces.length > 1) {
                const warningText = "INTEGRITY WARNING: More than one face is visible. Please ensure you are alone for the interview.";
                setWarning(warningText);
                onWarningRef.current?.(warningText);
                record("MULTIPLE_FACES", "WARNING", `${faces.length} faces detected in the camera frame.`);
              } else {
                const points = faces[0];
                // Approximate iris position relative to eye corners. This is a screening
                // heuristic, not proof of looking at notes or copying.
                const leftIris = points[468], rightIris = points[473];
                const leftOuter = points[33], leftInner = points[133];
                const rightInner = points[362], rightOuter = points[263];
                const leftRatio = leftIris && leftOuter && leftInner
                  ? Math.abs(leftIris.x - leftOuter.x) / Math.max(0.001, Math.abs(leftInner.x - leftOuter.x)) : 0.5;
                const rightRatio = rightIris && rightInner && rightOuter
                  ? Math.abs(rightIris.x - rightInner.x) / Math.max(0.001, Math.abs(rightOuter.x - rightInner.x)) : 0.5;
                const horizontalRatio = (leftRatio + rightRatio) / 2;
                const leftUpper = points[159], leftLower = points[145];
                const rightUpper = points[386], rightLower = points[374];
                const leftVertical = leftIris && leftUpper && leftLower
                  ? Math.abs(leftIris.y - leftUpper.y) / Math.max(0.001, Math.abs(leftLower.y - leftUpper.y)) : 0.5;
                const rightVertical = rightIris && rightUpper && rightLower
                  ? Math.abs(rightIris.y - rightUpper.y) / Math.max(0.001, Math.abs(rightLower.y - rightUpper.y)) : 0.5;
                const verticalRatio = (leftVertical + rightVertical) / 2;
                const xs = points.map(point => point.x);
                const minX = Math.min(...xs), maxX = Math.max(...xs);
                const nose = points[1];
                const headCenterRatio = nose ? (nose.x - minX) / Math.max(0.001, maxX - minX) : 0.5;
                const eyesAway = horizontalRatio < 0.18 || horizontalRatio > 0.82 || verticalRatio < 0.12 || verticalRatio > 0.88;
                const headTurned = headCenterRatio < 0.27 || headCenterRatio > 0.73;
                const lookingAway = eyesAway || headTurned;
                const nextAttention = lookingAway ? "LOOKING_AWAY" : "LOOKING_AT_SCREEN";
                if (attentionRef.current !== nextAttention) {
                  attentionRef.current = nextAttention;
                  setAttention(lookingAway ? "Looking away — return your attention to the screen" : "Face visible · attention toward screen");
                }
                if (lookingAway) {
                  if (!gazeStartedRef.current) gazeStartedRef.current = now;
                  if (now - gazeStartedRef.current > 3000) {
                    const warningText = "INTEGRITY WARNING: Please look toward your screen. Continued looking away may end the interview.";
                    setWarning(warningText);
                    onWarningRef.current?.(warningText);
                    record("GAZE_AWAY", "WARNING", "Approximate eye-gaze or head-orientation signal remained outside the on-screen range for more than three seconds.");
                  }
                } else {
                  gazeStartedRef.current = 0;
                  setWarning("");
                  onWarningRef.current?.("");
                }
              }
            }
          }
          rafRef.current = requestAnimationFrame(detect);
        };
        rafRef.current = requestAnimationFrame(detect);
      } catch (error) {
        if (!active) return;
        setStatus("error");
        setMessage(error?.name === "NotAllowedError" ? "Camera permission was denied. Allow camera access in Chrome site settings." : error.message || "Camera monitoring could not start.");
      }
    };
    start();
    return () => {
      active = false;
      cancelAnimationFrame(rafRef.current);
      landmarkerRef.current?.close();
      landmarkerRef.current = null;
      objectDetectorRef.current?.close();
      objectDetectorRef.current = null;
      streamRef.current?.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    };
  }, [sessionId]);

  return (
    <aside className={`integrity-widget ${expanded ? "expanded" : "collapsed"}`} aria-label="Interview camera and integrity monitor">
      <div className="integrity-widget-top">
        <div className="integrity-icon">{status === "monitoring" ? <ShieldCheck size={18}/> : status === "error" ? <CameraOff size={18}/> : <Camera size={18}/>}</div>
        <div className="integrity-widget-title"><strong>Camera monitor</strong><span className={`integrity-status ${status}`}>{status === "monitoring" ? "ACTIVE" : status.toUpperCase()}</span></div>
        <button type="button" className="integrity-collapse" onClick={() => setExpanded(value => !value)} aria-label={expanded ? "Minimize camera widget" : "Expand camera widget"}>{expanded ? <ChevronDown size={17}/> : <ChevronUp size={17}/>}</button>
      </div>
      <div className={`integrity-preview-wrap ${expanded ? "" : "preview-collapsed"}`}>
        {/* Keep the video node mounted while collapsed. Removing it detaches the live stream
            and previously left the reopened widget showing a dead camera preview. */}
        <video ref={videoRef} className="integrity-preview" muted playsInline />
        {!cameraEnabled && <div className="integrity-preview-placeholder">{status === "starting" ? <LoaderCircle className="spin"/> : <CameraOff/>}</div>}
        <span className="integrity-live-label">● LIVE</span>
      </div>
      {expanded && <>
        <div className="integrity-attention-state">{attention}</div>
        <p className="integrity-widget-message">{message}</p>
        <div className={`integrity-phone-status ${phoneDetectorStatus}`} role="status">
          <span className="integrity-phone-status-dot" />
          <span>{phoneDetectorDetail}</span>
          {phoneConfidence > 0 && <strong>{Math.round(phoneConfidence * 100)}%</strong>}
        </div>
        <small className="integrity-privacy-note">Local camera processing. No video recording. Phone detection depends on lighting, size, angle and visibility; it cannot detect phones outside the frame.</small>
      </>}
    </aside>
  );
}
