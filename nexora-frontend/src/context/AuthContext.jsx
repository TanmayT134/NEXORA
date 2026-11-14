import { createContext, useContext, useMemo, useState } from "react";
import api from "../api/axiosInstance";

const AuthContext = createContext(null);
const USER_KEY = "nexora_user";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || "null"); }
    catch { return null; }
  });
  const [token, setToken] = useState(() => localStorage.getItem("nexora_token"));

  const login = async (credentials) => {
    const { data } = await api.post("/auth/login", credentials);
    localStorage.setItem("nexora_token", data.accessToken);
    const nextUser = { id: data.userId, fullName: data.fullName, email: data.email, role: data.role };
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(data.accessToken);
    setUser(nextUser);
    return nextUser;
  };

  const register = async (details) => (await api.post("/auth/register", details)).data;

  const logout = () => {
    localStorage.removeItem("nexora_token");
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  };


  const value = useMemo(() => ({ user, token, isAuthenticated: Boolean(token && user), login, register, logout }), [user, token]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}