import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api, { errorMessage } from "../services/api";

export default function VerifyEmail() {
  const [params] = useSearchParams(); const token = params.get("token"); const [status, setStatus] = useState("Verifying your email…"); const [failed, setFailed] = useState(false);
  useEffect(() => { let active = true; if (!token) { setFailed(true); setStatus("Verification link is missing or invalid."); return () => { active = false; }; } api.post("/auth/verify-email", { token }).then(({ data }) => { if (active) setStatus(data.message || "Email verified. You can sign in now."); }).catch((e) => { if (active) { setFailed(true); setStatus(errorMessage(e, "This verification link is invalid or expired.")); } }); return () => { active = false; }; }, [token]);
  return <main className="auth-page"><aside className="auth-aside"><div className="brand-lockup"><span className="brand-symbol">C<span>×</span></span><span><strong>CommuneX</strong><small>ACCOUNT VERIFICATION</small></span></div><div><p className="eyebrow">ONE COMMUNITY, BUILT TOGETHER</p><h1>Confirm your email address.</h1><p>Email verification helps protect community accounts and communications.</p></div></aside><section className="auth-main"><div className="auth-card"><h2>Email verification</h2><p className={`status-message ${failed ? "error" : "success"}`} role="status">{status}</p><p className="auth-footer"><Link to="/login">Go to sign in</Link> · <Link to="/forgot-password">Resend verification</Link></p></div></section></main>;
}
