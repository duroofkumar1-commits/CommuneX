import { useEffect, useState } from "react";
import { Link, useOutletContext } from "react-router-dom";
import api, { errorMessage } from "../services/api";

const actions = [
  ["⌖", "Discover facilities", "Find hospitals, clinics, libraries and community spaces.", "/facilities", "Explore facilities"],
  ["✚", "Ask for help", "Create and track a community assistance request.", "/help-requests", "Open help centre"],
  ["♧", "Join your community", "Share ideas, discuss local needs and meet neighbours.", "/community", "Visit community"],
  ["♫", "Take a mindful break", "Use your own local audio in the music player.", "/music", "Open music"],
  ["▣", "Write something down", "Capture notes, plans and ideas with undo and redo.", "/editor", "Open editor"],
  ["▧", "Play & learn", "Try logic games and algorithm-inspired challenges.", "/games", "Explore games"],
];

const EMPTY_STATS = { requests: null, volunteers: null, facilities: null, events: null };

export default function Dashboard() {
  const { user } = useOutletContext();
  const [stats, setStats] = useState(EMPTY_STATS);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    api.get("/dashboard/summary", { signal: controller.signal })
      .then(({ data }) => {
        if (data && !controller.signal.aborted) {
          setStats({
            requests: Number.isFinite(Number(data.requests)) ? Number(data.requests) : null,
            volunteers: Number.isFinite(Number(data.volunteers)) ? Number(data.volunteers) : null,
            facilities: Number.isFinite(Number(data.facilities)) ? Number(data.facilities) : null,
            events: Number.isFinite(Number(data.events)) ? Number(data.events) : null,
          });
        }
      })
      .catch((err) => {
        if (err.code !== "ERR_CANCELED" && !controller.signal.aborted) {
          setError(errorMessage(err, "Could not load your community summary."));
        }
      });
    return () => controller.abort();
  }, []);

  const value = (count) => count === null ? "—" : count;

  return (
    <div className="section-stack">
      <section className="hero-panel">
        <p className="eyebrow">YOUR COMMUNITY, CONNECTED</p>
        <h1>{user ? `Good to see you, ${user.name?.split(" ")[0] || "neighbour"}.` : "Smart communities. Connected solutions."}</h1>
        <p>Discover trusted community information, coordinate support, find local opportunities and make useful connections — all in one place.</p>
        <div className="inline-actions"><Link className="btn lime" to="/search">⌕ Explore CommuneX</Link><Link className="btn secondary" to="/help-requests">＋ Request assistance</Link></div>
      </section>
      {error && <p className="status-message info" role="status">{error}</p>}
      <section className="metric-grid" aria-label="Live platform counts">
        <article className="surface metric-card"><div className="metric-icon">✚</div><div className="metric-label">Your requests</div><div className="metric-value">{value(stats.requests)}</div><div className="metric-help">Requests you have submitted</div></article>
        <article className="surface metric-card"><div className="metric-icon">♧</div><div className="metric-label">Available volunteers</div><div className="metric-value">{value(stats.volunteers)}</div><div className="metric-help">Profiles currently marked available</div></article>
        <article className="surface metric-card"><div className="metric-icon">⌖</div><div className="metric-label">Facilities listed</div><div className="metric-value">{value(stats.facilities)}</div><div className="metric-help">Platform records, when configured</div></article>
        <article className="surface metric-card"><div className="metric-icon">▦</div><div className="metric-label">Community events</div><div className="metric-value">{value(stats.events)}</div><div className="metric-help">Published event records</div></article>
      </section>
      <section>
        <div className="panel-title"><div><h2>Explore CommuneX</h2><p>Everything you need to participate in your community.</p></div><Link className="btn ghost small-btn" to="/search">Global search ↗</Link></div>
        <div className="feature-grid">{actions.map(([icon, title, description, href, label]) => <Link className="surface feature-card" to={href} key={href}><span className="feature-icon">{icon}</span><div><h3>{title}</h3><p>{description}</p><span className="small" style={{ color: "#357660", display: "inline-block", marginTop: 11, fontWeight: 800 }}>{label} →</span></div></Link>)}</div>
      </section>
      <section className="two-column">
        <div className="surface panel"><div className="panel-title"><div><h2>Community first</h2><p>Helpful actions, in one connected workflow.</p></div><span className="badge">LIVE MODULES</span></div><div className="data-list">
          <Link to="/community" className="data-card" style={{ textDecoration: "none" }}><div className="data-card-head"><div><h3>Community feed</h3><p>Publish a post, comment, react or report inappropriate content.</p></div><span>↗</span></div></Link>
          <Link to="/help-requests" className="data-card" style={{ textDecoration: "none" }}><div className="data-card-head"><div><h3>Assistance coordination</h3><p>Submit a support request and track status changes.</p></div><span>↗</span></div></Link>
          <Link to="/notifications" className="data-card" style={{ textDecoration: "none" }}><div className="data-card-head"><div><h3>Notification centre</h3><p>Review platform updates and mark them as read.</p></div><span>↗</span></div></Link>
        </div></div>
        <div className="surface panel"><div className="panel-title"><div><h2>Safety note</h2><p>What CommuneX is for</p></div><span className="feature-icon">♡</span></div><p className="muted" style={{ fontSize: 12 }}>CommuneX helps neighbours coordinate community support. It is not an emergency dispatch service and does not replace official emergency services.</p><div className="divider" /><p className="muted" style={{ fontSize: 12 }}>Facility, route and inventory details are only as reliable as their verified, current source data. Empty sections mean records have not been configured yet.</p><Link className="btn secondary" to="/assistant">Ask CommuneX assistant</Link></div>
      </section>
    </div>
  );
}
