import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Sparkles, Trophy, CheckCircle2, AlertTriangle, LoaderCircle, RefreshCw } from "lucide-react";
import api from "../api/axiosInstance";
import { getErrorMessage } from "../utils/errors";

export default function Evaluation() {
  const { sessionId } = useParams();
  const [data, setData] = useState(null);
  const [session, setSession] = useState(null);
  const [integrityEvents, setIntegrityEvents] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const sessionResponse = await api.get(`/candidate/interview-sessions/${sessionId}`);
      setSession(sessionResponse.data);
      const [evaluationResult, integrityResult] = await Promise.allSettled([
        api.get(`/candidate/interview-sessions/${sessionId}/evaluation`),
        api.get(`/candidate/interview-sessions/${sessionId}/integrity-events`)
      ]);
      if (evaluationResult.status === "fulfilled") setData(evaluationResult.value.data);
      else if (evaluationResult.reason?.response?.status !== 404) throw evaluationResult.reason;
      else setData(null);
      setIntegrityEvents(integrityResult.status === "fulfilled" && Array.isArray(integrityResult.value.data) ? integrityResult.value.data : []);
    } catch (e) { setError(getErrorMessage(e)); }
    finally { setLoading(false); }
  }, [sessionId]);

  useEffect(() => { load(); }, [load]);

  const generate = async () => {
    if (generating) return;
    setGenerating(true); setError("");
    try { await api.post(`/candidate/interview-sessions/${sessionId}/evaluate`); await load(); }
    catch (e) { setError(getErrorMessage(e)); }
    finally { setGenerating(false); }
  };

  if (loading) return <div className="loading-page"><LoaderCircle className="spin"/> Loading performance report…</div>;
  if (error && !session) return <div className="content-stack"><div className="form-error">{error}</div><Link className="btn btn-secondary" to="/app/sessions">Back to interviews</Link></div>;
  if (data?.status === "NOT_GENERATED") return <div className="content-stack evaluation-empty-state">
    <div className="page-intro-row"><div className="page-intro"><div className="eyebrow">INTERVIEW OUTCOME</div><h1>Interview submitted · Evaluation unavailable</h1><p>Your interview was safely recorded. An AI performance score was not generated because no answer was submitted.</p></div><Link to="/app/sessions" className="btn btn-secondary"><ArrowLeft size={16}/> Interview history</Link></div>
    <section className="panel" style={{ borderColor: "rgba(245,158,11,.35)", background: "linear-gradient(135deg, rgba(245,158,11,.10), rgba(255,255,255,.02))" }}>
      <div className="feedback-icon amber"><AlertTriangle size={20}/></div>
      <div className="eyebrow">WHY THERE IS NO SCORE</div>
      <h2>{session?.terminationReason ? session.terminationReason.replaceAll("_", " ") : "No answers submitted"}</h2>
      <p>{session?.terminationDetails || data.overallFeedback || data.message || "The interview ended before a scorable answer was submitted."}</p>
      <p className="field-help">No score or performance rating has been assigned. This outcome is not treated as a zero score or a failed interview assessment.</p>
    </section>
    <section className="panel"><div className="panel-heading"><div><h3>What happens next?</h3><p>You can start another practice session whenever you're ready.</p></div></div><div className="feedback-grid"><Feedback icon={CheckCircle2} title="Your session is saved" text="The interview outcome and recorded integrity events remain in your history." tone="green"/><Feedback icon={AlertTriangle} title="No performance score" text="AI evaluation requires at least one successfully submitted answer." tone="amber"/><Feedback icon={Sparkles} title="Try another interview" text="Return to Interview Studio and start a new session." tone="violet"/></div><Link to="/app/configurations" className="btn btn-primary">Start a new interview</Link></section>
    {integrityEvents.length > 0 && <section className="panel integrity-report"><div className="panel-heading"><div><h3>Recorded integrity events</h3><p>These explain which automated observations were recorded before submission.</p></div><span className="count-badge">{integrityEvents.length} events</span></div><div className="integrity-event-list">{integrityEvents.map(event => <article key={event.id}><strong>{event.eventType.replaceAll("_", " ")}</strong><span>{event.severity}</span><p>{event.details}</p><small>{new Date(event.occurredAt).toLocaleString()}</small></article>)}</div><p className="field-help">Automated observations are signals for review, not definitive proof of misconduct.</p></section>}
  </div>;

  if (data && data.status !== "COMPLETED") return <div className="content-stack evaluation-empty-state"><div className="page-intro"><div className="eyebrow">PERFORMANCE REPORT</div><h1>{data.status === "IN_PROGRESS" ? "Evaluation is being prepared" : data.status === "PENDING" ? "Your evaluation is ready to generate" : "Evaluation needs another attempt"}</h1><p>{data.status === "IN_PROGRESS" ? "Your answers are being evaluated. Refresh in a moment to check the report." : data.status === "PENDING" ? "Your submitted answers are saved. Generate the AI evaluation when you're ready." : "The previous evaluation did not finish successfully. You can retry it."}</p></div>{error && <div className="form-error">{error}</div>}<div className="panel"><Trophy size={28}/><h3>{session?.targetRole || "Interview practice"}</h3><p>Evaluation status: {data.status}</p><button className="btn btn-primary" onClick={generate} disabled={generating || data.status === "IN_PROGRESS" || session?.status !== "COMPLETED"}>{generating ? <LoaderCircle className="spin" size={16}/> : <RefreshCw size={16}/>} {generating ? "Generating evaluation…" : data.status === "PENDING" ? "Generate evaluation" : "Retry evaluation"}</button><button className="btn btn-secondary" onClick={load} disabled={loading}><RefreshCw size={15}/> Refresh status</button></div><Link to="/app/sessions" className="btn btn-secondary">Back to interviews</Link></div>;

  if (!data) return <div className="content-stack evaluation-empty-state"><div className="page-intro"><div className="eyebrow">PERFORMANCE REPORT</div><h1>Your evaluation isn't ready yet</h1><p>{session?.status === "CANCELLED" ? "Cancelled sessions cannot be evaluated." : "This session does not have a saved evaluation yet. If you have submitted answers, you can request the evaluation now."}</p></div>{error && <div className="form-error">{error}</div>}<div className="panel"><Trophy size={28}/><h3>{session?.targetRole || "Interview practice"}</h3><p>Session #{sessionId} · {session?.status || "Unknown status"}</p><button className="btn btn-primary" onClick={generate} disabled={generating || session?.status === "CANCELLED" || session?.status === "CREATED"}>{generating ? <LoaderCircle className="spin" size={16}/> : <RefreshCw size={16}/>} {generating ? "Generating evaluation…" : "Generate evaluation"}</button></div><Link to="/app/sessions" className="btn btn-secondary">Back to interviews</Link></div>;

  const score = Number(data?.overallScore || 0);
  return <div className="content-stack">
    <div className="page-intro-row"><div className="page-intro"><div className="eyebrow">YOUR PERFORMANCE REPORT</div><h1>Interview evaluation</h1><p>Review the feedback from your adaptive practice session. Treat AI feedback as guidance, not a hiring decision.</p></div><Link to="/app/sessions" className="btn btn-secondary"><ArrowLeft size={16}/> Interview history</Link></div>
    {error && <div className="form-error">{error}</div>}
    <section className="score-hero"><div className="score-medallion"><Trophy size={23}/><strong>{score.toFixed(1)}</strong><span>OUT OF 100</span></div><div className="score-copy"><div className="eyebrow eyebrow-light">OVERALL PERFORMANCE · {data.overallRating}</div><h2>{data.overallRating?.replaceAll("_", " ") || "Evaluation complete"}</h2><p>{data.overallFeedback}</p></div></section>
    {session?.terminationReason && <section className="panel termination-summary"><div className="eyebrow">SESSION OUTCOME</div><h3>{session.terminationReason.replaceAll("_", " ")}</h3><p>{session.terminationDetails || "The interview ended."}</p><p className="field-help">{session.resumeBased ? "This interview included questions based on your analyzed resume." : "This interview used your configured skills."}</p></section>}
    <div className="feedback-grid"><Feedback icon={CheckCircle2} title="Strengths" text={data.strengths} tone="green"/><Feedback icon={AlertTriangle} title="Areas to improve" text={data.improvementAreas} tone="amber"/><Feedback icon={Sparkles} title="Recommendations" text={data.recommendations} tone="violet"/></div>
    <section className="panel"><div className="panel-heading"><div><h3>Question-wise feedback</h3><p>Only submitted answers are included in this evaluation</p></div><span className="count-badge">{data.questionEvaluations?.length || 0} evaluated</span></div><div className="evaluation-list">{(data.questionEvaluations || []).map((q, i) => <article className="evaluation-row" key={q.id || i}><div className="evaluation-number">{i + 1}</div><div className="evaluation-detail"><strong>Question {q.questionId || i + 1}</strong><span>{q.rating || "Evaluated"}</span>{q.feedback && <p>{q.feedback}</p>}{q.strengths && <small><b>Strength:</b> {q.strengths}</small>}{q.improvementAreas && <small><b>Improve:</b> {q.improvementAreas}</small>}</div><div className="evaluation-score">{Number(q.score || 0).toFixed(0)}<small>/100</small></div></article>)}</div></section>
    {integrityEvents.length > 0 && <section className="panel integrity-report"><div className="panel-heading"><div><h3>Integrity event history</h3><p>Automated camera observations recorded during this session</p></div><span className="count-badge">{integrityEvents.length} events</span></div><div className="integrity-event-list">{integrityEvents.map(event => <article key={event.id}><strong>{event.eventType.replaceAll("_", " ")}</strong><span>{event.severity}</span><p>{event.details}</p><small>{new Date(event.occurredAt).toLocaleString()}</small></article>)}</div><p className="field-help">These automated observations are signals for human review, not definitive proof of misconduct.</p></section>}
  </div>;
}
function Feedback({ icon: Icon, title, text, tone }) { return <section className="panel feedback-card"><div className={`feedback-icon ${tone}`}><Icon size={18}/></div><h3>{title}</h3><p>{text || "No feedback available."}</p></section>; }
