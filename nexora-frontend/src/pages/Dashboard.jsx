import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowUpRight, BrainCircuit, FileText, Target, Clock3, Sparkles, Plus,
  Activity, CircleCheck, Zap, CalendarDays, TrendingUp, Mic2, Code2
} from "lucide-react";
import api from "../api/axiosInstance";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../utils/errors";

const DAY_MS = 24 * 60 * 60 * 1000;

export default function Dashboard() {
  const { user } = useAuth();
  const [sessions, setSessions] = useState([]);
  const [configs, setConfigs] = useState([]);
  const [health, setHealth] = useState("checking");
  const [error, setError] = useState("");

  useEffect(() => {
    let alive = true;
    Promise.allSettled([
      api.get("/candidate/interview-sessions"),
      api.get("/candidate/interview-configurations"),
      api.get("/health")
    ]).then(([s, c, h]) => {
      if (!alive) return;
      if (s.status === "fulfilled") setSessions(Array.isArray(s.value.data) ? s.value.data : []);
      if (c.status === "fulfilled") setConfigs(Array.isArray(c.value.data) ? c.value.data : []);
      setHealth(h.status === "fulfilled" ? "online" : "offline");
      if (s.status === "rejected" && c.status === "rejected") setError(getErrorMessage(s.reason));
    });
    return () => { alive = false; };
  }, []);

  const completed = sessions.filter(x => x.status === "COMPLETED").length;
  const active = sessions.filter(x => x.status === "IN_PROGRESS").length;
  const completionRate = sessions.length ? Math.round((completed / sessions.length) * 100) : 0;
  const recent = [...sessions].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
  const weekly = useMemo(() => {
    const now = new Date();
    const start = new Date(now);
    start.setHours(0, 0, 0, 0);
    start.setDate(start.getDate() - 6);
    return Array.from({ length: 7 }, (_, i) => {
      const day = new Date(start.getTime() + i * DAY_MS);
      const key = day.toDateString();
      return {
        label: day.toLocaleDateString(undefined, { weekday: "short" }),
        count: sessions.filter(s => s.createdAt && new Date(s.createdAt).toDateString() === key).length
      };
    });
  }, [sessions]);
  const maxWeekly = Math.max(1, ...weekly.map(d => d.count));
  const modes = [
    { label: "Text", count: sessions.filter(s => String(s.mode).toUpperCase() === "TEXT").length, color: "#49c5d5" },
    { label: "Voice", count: sessions.filter(s => String(s.mode).toUpperCase() === "VOICE").length, color: "#8b7cf6" },
    { label: "Hybrid", count: sessions.filter(s => String(s.mode).toUpperCase() === "HYBRID").length, color: "#d2a5ff" }
  ];
  const modeTotal = modes.reduce((sum, item) => sum + item.count, 0);
  const skillNodes = [...new Set(configs.flatMap(config => {
    const values = Array.isArray(config.selectedSkills)
      ? config.selectedSkills
      : String(config.selectedSkills || "").split(",");
    return values.map(value => String(value).trim()).filter(Boolean);
  }))].slice(0, 12);
  const modeGradient = (() => {
    if (!modeTotal) return "#26364c";
    let cursor = 0;
    return `conic-gradient(${modes.map(item => {
      const start = cursor;
      cursor += (item.count / modeTotal) * 100;
      return `${item.color} ${start}% ${cursor}%`;
    }).join(", ")})`;
  })();

  return <div className="content-stack dashboard-page">
    <section className="welcome-banner">
      <div className="welcome-copy">
        <div className="eyebrow eyebrow-light"><Sparkles size={14}/> NEXORA INTELLIGENCE / YOUR WORKSPACE</div>
        <h1>Good to see you, {user?.fullName?.split(" ")[0] || "there"}.</h1>
        <p>Your next opportunity is built one focused practice session at a time. Turn preparation into measurable progress.</p>
        <div className="welcome-actions">
          <Link className="btn btn-white" to="/app/configurations"><Plus size={17}/> Start an interview</Link>
          <Link className="text-link-light" to="/app/resume">Explore resume intelligence <ArrowUpRight size={15}/></Link>
        </div>
      </div>
      <div className="welcome-art" aria-hidden="true">
        <div className="art-glow"/><div className="art-ring ring-a"/><div className="art-ring ring-b"/>
        <div className="art-ring ring-c"/><div className="art-core"><BrainCircuit size={54}/></div>
        <span className="art-spark spark-a">✦</span><span className="art-spark spark-b">✧</span>
        <span className="art-orbit-label">AI / ADAPTIVE</span>
      </div>
      <div className="welcome-index"><span>01</span><i/> PERSONAL PERFORMANCE HUB</div>
    </section>

    {error && <div className="notice notice-warn">{error}</div>}

    <div className="section-heading dashboard-section-heading">
      <div><div className="eyebrow">LIVE OVERVIEW</div><h2>Your preparation, at a glance</h2></div>
      <div className={`connection-pill ${health}`}><i/> Backend {health === "online" ? "connected" : health === "offline" ? "unavailable" : "checking"}</div>
    </div>

    <section className="stats-grid dashboard-stats">
      <div className="stat-card stat-card-featured">
        <div className="stat-card-top"><span className="stat-icon violet"><BrainCircuit size={19}/></span><span className="stat-trend"><Activity size={13}/> TOTAL</span></div>
        <div className="stat-value">{sessions.length}</div><div className="stat-label">Interview sessions</div><div className="stat-note">Your practice history</div>
        <div className="stat-sparkline" aria-hidden="true"><i/><i/><i/><i/><i/><i/><i/></div>
      </div>
      <div className="stat-card stat-card-ring">
        <div className="stat-card-top"><span className="stat-icon green"><CircleCheck size={19}/></span><span className="stat-trend positive">COMPLETION</span></div>
        <div className="stat-ring-wrap"><CircularProgress value={completionRate}/><div><div className="stat-value">{completed}</div><div className="stat-label">Completed</div></div></div>
        <div className="stat-note">{completionRate}% of all sessions finished</div>
      </div>
      <div className="stat-card">
        <div className="stat-card-top"><span className="stat-icon blue"><Activity size={19}/></span><span className="stat-trend">IN MOTION</span></div>
        <div className="stat-value">{active}</div><div className="stat-label">In progress</div><div className="stat-note">Sessions currently active</div>
        <div className="stat-mini-track"><span style={{ width: `${sessions.length ? Math.min(100, active / sessions.length * 100) : 0}%` }}/></div>
      </div>
      <div className="stat-card">
        <div className="stat-card-top"><span className="stat-icon amber"><Target size={19}/></span><span className="stat-trend">READY TO USE</span></div>
        <div className="stat-value">{configs.length}</div><div className="stat-label">Interview blueprints</div><div className="stat-note">Your saved practice setups</div>
        <div className="stat-mini-dots"><i/><i/><i/><i/><i/><i/><i/><i/><i/><i/></div>
      </div>
    </section>

    <section className="dashboard-analytics-grid">
      <article className="panel analytics-panel activity-panel">
        <div className="panel-heading analytics-heading">
          <div><div className="eyebrow">CONSISTENCY TRACKER</div><h3>Practice activity</h3><p>Interview sessions created over the last seven days</p></div>
          <span className="chart-icon"><TrendingUp size={17}/></span>
        </div>
        <div className="activity-chart">
          <div className="chart-y-labels"><span>{maxWeekly}</span><span>{Math.ceil(maxWeekly / 2)}</span><span>0</span></div>
          <div className="activity-chart-main">
            <div className="chart-grid-lines"><i/><i/><i/></div>
            <div className="bar-chart" role="img" aria-label={`Practice sessions in the last seven days: ${weekly.map(d => `${d.label} ${d.count}`).join(", ")}`}>
              {weekly.map((day, i) => <div className="bar-column" key={day.label}>
                <div className="bar-value">{day.count > 0 ? day.count : ""}</div>
                <div className={`bar-track ${day.count ? "has-value" : ""}`}><span style={{ height: `${day.count ? Math.max(8, day.count / maxWeekly * 100) : 3}%` }}/></div>
                <span className="bar-day">{day.label}</span>
              </div>)}
            </div>
          </div>
        </div>
        <div className="chart-footnote"><span><i className="legend-dot cyan"/> Sessions created</span><span>Last 7 days</span></div>
      </article>

      <article className="panel analytics-panel mode-panel">
        <div className="panel-heading analytics-heading">
          <div><div className="eyebrow">EXPERIENCE MIX</div><h3>Interview modes</h3><p>How you prefer to practise</p></div>
          <span className="chart-icon"><Zap size={17}/></span>
        </div>
        <div className="mode-chart-layout">
          <div className="mode-donut" style={{ background: modeGradient }} role="img" aria-label={`Interview modes: ${modes.map(m => `${m.label} ${m.count}`).join(", ")}`}>
            <div className="mode-donut-hole"><strong>{modeTotal}</strong><span>SESSIONS</span></div>
          </div>
          <div className="mode-legend">{modes.map(item => <div className="mode-legend-row" key={item.label}><i style={{ background: item.color }}/><span>{item.label}</span><strong>{item.count}</strong></div>)}</div>
        </div>
        {!modeTotal && <p className="chart-empty-hint">Your mode breakdown will appear after your first interview.</p>}
        <div className="chart-footnote"><span><i className="legend-dot violet"/> Session distribution</span><span>All time</span></div>
      </article>
    </section>

    <div className="dashboard-grid dashboard-lower-grid">
      <section className="panel recent-panel">
        <div className="panel-heading"><div><div className="eyebrow">YOUR TIMELINE</div><h3>Recent interviews</h3><p>Your latest practice sessions</p></div><Link to="/app/sessions" className="subtle-link">View all <ArrowUpRight size={14}/></Link></div>
        {recent.length === 0 ? <Empty icon={BrainCircuit} title="Your practice journey starts here" text="Create an interview configuration to begin your first adaptive session." action={<Link to="/app/configurations" className="btn btn-primary btn-small">Set up interview</Link>}/> :
          <div className="session-list">{recent.slice(0, 5).map(s => <div className="session-row" key={s.id}>
            <div className="session-icon"><BrainCircuit size={18}/></div>
            <div className="session-info"><strong>{s.targetRole || "Interview practice"}</strong><span>{s.interviewType} · {s.difficulty} · {s.mode}</span></div>
            <span className={`status-chip status-${String(s.status).toLowerCase()}`}>{String(s.status).replaceAll("_", " ")}</span>
            <span className="session-date">{s.createdAt ? new Date(s.createdAt).toLocaleDateString() : "—"}</span>
          </div>)}</div>}
      </section>
      <section className="panel quick-panel">
        <div className="panel-heading"><div><div className="eyebrow">NEXT BEST ACTION</div><h3>Keep your momentum</h3><p>Tools for your next step</p></div><span className="chart-icon"><Sparkles size={17}/></span></div>
        <Quick to="/app/resume" icon={FileText} title="Resume intelligence" text="Extract skills and experience"/>
        <Quick to="/app/profile" icon={Target} title="Candidate profile" text="Set your career preferences"/>
        <Quick to="/app/configurations" icon={Clock3} title="Interview studio" text="Build a tailored practice session"/>
        <div className="quick-panel-footer"><span><Mic2 size={14}/> TEXT · VOICE · HYBRID</span><span><Code2 size={14}/> AI-POWERED PRACTICE</span></div>
      </section>
    </div>

    <section className="constellation-panel">
      <div className="constellation-copy">
        <div className="eyebrow"><Sparkles size={13}/> SKILL INTELLIGENCE MAP</div>
        <h3>Your preparation universe</h3>
        <p>A live visual map of the skills in your saved interview blueprints. Use it to see the breadth of your preparation.</p>
        <div className="constellation-stats">
          <span><strong>{skillNodes.length}</strong><small>Distinct skills</small></span>
          <span><strong>{configs.length}</strong><small>Blueprints</small></span>
        </div>
        <Link to="/app/configurations" className="constellation-link">Expand your skill map <ArrowUpRight size={15}/></Link>
      </div>
      <SkillConstellation skills={skillNodes}/>
    </section>
    <div className="bottom-note"><Sparkles size={16}/><span><strong>Make progress measurable.</strong> Review your feedback after every interview and turn it into your next practice goal.</span></div>
  </div>;
}

