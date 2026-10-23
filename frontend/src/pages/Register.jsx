import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errorMessage } from "../services/api";
import BrandLogo from "../components/BrandLogo";
import "../styles/auth.css";

export default function Register() {
  const navigate = useNavigate();

  /* ---------- State ---------- */
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  /* ---------- Handlers ---------- */
  async function submit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);

    try {
      await api.post("/auth/register", {
        name: name.trim(),
        email: email.trim().toLowerCase(),
        password,
      });
      navigate("/login?registered=1", { replace: true });
    } catch (err) {
      setError(
        errorMessage(
          err,
          "Registration failed. Check the details and try another email if already registered."
        )
      );
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
          <p className="eyebrow">YOUR COMMUNITY NEEDS YOU</p>
          <h1>Find your people. Share what you can. Build something good.</h1>
          <p>
            Join community conversations, discover resources, volunteer for
            meaningful tasks and take part in local events.
          </p>
          <div className="auth-feature-pills">
            <span>Free to get started</span>
            <span>Community-first</span>
            <span>Privacy-aware</span>
          </div>
        </div>

        <p className="small">
          A welcoming place for people who want to make a difference.
        </p>
      </aside>

      {/* Right side */}
      <section className="auth-main">
        <div className="auth-card">
          <h2>Create your account</h2>
          <p className="subtitle">A few details to get you started.</p>

          {error && (
            <p className="status-message error" role="alert">
              {error}
            </p>
          )}

          <form className="form-stack" onSubmit={submit}>
            <label>
              <span className="field-label">Full name</span>
              <input
                className="field"
                type="text"
                autoComplete="name"
                maxLength={100}
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </label>

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

            <label>
              <span className="field-label">Password</span>
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
              <span className="muted small">
                Use 8–72 characters. Never reuse a sensitive password.
              </span>
            </label>

            <button className="btn" type="submit" disabled={busy}>
              {busy ? "Creating account…" : "Create account →"}
            </button>
          </form>

          <p className="auth-footer">
            Already a member? <Link to="/login">Sign in</Link>
          </p>
          <p className="auth-footer">
            By joining, you agree not to misuse community information or harass
            other users.
          </p>
        </div>
      </section>
    </main>
  );
}
