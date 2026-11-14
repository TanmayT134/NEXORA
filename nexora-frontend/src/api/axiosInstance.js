import axios from "axios";

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/v1",
  timeout: 60000,
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("nexora_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // Do not silently clear a valid-looking local session on any endpoint-level 401.
    // Preserve the page and surface the backend response so the exact failing request
    // can be diagnosed. The user can explicitly sign out and sign in again if needed.
    return Promise.reject(error);
  }
);

export default axiosInstance;