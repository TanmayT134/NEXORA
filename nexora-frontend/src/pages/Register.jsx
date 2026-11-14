import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, UserRound, Mail, LockKeyhole } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../utils/errors";
export default function Register() {
  const { register } = useAuth(); const navigate=useNavigate();
  const [form,setForm]=useState({fullName:"",email:"",password:""}); const [show,setShow]=useState(false); const [loading,setLoading]=useState(false); const [error,setError]=useState("");
  const change=e=>setForm({...form,[e.target.name]:e.target.value});
  const submit=async e=>{e.preventDefault();setError("");setLoading(true);try{await register(form);navigate("/login",{replace:true,state:{registered:true}});}catch(err){setError(getErrorMessage(err));}finally{setLoading(false);}};
  return <div className="auth-card"><div className="eyebrow"><span className="eyebrow-dot"/> START YOUR PREPARATION</div><h2>Create your account</h2><p className="auth-subtitle">Set up your candidate workspace in a few steps.</p>{error&&<div className="form-error">{error}</div>}
    <form onSubmit={submit} className="form-stack"><label>Full name</label><div className="input-wrap"><UserRound size={17}/><input name="fullName" placeholder="Your full name" value={form.fullName} onChange={change} maxLength={100} required/></div><label>Email address</label><div className="input-wrap"><Mail size={17}/><input name="email" type="email" placeholder="you@example.com" value={form.email} onChange={change} maxLength={150} required/></div><label>Password</label><div className="input-wrap"><LockKeyhole size={17}/><input name="password" type={show?"text":"password"} placeholder="At least 8 characters" value={form.password} onChange={change} minLength={8} maxLength={72} required/><button type="button" className="input-action" onClick={()=>setShow(!show)}>{show?<EyeOff size={17}/>:<Eye size={17}/>}</button></div><button className="btn btn-primary btn-full" disabled={loading}>{loading?"Creating account...":"Create account"}<ArrowRight size={17}/></button></form>
    <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p><p className="auth-legal">Your account is created securely by the NEXORA backend.</p>
  </div>;
}