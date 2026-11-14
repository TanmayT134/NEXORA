import { useEffect, useRef, useState } from "react";
import { Bot, X, Send, Sparkles, LoaderCircle, ArrowRight, MessageCircle } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import api from "../api/axiosInstance";

const getSuggestedPrompts = (pathname) => {
  if (pathname.includes("/resume")) return [
    "How can I improve my resume score?",
    "Generate interview questions from my resume",
    "What skills should I strengthen next?",
  ];
  if (pathname.includes("/sessions")) return [
    "Explain my latest interview performance",
    "Help me prepare for my next interview",
    "Show me how to improve my weak areas",
  ];
  if (pathname.includes("/evaluation")) return [
    "Explain my weakest evaluation area",
    "Create a focused improvement plan",
    "Give me practice questions for my mistakes",
  ];
  if (pathname.includes("/configurations")) return [
    "Help me configure a realistic interview",
    "Which skills should I practice?",
    "Explain resume-based vs skill-based interviews",
  ];
  if (pathname.includes("/profile")) return [
    "How should I complete my candidate profile?",
    "Help me plan my preparation schedule",
    "Take me to resume intelligence",
  ];
  return [
    "Create a 7-day interview preparation plan",
    "Generate questions for my target role",
    "How can I improve my interview performance?",
  ];
};

export default function NexoraCopilot() {
  const location = useLocation();
  const navigate = useNavigate();
  const disabled = location.pathname.startsWith("/app/interview/");
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([{ role: "assistant", content: "Hey! I'm NEXORA Copilot. I can help you navigate your workspace, plan preparation, and understand your interview insights." }]);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState(false);
  const suggestedPrompts = getSuggestedPrompts(location.pathname);
  const bottomRef = useRef(null);

  useEffect(() => { if (disabled) setOpen(false); }, [disabled]);
  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" }); }, [messages, open]);

  const send = async (value = draft) => {
    const message = value.trim();
    if (!message || busy || disabled) return;
    setDraft("");
    setMessages((items) => [...items, { role: "user", content: message }]);
    const normalized = message.toLowerCase();
    const destinations = [
      { terms: ["dashboard", "overview", "home"], path: "/app", label: "your dashboard" },
      { terms: ["profile", "account"], path: "/app/profile", label: "your profile" },
      { terms: ["resume", "cv"], path: "/app/resume", label: "resume intelligence" },
      { terms: ["history", "my interviews", "sessions"], path: "/app/sessions", label: "interview history" },
      { terms: ["interview studio", "configuration", "configurations", "setup"], path: "/app/configurations", label: "interview studio" },
    ];
    const destination = destinations.find((item) => item.terms.some((term) => normalized.includes(term)) && /\b(open|go to|take me|show|navigate)\b/.test(normalized));
    if (destination) {
      navigate(destination.path);
      setMessages((items) => [...items, { role: "assistant", content: `Opening ${destination.label} for you.` }]);
      return;
    }
    setBusy(true);
    try {
      const { data } = await api.post("/assistant/chat", { message, pageContext: location.pathname });
      setMessages((items) => [...items, { role: "assistant", content: data?.message || "I couldn't prepare a response. Please try again." }]);
    } catch (error) {
      const text = error?.response?.data?.message || "Copilot is temporarily unavailable. Check your connection and AI provider quota, then try again.";
      setMessages((items) => [...items, { role: "assistant", content: text }]);
    } finally { setBusy(false); }
  };

  if (disabled) return null;
  return <div className="copilot-root">
    {open && <section className="copilot-panel" aria-label="NEXORA Copilot">
      <header className="copilot-header"><div className="copilot-avatar"><Bot size={20}/></div><div><strong>NEXORA Copilot</strong><span><i/> Ready to help</span></div><button onClick={() => setOpen(false)} aria-label="Close Copilot"><X size={18}/></button></header>
      <div className="copilot-messages">
        {messages.map((item, index) => <div key={index} className={`copilot-message ${item.role}`}><div className="copilot-message-icon">{item.role === "assistant" ? <Sparkles size={14}/> : "Y"}</div><p>{item.content}</p></div>)}

        {busy && <div className="copilot-thinking"><LoaderCircle className="spin" size={15}/> Thinking…</div>}
        <div ref={bottomRef}/>
      </div>
      <div className="copilot-suggestions" aria-label="Suggested follow-up prompts">
        <div className="copilot-suggestions-heading"><Sparkles size={12}/> SUGGESTED NEXT PROMPTS</div>
        <div className="copilot-starters">{suggestedPrompts.map((item) => <button key={item} onClick={() => send(item)} disabled={busy}>{item}<ArrowRight size={13}/></button>)}</div>
      </div>
      <form className="copilot-composer" onSubmit={(event) => { event.preventDefault(); send(); }}><input value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Ask NEXORA anything…" maxLength={2000} disabled={busy}/><button type="submit" disabled={!draft.trim() || busy} aria-label="Send message"><Send size={17}/></button></form>
      <div className="copilot-disclaimer">Copilot can make mistakes. Review important actions before confirming.</div>
    </section>}
    <button className={`copilot-launcher ${open ? "is-open" : ""}`} onClick={() => setOpen((value) => !value)} aria-label={open ? "Close NEXORA Copilot" : "Open NEXORA Copilot"}>{open ? <X size={22}/> : <><MessageCircle size={21}/><span>Ask NEXORA</span><i/></>}</button>
  </div>;
}
