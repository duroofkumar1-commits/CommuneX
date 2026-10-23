import { useEffect, useState } from "react";
import { errorMessage } from "../services/api";
import api from "../services/api";

export default function Announcements() {
  const [items, setItems] = useState([]);
  const [user, setUser] = useState(null);
  const [form, setForm] = useState({ title: "", category: "GENERAL", body: "" });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  async function load() {
    try { const { data } = await api.get("/announcements"); setItems(Array.isArray(data) ? data : []); }
    catch (e) { setError(errorMessage(e, "Could not load announcements.")); }
  }
  useEffect(() => { api.get("/users/me").then(({ data }) => setUser(data)).catch(() => {}); load(); }, []);

  async function publish(event) {
    event.preventDefault(); setBusy(true); setError(""); setNotice("");
    try { await api.post("/announcements", form); setNotice("Announcement published."); setForm({ title: "", category: "GENERAL", body: "" }); setShowForm(false); await load(); }
    catch (e) { setError(errorMessage(e, "Only an authorized moderator or admin can publish announcements.")); }
    finally { setBusy(false); }
  }
  const canPublish = ["ADMIN", "MODERATOR"].includes(user?.role);
  return <div className="section-stack">
    <div className="page-heading"><div><p className="eyebrow">OFFICIAL COMMUNITY UPDATES</p><h1>Announcements</h1><p>Read local platform announcements. Information is only shown when a verified/authorized platform member publishes it.</p></div>{canPublish && <button className="btn" onClick={() => setShowForm((v) => !v)}>{showForm ? "Cancel" : "+ New announcement"}</button>}</div>
    {error && <div className="status-message error" role="alert">{error}</div>}{notice && <div className="status-message success" role="status">{notice}</div>}
    {showForm && canPublish && <form className="surface panel form-stack" onSubmit={publish}><h2>Publish announcement</h2><div className="form-grid"><label className="form-field"><span className="field-label">Title</span><input className="field" required maxLength={160} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}/></label><label className="form-field"><span className="field-label">Category</span><select className="field-select" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}><option value="GENERAL">General</option><option value="SAFETY">Safety</option><option value="EVENT">Event</option><option value="RESOURCE">Resources</option><option value="SERVICE">Service update</option></select></label><label className="form-field full"><span className="field-label">Announcement</span><textarea className="field-textarea" required maxLength={3000} rows={5} value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })}/></label></div><button className="btn" disabled={busy}>{busy ? "Publishing…" : "Publish"}</button></form>}
    <section className="surface panel"><div className="panel-title"><div><h2>Latest announcements</h2><p>{items.length} published item(s)</p></div><button className="btn ghost small-btn" onClick={load}>Refresh</button></div>
      {items.length === 0 ? <div className="empty-state"><span className="empty-symbol">▤</span><h3>No announcements published</h3><p>When an authorized community organizer shares an announcement, it will appear here.</p></div> : <div className="data-list">{items.map((item) => <article className="data-card" key={item.id}><div className="data-card-head"><div><span className="badge neutral">{item.category}</span><h3 style={{ marginTop: 10 }}>{item.title}</h3></div><span className="data-meta">{item.createdAt ? new Date(item.createdAt).toLocaleString() : ""}</span></div><p style={{ whiteSpace: "pre-wrap" }}>{item.body}</p><div className="data-meta"><span>Published by {item.authorName}</span></div></article>)}</div>}
    </section>
  </div>;
}
