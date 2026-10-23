import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api, { errorMessage } from "../services/api";
import BrandLogo from "../components/BrandLogo";
import "../styles/auth.css";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get("token");

  /* ---------- State ---------- */
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  /* ---------- Handlers ---------- */
  async function submit(event) {
    event.preventDefault();
    setError("");

    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    setBusy(true);
    try {
      await api.post("/auth/reset-password", { token, newPassword: password });
      navigate("/login?password-reset=1", { replace: true });
    } catch (e) {
      setError(errorMessage(e, "This reset link is invalid or expired."));
    } finally {
      setBusy(false);
    }
  }

  /* ---------- UI ---------- */
  return (
    <main className="auth-page">
      {/* Left side */}
      <aside className="auth-aside">
        <BrandLogo />

        <div>
          <p className="eyebrow">PASSWORD RECOVERY</p>
          <h1>Choose a new password.</h1>
          <p>Reset links are single-use and expire after a short time.</p>
        </div>
      </aside>

      {/* Right side */}
      <section className="auth-main">
        <div className="auth-card">
          <h2>Set a new password</h2>
          <p className="subtitle">
            Choose a strong password you have not used elsewhere.
          </p>

          {error && (
            <p className="status-message error" role="alert">
              {error}
            </p>
          )}

          <form className="form-stack" onSubmit={submit}>
            <label>
              <span className="field-label">New password</span>
              <input
                className="field"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>

            <label>
              <span className="field-label">Confirm password</span>
              <input
                className="field"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
              />
            </label>

            <button className="btn" type="submit" disabled={busy || !token}>
              {busy ? "Updating…" : "Reset password"}
            </button>
          </form>

          {!token && (
            <p className="status-message error" role="alert">
              Reset token is missing. Request a new link.
            </p>
          )}

          <p className="auth-footer">
            <Link to="/forgot-password">Request another link</Link>
          </p>
        </div>
      </section>
    </main>
  );
}
