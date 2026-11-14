import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../utils/errors";
export default function Login() {
  const { login } = useAuth(); const navigate = useNavigate();
  const [form,setForm] = useState({email:"",password:""}); const [show,setShow] = useState(false); const [loading,setLoading] = useState(false); const [error,setError] = useState("");
  const change = e => setForm({...form,[e.target.name]:e.target.value});
  const submit = async e => { e.preventDefault(); setError(""); setLoading(true); try { await login(form); navigate("/app",{replace:true}); } catch(err) { setError(getErrorMessage(err)); } finally { setLoading(false); } };
  return <div className="auth-card"><div className="eyebrow"><span className="eyebrow-dot"/> YOUR NEXT CHAPTER STARTS HERE</div><h2>Welcome back</h2><p className="auth-subtitle">Sign in to continue your interview preparation.</p>
    {error && <div className="form-error">{error}</div>}
    <form onSubmit={submit} className="form-stack"><label>Email address</label><div className="input-wrap"><Mail size={17}/><input name="email" type="email" placeholder="you@example.com" value={form.email} onChange={change} required autoComplete="email"/></div><div className="field-row"><label>Password</label></div><div className="input-wrap"><LockKeyhole size={17}/><input name="password" type={show?"text":"password"} placeholder="Enter your password" value={form.password} onChange={change} required autoComplete="current-password"/><button type="button" className="input-action" onClick={()=>setShow(!show)}>{show?<EyeOff size={17}/>:<Eye size={17}/>}</button></div><button className="btn btn-primary btn-full" disabled={loading}>{loading?"Signing in...":"Sign in"}<ArrowRight size={17}/></button></form>
    <p className="auth-switch">New to NEXORA? <Link to="/register">Create an account</Link></p><p className="auth-legal">By continuing, you agree to use NEXORA responsibly for learning and interview practice.</p>
  </div>;
}