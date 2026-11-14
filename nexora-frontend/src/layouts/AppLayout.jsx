import { NavLink, Outlet, useLocation, Link } from "react-router-dom";
import { BrainCircuit, LayoutDashboard, UserRound, FileText, SlidersHorizontal, LogOut, Menu, X, ChevronRight, History, Sparkles, Bell, Command } from "lucide-react";
import { useEffect, useState } from "react";
import api from "../api/axiosInstance";
import { useAuth } from "../context/AuthContext";
import NexoraCopilot from "../components/NexoraCopilot";

const links = [
  { to: "/app", label: "Overview", icon: LayoutDashboard, end: true, group: "YOUR SPACE" },
  { to: "/app/sessions", label: "My interviews", icon: History, group: "YOUR SPACE" },
  { to: "/app/profile", label: "Profile", icon: UserRound, group: "PREPARATION" },
  { to: "/app/resume", label: "Resume intelligence", icon: FileText, group: "PREPARATION" },
  { to: "/app/configurations", label: "Interview studio", icon: SlidersHorizontal, group: "PREPARATION" },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [profileMenu, setProfileMenu] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const location = useLocation();
  const title = links.find(x => x.to === location.pathname)?.label || (location.pathname.includes("/evaluation/") ? "Performance report" : location.pathname.includes("/interview/") ? "Live interview" : "Workspace");
  const firstName = user?.fullName?.trim()?.split(" ")[0] || "Candidate";
  useEffect(() => {
    let mounted = true;
    api.get("/candidate/interview-sessions").then(({ data }) => {
      if (!mounted) return;
      const items = (Array.isArray(data) ? data : []).filter(item => ["IN_PROGRESS", "CREATED", "COMPLETED"].includes(item.status)).slice(0, 6);
      setNotifications(items);
    }).catch(() => { if (mounted) setNotifications([]); });
    return () => { mounted = false; };
  }, [location.pathname]);
  const signOut = () => { setProfileMenu(false); logout(); };
  return <div className="app-shell nexora-shell">
    <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
      <div className="sidebar-brand"><span className="brand-mark"><BrainCircuit size={21}/></span><span className="brand-word">NEXORA<span className="brand-period">.</span></span><button className="mobile-close" onClick={() => setOpen(false)} aria-label="Close menu"><X size={18}/></button></div>
      <div className="workspace-switch"><span className="workspace-avatar"><Sparkles size={15}/></span><span><strong>Personal workspace</strong><small>Candidate account</small></span><ChevronRight size={14}/></div>
      <nav className="side-nav">
        <div className="sidebar-label">YOUR SPACE</div>
        {links.filter(x => x.group === "YOUR SPACE").map(({to,label,icon:Icon,end}) => <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)} className={({isActive}) => `nav-link ${isActive ? "active" : ""}`}><Icon size={18}/><span>{label}</span>{to === "/app" && <span className="nav-key">⌘ 1</span>}</NavLink>)}
        <div className="sidebar-label nav-group-label">PREPARATION</div>
        {links.filter(x => x.group === "PREPARATION").map(({to,label,icon:Icon}) => <NavLink key={to} to={to} onClick={() => setOpen(false)} className={({isActive}) => `nav-link ${isActive ? "active" : ""}`}><Icon size={18}/><span>{label}</span></NavLink>)}
      </nav>
      <div className="sidebar-bottom"><div className="sidebar-tip"><div className="tip-topline"><span className="tip-icon"><Sparkles size={16}/></span><span className="tip-pill">NEXORA AI</span></div><strong>Your next opportunity starts here.</strong><p>Build clarity, confidence, and consistency with every practice session.</p></div><button className="logout-link" onClick={logout}><LogOut size={17}/> Sign out <span>↗</span></button></div>
    </aside>
    {open && <button className="sidebar-backdrop" aria-label="Close navigation" onClick={() => setOpen(false)}/>}
    <div className="main-column"><header className="topbar"><div className="topbar-left"><button className="mobile-menu" onClick={() => setOpen(true)} aria-label="Open menu"><Menu size={20}/></button><div><div className="breadcrumb"><span>WORKSPACE</span><i>/</i>{title.toUpperCase()}</div><h2>{title}</h2></div></div><div className="topbar-right"><div className="topbar-status"><i/> All systems operational</div><div className="topbar-popover-wrap"><button className="topbar-icon" aria-label="Notifications" aria-expanded={notificationsOpen} onClick={() => { setNotificationsOpen(v => !v); setProfileMenu(false); }}><Bell size={17}/>{notifications.length > 0 && <i/>}</button>{notificationsOpen && <div className="topbar-popover notification-popover"><div className="popover-heading"><strong>Notifications</strong><span>{notifications.length} updates</span></div>{notifications.length ? notifications.map(item => <Link key={item.id} to={item.status === "COMPLETED" ? `/app/evaluation/${item.id}` : `/app/interview/${item.id}`} onClick={() => setNotificationsOpen(false)} className="notification-item"><span className={`notification-dot ${String(item.status).toLowerCase()}`}/><span><strong>{item.status === "IN_PROGRESS" ? "Interview in progress" : item.status === "COMPLETED" ? "Evaluation available" : "Interview ready to start"}</strong><small>{item.targetRole || "Interview practice"}</small></span><ChevronRight size={14}/></Link>) : <p className="popover-empty">You're all caught up. Your interview updates will appear here.</p>}<Link to="/app/sessions" className="popover-footer" onClick={() => setNotificationsOpen(false)}>Open interview history <ChevronRight size={14}/></Link></div>}</div><div className="topbar-divider"/><div className="topbar-popover-wrap"><button type="button" className="topbar-user topbar-user-button" aria-expanded={profileMenu} onClick={() => { setProfileMenu(v => !v); setNotificationsOpen(false); }}><div className="avatar">{user?.fullName?.slice(0,1)?.toUpperCase() || "C"}</div><div className="user-meta"><strong>{firstName}</strong><span>Candidate</span></div><ChevronRight size={14}/></button>{profileMenu && <div className="topbar-popover profile-popover"><div className="profile-popover-user"><div className="avatar">{user?.fullName?.slice(0,1)?.toUpperCase() || "C"}</div><span><strong>{user?.fullName || "Candidate"}</strong><small>{user?.email || ""}</small></span></div><Link to="/app/profile" onClick={() => setProfileMenu(false)}><UserRound size={16}/> My profile</Link><button type="button" className="popover-signout" onClick={signOut}><LogOut size={16}/> Sign out <span>↗</span></button></div>}</div></div></header><main className="page-content"><div key={location.pathname} className="route-motion"><Outlet/></div></main><footer className="app-footer"><span>© {new Date().getFullYear()} NEXORA</span><span>Designed for your next chapter <b>✦</b></span></footer></div><NexoraCopilot/>
  </div>;
}
