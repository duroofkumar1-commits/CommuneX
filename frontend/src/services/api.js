import axios from "axios";

const apiBaseURL = (
  import.meta.env.VITE_API_URL || "/api"
).replace(/\/+$/, "");

const api = axios.create({
  baseURL: apiBaseURL,
  headers: { "Content-Type": "application/json" },
  timeout: 20000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      localStorage.getItem("token")
    ) {
      localStorage.removeItem("token");
      window.dispatchEvent(
        new CustomEvent("communex:session-expired")
      );
    }
    return Promise.reject(error);
  }
);

export function errorMessage(
  error,
  fallback = "Something went wrong. Please try again."
) {
  const data = error?.response?.data;

  if (typeof data?.detail === "string") return data.detail;
  if (typeof data?.message === "string") return data.message;
  if (typeof data?.error === "string") return data.error;

  if (error?.code === "ECONNABORTED") {
    return "The server took too long to respond.";
  }

  if (!error?.response) {
    return "Cannot reach the CommuneX server. Check that the backend is running.";
  }

  return fallback;
}

export default api;