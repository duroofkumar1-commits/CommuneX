import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import api from "../services/api";
import Login from "../pages/Login";
import Register from "../pages/Register";

vi.mock("../services/api", () => ({
  default: { post: vi.fn() },
  errorMessage: (_error, fallback) => fallback || "Request failed",
}));

function renderRoutes(path, element) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={path} element={element} />
        <Route path="/login" element={<p>login-route-visible</p>} />
        <Route path="/dashboard" element={<p>dashboard-route-visible</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("authentication pages", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("registers with trimmed lower-case email and navigates to login", async () => {
    const user = userEvent.setup();
    api.post.mockResolvedValueOnce({ data: { id: 4 } });
    renderRoutes("/register", <Register />);
    await user.type(screen.getByLabelText(/full name/i), "Test Neighbour");
    await user.type(screen.getByLabelText(/email address/i), "  NEIGHBOUR@example.com");
    await user.type(screen.getByLabelText(/password/i), "SafePass123!");
    await user.click(screen.getByRole("button", { name: /create account/i }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/auth/register", {
      name: "Test Neighbour", email: "neighbour@example.com", password: "SafePass123!",
    }));
    expect(await screen.findByText("login-route-visible")).toBeInTheDocument();
  });

  it("stores a returned token and routes to the dashboard on successful login", async () => {
    const user = userEvent.setup();
    api.post.mockResolvedValueOnce({ data: { token: "test-jwt", email: "test@example.com" } });
    renderRoutes("/login", <Login />);
    await user.type(screen.getByLabelText(/email address/i), "test@example.com");
    await user.type(screen.getByLabelText(/^Password$/i), "SafePass123!");
    await user.click(screen.getByRole("button", { name: /sign in/i }));
    await waitFor(() => expect(localStorage.getItem("token")).toBe("test-jwt"));
    expect(await screen.findByText("dashboard-route-visible")).toBeInTheDocument();
  });

  it("shows an API failure without storing a token", async () => {
    const user = userEvent.setup();
    api.post.mockRejectedValueOnce(new Error("offline"));
    renderRoutes("/login", <Login />);
    await user.type(screen.getByLabelText(/email address/i), "test@example.com");
    await user.type(screen.getByLabelText(/^Password$/i), "SafePass123!");
    await user.click(screen.getByRole("button", { name: /sign in/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/offline/i);
    expect(localStorage.getItem("token")).toBeNull();
  });
});
