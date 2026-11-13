import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api, { errorMessage } from "../services/api";
import GoogleSignInButton from "../components/GoogleSignInButton";
import "../styles/auth.css";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const [params] = useSearchParams();

  async function submit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);

    try {
      const { data } = await api.post("/auth/login", {
        email: email.trim().toLowerCase(),
        password,
      });

      if (!data?.token) {
        throw new Error("The login response did not include a token.");
      }

      localStorage.setItem("token", data.token);
      localStorage.setItem(
        "communex.email",
        data.email || email.trim().toLowerCase()
      );
      navigate("/dashboard", { replace: true });
    } catch (e) {
      setError(errorMessage(e, e.message || "Login failed. Check your credentials."));
    } finally {
      setBusy(false);
    }
  }

  async function handleGoogleCredential(credential) {
    setError("");
    setBusy(true);

    try {
  
      const { data } = await api.post("/auth/google", { credential });

      if (!data?.token) {
        throw new Error("Google sign-in response did not include a token.");
      }

      localStorage.setItem("token", data.token);
      if (data.email) localStorage.setItem("communex.email", data.email);
      navigate("/dashboard", { replace: true });
    } catch (e) {
      setError(
        errorMessage(
          e,
          "Google sign-in could not be completed. Check the Google OAuth backend configuration."
        )
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
  <aside className="auth-aside">
    <div className="brand-lockup brand-lockup--light">
      <span className="brand-symbol" aria-hidden="true">
        <svg viewBox="0 0 64 64" role="img">
          {/* Outer container shape - matched to outer background color #143b3c */}
          <path
            fillRule="evenodd"
            fill="#99DAB5" /* lines ka color */
            stroke="rgba(160, 220, 198, 0.2)"
            strokeWidth="1"
            d="M32.31 62L6.09 46.74 6.07 16.78 31.82 1.96 57.87 16.94 47.34 23.31 31.82 14.37 16.86 23.03 16.87 40.48 30.04 48.19C30.15 47.59 30.06 46.95 30.06 46.34 30.06 45.32 30.13 44.31 30.3 43.3 30.53 41.93 30.96 40.63 31.33 39.29 32.1 36.48 32.75 34.15 34.65 31.83 36.52 29.55 39.17 28.02 41.99 27.24 43.3 26.88 45.53 26.37 46.86 26.59 47.07 27.9 46.94 29.31 46.73 30.61 45.97 35.29 43.31 39.09 38.73 40.65 37.72 40.99 36.09 41.48 35.04 41.36 35.24 40.52 35.61 39.72 36 38.96 37.4 36.22 39.42 34.03 41.64 31.95 40.56 32.08 38.18 33.99 37.31 34.76 36.41 35.55 35.61 36.46 34.91 37.44 31.18 42.66 31.89 48.48 33.78 54.23L33.66 58.65 53.87 46.49 47.38 42.71 34.52 50.35 34.12 48.04 47.35 40.18 58.18 46.47 32.31 62ZM30.71 12.47L30.7 5.13 9.38 17.42 15.75 21.13 30.71 12.47ZM53.53 16.98L32.9 5.12 32.9 12.46 47.31 20.76 53.53 16.98ZM14.66 40.48L14.65 23.03 8.29 19.35 8.3 44.18 14.66 40.48ZM31.59 54.73L30.57 51.1 15.79 42.35 9.34 46.1 31.44 58.96 31.59 54.73Z"
          />
          {/* Inner leaves - colored with #99DAB5 */}
          <path
            fill="#99DAB5"
            d="M30.75 34.17C29.9 33.62 29.14 32.31 28.66 31.43 26.67 27.75 27.2 23.58 29.75 20.33 30.28 19.65 30.86 18.94 31.57 18.44 32.15 18.8 32.62 19.31 33.07 19.82 34.92 21.9 35.97 24.7 35.25 27.46 35.1 28.03 34.92 28.64 34.58 29.1L32.3 31.92 31.55 24.52 30.75 34.17ZM20.24 31.88C21.23 31.71 22.37 31.86 23.35 32.07 26.91 32.82 28.8 34.63 29.84 38.09L29.28 39.97C28.86 39.56 28.56 39.02 28.18 38.57 27.27 37.48 25.72 36.18 24.33 35.82 24.66 36.21 25.07 36.51 25.41 36.88 26.52 38.04 27.65 39.55 28.03 41.14 27.13 41.24 26.07 40.88 25.24 40.55 22.52 39.44 20.95 37.23 20.38 34.4 20.22 33.59 20.1 32.7 20.24 31.88Z"
          />
        </svg>
      </span>
      <span className="brand-copy">
        <strong>Commune<span>X</span></strong>
        <small>SMART COMMUNITIES. CONNECTED SOLUTIONS.</small>
      </span>
    </div>

        <div className="auth-intro">
          <p className="eyebrow">WELCOME TO YOUR COMMUNITY</p>
          <h1>A stronger community starts with a connection.</h1>
          <p className="auth-description">
            Discover local information, coordinate support, and engage in your
            community—all in one place.
          </p>
          <div className="auth-feature-pills">
            <span><span aria-hidden="true">♡</span> Community Support</span>
            <span><span aria-hidden="true">⌂</span> Local Resources</span>
            <span><span aria-hidden="true">☆</span> Community Impact</span>
          </div>
        </div>

        <p className="auth-aside-footer">
          Built for neighbours, volunteers, and community organizers.
        </p>
      </aside>

      <section className="auth-main">
        <div className="auth-card">
          <h2>Welcome Back to CommuneX</h2>
          <p className="auth-card-subtitle">Sign in to your community</p>
          <p className="auth-card-description">
            Your community is better when you're part of it.
          </p>

          {params.get("registered") && (
            <p className="status-message success">
              Account created. Sign in with your new credentials.
            </p>
          )}
          {params.get("password-reset") && (
            <p className="status-message success">
              Password reset. You can now sign in with your new password.
            </p>
          )}
          {error && <p className="status-message error" role="alert">{error}</p>}

          <form className="form-stack" onSubmit={submit}>
            <div className="auth-fields-grid">
              <label className="auth-field-label">
                <span>Email address</span>
                <input
                  className="field"
                  type="email"
                  placeholder="e.g., yourname@gmail.com"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </label>

              <label className="auth-field-label">
                <span>Password</span>
                <span className="password-input-wrap">
                  <input
                    className="field"
                    type={showPassword ? "text" : "password"}
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                  <button
                    className="password-toggle"
                    type="button"
                    onClick={() => setShowPassword((shown) => !shown)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    title={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? "◉" : "◎"}
                  </button>
                </span>
              </label>
            </div>

            <div className="forgot-row">
              <Link to="/forgot-password">Forgot password or need verification?</Link>
            </div>

            <button className="btn auth-submit" type="submit" disabled={busy}>
              {busy ? "Signing in…" : "Sign in"}
            </button>
          </form>

          <div className="auth-divider"><span>or continue with</span></div>

          <GoogleSignInButton
            disabled={busy}
            onCredential={handleGoogleCredential}
            onError={setError}
          />

          <p className="auth-register">
            New to CommuneX? <Link to="/register">Create an account</Link>
          </p>
          <p className="auth-legal">
            Rules and conditions apply. Please respect your community and protect
            members' privacy.
          </p>
        </div>
      </section>
    </main>
  );
}
