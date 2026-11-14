import { Outlet } from "react-router-dom";
import { BrainCircuit, Sparkles, ShieldCheck, Target } from "lucide-react";
export default function AuthLayout() {
  return <main className="auth-shell">
    <section className="auth-visual">
      <div className="brand brand-light"><span className="brand-mark"><BrainCircuit size={21}/></span><span>NEXORA</span></div>
      <div className="auth-visual-copy">
        <div className="eyebrow eyebrow-light"><Sparkles size={14}/> INTERVIEW INTELLIGENCE</div>
        <h1>Prepare with purpose.<br/><em>Perform with confidence.</em></h1>
        <p>An adaptive interview experience that understands your answers, challenges your thinking, and helps you improve.</p>
        <div className="auth-benefits"><span><Target size={16}/> Adaptive practice</span><span><ShieldCheck size={16}/> Actionable feedback</span></div>
      </div>
      <div className="visual-orbit orbit-one"/><div className="visual-orbit orbit-two"/>
      <div className="visual-footer">A smarter way to get interview-ready.</div>
    </section>
    <section className="auth-content"><Outlet /></section>
  </main>;
}