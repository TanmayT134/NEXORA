import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AuthLayout from "../layouts/AuthLayout";
import AppLayout from "../layouts/AppLayout";
import Login from "../pages/Login";
import Register from "../pages/Register";
import Dashboard from "../pages/Dashboard";
import MySessions from "../pages/MySessions";
import Profile from "../pages/Profile";
import Resume from "../pages/Resume";
import Configurations from "../pages/Configurations";
import InterviewRoom from "../pages/InterviewRoom";
import Evaluation from "../pages/Evaluation";
import NotFound from "../pages/NotFound";

function Protected({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}
function Guest({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to="/app" replace /> : children;
}

export default function AppRoutes() {
  return <Routes>
    <Route element={<Guest><AuthLayout /></Guest>}>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
    </Route>
    <Route path="/" element={<Navigate to="/app" replace />} />
    <Route path="/app" element={<Protected><AppLayout /></Protected>}>
      <Route index element={<Dashboard />} />
      <Route path="sessions" element={<MySessions />} />
      <Route path="profile" element={<Profile />} />
      <Route path="resume" element={<Resume />} />
      <Route path="configurations" element={<Configurations />} />
      <Route path="interview/:sessionId" element={<InterviewRoom />} />
      <Route path="evaluation/:sessionId" element={<Evaluation />} />
    </Route>
    <Route path="*" element={<NotFound />} />
  </Routes>;
}