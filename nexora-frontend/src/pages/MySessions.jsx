import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Activity, ArrowRight, ArrowUpRight, Ban, BrainCircuit, CalendarDays,
  Check, CircleCheck, Clock3, Filter, History, LoaderCircle, Plus,
  Search, ShieldCheck, Sparkles, X
} from "lucide-react";
import api from "../api/axiosInstance";
import { getErrorMessage } from "../utils/errors";
import { useToast } from "../components/Toast";

const FILTERS = ["ALL", "IN_PROGRESS", "CREATED", "COMPLETED", "CANCELLED"];
const label = (value = "") => value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
const dateLabel = value => value ? new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "Date unavailable";

export default function MySessions() {
  const navigate = useNavigate();
  const notify = useToast();
  const [sessions, setSessions] = useState([]);
  const [filter, setFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [clearOpen, setClearOpen] = useState(false);
  const [clearing, setClearing] = useState(false);

  const loadSessions = useCallback(async () => {
    setError("");
    try {
      const { data } = await api.get("/candidate/interview-sessions");
      setSessions(Array.isArray(data) ? data : []);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadSessions(); }, [loadSessions]);

  const counts = useMemo(() => FILTERS.reduce((acc, key) => {
    acc[key] = key === "ALL" ? sessions.length : sessions.filter(s => s.status === key).length;
    return acc;
  }, {}), [sessions]);

  const visible = useMemo(() => sessions.filter(s => {
    const matchesFilter = filter === "ALL" || s.status === filter;
    const haystack = `${s.targetRole || ""} ${s.interviewType || ""} ${s.mode || ""} ${(s.selectedSkills || []).join(" ")}`.toLowerCase();
    return matchesFilter && haystack.includes(query.toLowerCase().trim());
  }), [sessions, filter, query]);

  const cancelSession = async () => {
    if (!cancelTarget || cancelling) return;
    setCancelling(true);
    try {
      const { data } = await api.patch(`/candidate/interview-sessions/${cancelTarget.id}/cancel`);
      setSessions(current => current.map(s => s.id === cancelTarget.id ? { ...s, ...data } : s));
      setCancelTarget(null);
      notify("Interview session cancelled. It will remain in your history.");
    } catch (e) {
      notify(getErrorMessage(e), "error");
    } finally {
      setCancelling(false);
    }
  };

  const clearHistory = async () => {
    if (clearing) return;
    setClearing(true);
    try {
      const { data } = await api.delete("/candidate/interview-sessions/history");
      setSessions([]); setFilter("ALL"); setQuery(""); setClearOpen(false);
      notify(`${data?.deletedCount ?? 0} interview sessions permanently deleted.`);
    } catch (e) { notify(getErrorMessage(e), "error"); }
    finally { setClearing(false); }
  };

  const activeCount = counts.IN_PROGRESS + counts.CREATED;
  return (
    <div className="content-stack sessions-page">
      <section className="sessions-hero">
        <div className="sessions-hero-copy">
          <div className="eyebrow eyebrow-light"><Sparkles size={14}/> YOUR INTERVIEW SPACE</div>
          <h1>Every session moves you forward.</h1>
          <p>Pick up where you left off, review completed practice, or close sessions you no longer need.</p>
          <Link to="/app/configurations" className="btn btn-white"><Plus size={17}/> Create interview <ArrowUpRight size={15}/></Link>
        </div>
        <div className="sessions-hero-orbit" aria-hidden="true">
          <div className="sessions-orbit orbit-one"/><div className="sessions-orbit orbit-two"/>
          <div className="sessions-orbit-core"><BrainCircuit size={38}/></div>
          <span className="orbit-spark spark-one">✦</span><span className="orbit-spark spark-two">✧</span>
        </div>
      </section>

      <section className="session-metrics">
        <Metric icon={History} title="All sessions" value={counts.ALL} tone="violet"/>
        <Metric icon={Activity} title="Active now" value={counts.IN_PROGRESS} tone="blue"/>
        <Metric icon={Clock3} title="Not started" value={counts.CREATED} tone="amber"/>
        <Metric icon={CircleCheck} title="Completed" value={counts.COMPLETED} tone="green"/>
      </section>

      <section className="sessions-toolbar-panel">
        <div className="sessions-toolbar-top">
          <div><div className="eyebrow">SESSION LIBRARY</div><h2>Your interviews</h2><p>{activeCount ? `${activeCount} session${activeCount === 1 ? "" : "s"} waiting for your attention` : "Your complete practice history, all in one place"}</p></div>
          <div className="sessions-toolbar-actions">{sessions.length > 0 && <button className="btn btn-danger btn-small" onClick={() => setClearOpen(true)}><X size={14}/> Clear history</button>}
          <div className="sessions-search"><Search size={17}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search role, skill or mode…" aria-label="Search sessions"/><Filter size={15}/></div></div>
        </div>
        <div className="session-filter-tabs" role="tablist" aria-label="Filter sessions">
          {FILTERS.map(key => <button key={key} role="tab" aria-selected={filter === key} className={filter === key ? "selected" : ""} onClick={() => setFilter(key)}>{label(key)}<span>{counts[key]}</span></button>)}
        </div>
      </section>

      {error && <div className="notice notice-warn session-error">{error} <button onClick={loadSessions}>Retry</button></div>}
      {loading ? <div className="sessions-loading"><LoaderCircle className="spin" size={25}/><span>Gathering your sessions…</span></div> : visible.length ? (
        <div className="session-card-grid">
          {visible.map((session, index) => <SessionCard key={session.id} session={session} index={index} onCancel={() => setCancelTarget(session)} onOpen={() => navigate(session.status === "COMPLETED" ? `/app/evaluation/${session.id}` : `/app/interview/${session.id}`)}/>)}
        </div>
      ) : (
        <div className="sessions-empty"><div className="sessions-empty-icon"><BrainCircuit size={28}/></div><h3>{query || filter !== "ALL" ? "No matching sessions" : "Your next chapter starts here"}</h3><p>{query || filter !== "ALL" ? "Try another search or choose a different status filter." : "Create your first interview and start building confidence, one answer at a time."}</p>{query || filter !== "ALL" ? <button className="btn btn-soft" onClick={() => { setQuery(""); setFilter("ALL"); }}>Clear filters</button> : <Link className="btn btn-primary" to="/app/configurations"><Plus size={16}/> Set up an interview</Link>}</div>
      )}

      <div className="sessions-footnote"><ShieldCheck size={16}/><span><strong>Your history stays yours.</strong> Cancelling a session closes it permanently but keeps its record visible here.</span></div>

      {clearOpen && <div className="session-modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget && !clearing) setClearOpen(false); }}>
        <section className="session-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="clear-history-title">
          <button className="session-modal-close" onClick={() => setClearOpen(false)} disabled={clearing} aria-label="Close"><X size={18}/></button>
          <div className="session-modal-icon danger"><History size={23}/></div><div className="eyebrow">PERMANENT ACTION</div>
          <h2 id="clear-history-title">Clear your interview history?</h2>
          <p>This permanently deletes all {sessions.length} interview sessions, their answers, evaluations and integrity-event records. This action cannot be undone.</p>
          <div className="session-modal-actions"><button className="btn btn-soft" onClick={() => setClearOpen(false)} disabled={clearing}>Keep history</button><button className="btn btn-danger" onClick={clearHistory} disabled={clearing}>{clearing ? <LoaderCircle size={16} className="spin"/> : <X size={16}/>} {clearing ? "Deleting…" : "Permanently clear"}</button></div>
        </section>
      </div>}

      {cancelTarget && <div className="session-modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget && !cancelling) setCancelTarget(null); }}>
        <section className="session-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="cancel-title">
          <button className="session-modal-close" onClick={() => setCancelTarget(null)} disabled={cancelling} aria-label="Close"><X size={18}/></button>
          <div className="session-modal-icon"><Ban size={23}/></div>
          <div className="eyebrow">CLOSE INTERVIEW</div>
          <h2 id="cancel-title">End this session?</h2>
          <p><strong>{cancelTarget.targetRole || "Interview practice"}</strong> will be marked as cancelled. You won't be able to resume it, and its existing answers won't be evaluated automatically.</p>
          <div className="session-modal-summary"><span>Session #{cancelTarget.id}</span><span className={`status-chip status-${String(cancelTarget.status).toLowerCase()}`}>{label(cancelTarget.status)}</span></div>
          <div className="session-modal-actions"><button className="btn btn-soft" onClick={() => setCancelTarget(null)} disabled={cancelling}>Keep session</button><button className="btn btn-danger" onClick={cancelSession} disabled={cancelling}>{cancelling ? <LoaderCircle size={16} className="spin"/> : <Ban size={16}/>} {cancelling ? "Cancelling…" : "Yes, cancel session"}</button></div>
        </section>
      </div>}
    </div>
  );
}

function Metric({ icon: Icon, title, value, tone }) {
  return <article className="session-metric-card"><span className={`session-metric-icon ${tone}`}><Icon size={19}/></span><div><strong>{value}</strong><span>{title}</span></div></article>;
}

function SessionCard({ session, index, onCancel, onOpen }) {
  const status = session.status || "CREATED";
  const active = status === "IN_PROGRESS";
  const canCancel = status === "IN_PROGRESS" || status === "CREATED";
  const skills = (session.selectedSkills || []).slice(0, 3);
  return <article className="session-library-card" style={{ "--card-order": index }}>
    <div className="session-card-top"><div className="session-card-symbol"><BrainCircuit size={20}/></div><span className={`session-card-status ${status.toLowerCase()}`}><i/>{label(status)}</span></div>
    <h3>{session.targetRole || "Interview practice"}</h3>
    <div className="session-card-subtitle">{label(session.interviewType)} <span>·</span> {label(session.difficulty)} <span>·</span> {label(session.mode)}</div>
    <div className="session-card-tags">{session.resumeBased && <span className="session-tag resume-tag"><Sparkles size={11}/> Resume based</span>}{skills.map(skill => <span className="session-tag" key={skill}>{skill}</span>)}{(session.selectedSkills || []).length > 3 && <span className="session-tag">+{session.selectedSkills.length - 3}</span>}</div>
    <div className="session-card-meta"><span><CalendarDays size={14}/>{dateLabel(session.createdAt)}</span><span><Clock3 size={14}/>{session.durationMinutes || "—"} min</span><span><MessageCount count={session.numberOfQuestions}/></span></div>
    {session.terminationReason && status !== "IN_PROGRESS" && <div className="session-termination-note">{label(session.terminationReason)}</div>}
    <div className="session-card-actions">{status === "CANCELLED" ? <button className="btn btn-soft" disabled>Session cancelled</button> : <button className={`btn ${active || status === "CREATED" ? "btn-primary" : "btn-soft"}`} onClick={onOpen}>{active ? "Continue interview" : status === "CREATED" ? "Start interview" : "View evaluation"}<ArrowRight size={15}/></button>}{canCancel && <button className="session-icon-action" onClick={onCancel} title="Cancel session" aria-label={`Cancel ${session.targetRole || "interview"}`}><Ban size={17}/></button>}</div>
  </article>;
}

function MessageCount({ count }) { return <><Check size={13}/>{count || "—"} questions</>; }