function SkillConstellation({ skills }) {
  const points = skills.map((skill, index) => {
    const angle = (-90 + (360 / Math.max(skills.length, 1)) * index) * Math.PI / 180;
    const radius = index % 3 === 0 ? 116 : index % 3 === 1 ? 88 : 137;
    return {
      skill,
      x: 190 + Math.cos(angle) * radius,
      y: 155 + Math.sin(angle) * radius,
      index
    };
  });
  return <div className="constellation-visual" role="img" aria-label={`Skill constellation containing ${skills.length ? skills.join(", ") : "no configured skills yet"}`}>
    <div className="constellation-grid"/>
    <svg viewBox="0 0 380 310" aria-hidden="true">
      <defs>
        <linearGradient id="nx-constellation-line" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#43d9e6" stopOpacity=".75"/>
          <stop offset="100%" stopColor="#9b83ff" stopOpacity=".35"/>
        </linearGradient>
        <filter id="nx-node-glow"><feGaussianBlur stdDeviation="3" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
      </defs>
      <circle cx="190" cy="155" r="138" className="constellation-orbit orbit-outer"/>
      <circle cx="190" cy="155" r="96" className="constellation-orbit orbit-inner"/>
      {points.map(point => <g key={point.skill}>
        <line x1="190" y1="155" x2={point.x} y2={point.y} stroke="url(#nx-constellation-line)" strokeWidth="1.2" strokeDasharray="3 5"/>
        <circle cx={point.x} cy={point.y} r="5" className={`constellation-node node-${point.index % 4}`} filter="url(#nx-node-glow)"/>
        <text x={point.x} y={point.y - 12} textAnchor="middle" className="constellation-label">{point.skill.length > 16 ? `${point.skill.slice(0, 15)}…` : point.skill}</text>
      </g>)}
      <circle cx="190" cy="155" r="28" className="constellation-core-halo"/>
      <circle cx="190" cy="155" r="19" className="constellation-core"/>
      <text x="190" y="159" textAnchor="middle" className="constellation-core-label">NX</text>
    </svg>
    <div className="constellation-scan"/>
    {!skills.length && <div className="constellation-empty">Create an interview blueprint to populate your skill universe.</div>}
    <span className="constellation-coordinate coordinate-a">NEXORA / MAP</span>
    <span className="constellation-coordinate coordinate-b">SKILL GRID / LIVE</span>
  </div>;
}

function CircularProgress({ value }) {
  const radius = 24;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (Math.max(0, Math.min(100, value)) / 100) * circumference;
  return <div className="circular-progress" role="img" aria-label={`${value}% session completion`}>
    <svg viewBox="0 0 64 64"><circle className="progress-track" cx="32" cy="32" r={radius}/><circle className="progress-value" cx="32" cy="32" r={radius} strokeDasharray={circumference} strokeDashoffset={offset}/></svg>
    <span>{value}%</span>
  </div>;
}
function Quick({ to, icon: Icon, title, text }) {
  return <Link to={to} className="quick-action"><span className="quick-icon"><Icon size={18}/></span><span><strong>{title}</strong><small>{text}</small></span><ArrowUpRight size={16}/></Link>;
}
function Empty({ icon: Icon, title, text, action }) {
  return <div className="empty-state"><div className="empty-icon"><Icon size={24}/></div><strong>{title}</strong><p>{text}</p>{action}</div>;
}
