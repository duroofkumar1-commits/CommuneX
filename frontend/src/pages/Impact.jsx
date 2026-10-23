import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../services/api";

const challenges = [
  { title: "Explore a community facility", description: "Use the facility directory and review its verified status before relying on its details.", route: "/facilities" },
  { title: "Discover a community discussion", description: "Read a post and consider adding a constructive, respectful comment.", route: "/community" },
  { title: "Find a relevant community event", description: "Explore events and check the details before registering.", route: "/events" },
  { title: "Try an educational game", description: "Solve a small challenge and learn how an algorithm works.", route: "/games" },
  { title: "Explore community resources", description: "Review the recorded resource inventory; availability is based on recorded data.", route: "/resources" },
  { title: "Make a useful community plan", description: "Use Focus Mode to write down one practical idea for your neighbourhood.", route: "/focus" },
  { title: "Search for local information", description: "Use global search to look for a facility, volunteer or community topic.", route: "/search" },
];

function dayKey() {
  return new Date().toISOString().slice(0, 10);
}

export default function Impact() {
  const [activity, setActivity] = useState([]);
  const [scores, setScores] = useState([]);
  const [challengeDone, setChallengeDone] = useState(() => localStorage.getItem(`communex.challenge.${dayKey()}`) === "done");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [challengeIndex] = useState(() => Math.floor(Date.now() / 86400000) % challenges.length);
  const challenge = challenges[challengeIndex];

  const load = useCallback(async () => {
    const [activityResult, scoresResult] = await Promise.allSettled([api.get("/activity"), api.get("/games/scores/me")]);
    if (activityResult.status === "fulfilled") setActivity(Array.isArray(activityResult.value.data) ? activityResult.value.data : []);
    if (scoresResult.status === "fulfilled") setScores(Array.isArray(scoresResult.value.data) ? scoresResult.value.data : []);
    if (activityResult.status === "rejected" && scoresResult.status === "rejected") setError(errorMessage(activityResult.reason, "Could not load personal activity."));
    else setError("");
  }, []);

  useEffect(() => { load(); }, [load]);

  function completeChallenge() {
    localStorage.setItem(`communex.challenge.${dayKey()}`, "done");
    setChallengeDone(true);
    setNotice("Today's personal challenge is marked complete on this device. This does not count as verified community contribution.");
  }

  const today = activity.filter((item) => item.createdAt && new Date(item.createdAt).toISOString().slice(0, 10) === dayKey()).length;
  const points = Number(localStorage.getItem("communex.game.points") || 0);

  return (
    <div className="section-stack">
      <div className="page-heading"><div><p className="eyebrow">YOUR JOURNEY</p><h1>Impact & challenges</h1><p>Review recorded account activity and game scores, then choose one small action for today.</p></div><span className="badge neutral">REAL RECORDS + LOCAL CHALLENGE</span></div>
      {error && <div className="status-message error" role="alert">{error}</div>}
      {notice && <div className="status-message info" role="status">{notice}</div>}
      <div className="stat-grid">
        <article className="stat-card"><span className="stat-label">Recorded activity</span><strong className="stat-value">{activity.length}</strong><span className="stat-foot">Events returned by the account activity API</span></article>
        <article className="stat-card"><span className="stat-label">Today's activity</span><strong className="stat-value">{today}</strong><span className="stat-foot">Based on timestamps in your activity history</span></article>
        <article className="stat-card"><span className="stat-label">Game points</span><strong className="stat-value">{points}</strong><span className="stat-foot">Entertainment progress, not real-world contribution</span></article>
        <article className="stat-card"><span className="stat-label">Saved game scores</span><strong className="stat-value">{scores.length}</strong><span className="stat-foot">Score records returned by the backend</span></article>
      </div>
      <section className="surface panel">
        <div className="panel-title"><div><p className="eyebrow">DAILY CHALLENGE</p><h2>{challenge.title}</h2><p>{challenge.description}</p></div><span className={`badge ${challengeDone ? "success" : "neutral"}`}>{challengeDone ? "COMPLETED TODAY" : "READY"}</span></div>
        <div className="inline-actions"><Link className="btn secondary" to={challenge.route}>Open feature</Link><button className="btn" onClick={completeChallenge} disabled={challengeDone}>{challengeDone ? "Completed" : "Mark personal challenge complete"}</button></div>
        <p className="muted small" style={{ marginBottom: 0, marginTop: 12 }}>Challenge status is stored in this browser. It is not proof of a volunteer hour or verified community impact.</p>
      </section>
      <section className="surface panel">
        <div className="panel-title"><div><h2>Recent activity</h2><p>Actions recorded by CommuneX services.</p></div><span className="badge neutral">{activity.length} RECORDS</span></div>
        {!activity.length ? <div className="empty-state"><span className="empty-symbol">◷</span><h3>No activity records yet</h3><p>Your activity history will appear here when supported actions are recorded.</p></div> : <div className="data-list">{activity.slice(0, 12).map((item) => <article className="data-card" key={item.id}><div className="form-footer"><strong>{String(item.action || "Activity").replaceAll("_", " ")}</strong><span className="muted small">{item.createdAt ? new Date(item.createdAt).toLocaleString() : "Date unavailable"}</span></div><p>{item.details || item.entityType || "Platform activity"}</p></article>)}</div>}
      </section>
    </div>
  );
}
