import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 20000,
});

const publicAuthPaths = [
  "/auth/login",
  "/auth/register",
  "/auth/google",
  "/auth/verify-email",
  "/auth/forgot-password",
  "/auth/reset-password",
  "/auth/resend-verification",
];

api.interceptors.request.use((config) => {
  const path = config.url || "";

  const isPublicAuthRoute = publicAuthPaths.some(
    (route) => path.startsWith(route)
  );

  const token = localStorage.getItem("token");

  if (token && !isPublicAuthRoute) {
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

  if (typeof data?.detail === "string") {
    return data.detail;
  }

  if (typeof data?.message === "string") {
    return data.message;
  }

  if (typeof data?.error === "string") {
    return data.error;
  }

  if (error?.code === "ECONNABORTED") {
    return "The server took too long to respond.";
  }

  if (!error?.response) {
    return "Cannot reach the CommuneX server. Check that the backend is running.";
  }

  return fallback;
}

export default api;