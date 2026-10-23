import { useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../services/api";
import BrandLogo from "../components/BrandLogo";
import "../styles/auth.css";

const DEFAULT_NOTICE =
  "If your account exists and email is configured, an email will be sent.";

export default function ForgotPassword() {
  /* ---------- State ---------- */
  const [email, setEmail] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [resendVerification, setResendVerification] = useState(false);

  /* ---------- Handlers ---------- */
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");

    try {
      const path = resendVerification
        ? "/auth/resend-verification"
        : "/auth/forgot-password";
      const { data } = await api.post(path, {
        email: email.trim().toLowerCase(),
      });
      setNotice(data.message || DEFAULT_NOTICE);
    } catch (e) {
      setError(errorMessage(e, "The request could not be processed."));
    } finally {
      setBusy(false);
    }
  }

  const buttonLabel = busy
    ? "Sending request…"
    : resendVerification
    ? "Resend verification"
    : "Request password reset";

  /* ---------- UI ---------- */
  return (
    <main className="auth-page">
      {/* Left side */}
      <aside className="auth-aside">
        <BrandLogo />

        <div>
          <p className="eyebrow">ACCOUNT SUPPORT</p>
          <h1>Let's get you back to your community.</h1>
          <p>
            Request a one-time password reset or resend your email verification
            link.
          </p>
        </div>
      </aside>

      {/* Right side */}
      <section className="auth-main">
        <div className="auth-card">
          <h2>Account recovery</h2>
          <p className="subtitle">
            We will never reveal whether an email address has an account.
          </p>

          {notice && (
            <p className="status-message success" role="status">
              {notice}
            </p>
          )}
          {error && (
            <p className="status-message error" role="alert">
              {error}
            </p>
          )}

          <form className="form-stack" onSubmit={submit}>
            <label>
              <span className="field-label">Email address</span>
              <input
                className="field"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>

            <label
              style={{
                display: "flex",
                gap: 9,
                alignItems: "center",
                fontSize: 12,
              }}
            >
              <input
                type="checkbox"
                checked={resendVerification}
                onChange={(e) => setResendVerification(e.target.checked)}
              />
              Resend email verification instead
            </label>

            <button className="btn" type="submit" disabled={busy}>
              {buttonLabel}
            </button>
          </form>

          <p className="auth-footer">
            <Link to="/login">Back to sign in</Link>
          </p>
          <p className="muted small">
            Email delivery requires SMTP settings in the backend environment.
          </p>
        </div>
      </section>
    </main>
  );
}
