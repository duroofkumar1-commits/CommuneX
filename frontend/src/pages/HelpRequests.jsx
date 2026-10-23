import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../services/api";
import "./HelpRequests.css";

const terminalStates = new Set(["COMPLETED", "RESOLVED", "CLOSED", "CANCELLED"]);

export default function HelpRequests() {
  const [user, setUser] = useState(null);
  const [requests, setRequests] = useState([]);
  const [history, setHistory] = useState({});
  const [historyOpen, setHistoryOpen] = useState(null);
  const [view, setView] = useState("open");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [workingId, setWorkingId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState({ title: "", description: "", category: "", location: "" });

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get(view === "my" ? "/requests/my" : "/requests/open");
      const data = response.data;
      setRequests(Array.isArray(data) ? data : Array.isArray(data?.items) ? data.items : Array.isArray(data?.content) ? data.content : []);
    } catch (err) {
      setError(errorMessage(err, "Could not load help requests."));
      setRequests([]);
    } finally {
      setLoading(false);
    }
  }, [view]);

  useEffect(() => {
    let active = true;
    api.get("/users/me").then(({ data }) => { if (active) setUser(data); }).catch(() => {});
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const timer = setTimeout(loadRequests, 0);
    return () => clearTimeout(timer);
  }, [loadRequests]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submitRequest(event) {
    event.preventDefault();
    setError("");
    setNotice("");
    if (!form.title.trim() || !form.description.trim() || !form.category.trim() || !form.location.trim()) {
      setError("Please complete all four fields.");
      return;
    }
    setSubmitting(true);
    try {
      await api.post("/requests", {
        title: form.title.trim(),
        description: form.description.trim(),
        category: form.category.trim(),
        location: form.location.trim(),
      });
      setForm({ title: "", description: "", category: "", location: "" });
      setNotice("Your help request was submitted.");
      setView("my");
    } catch (err) {
      setError(errorMessage(err, "Could not submit the request."));
    } finally {
      setSubmitting(false);
    }
  }

  async function transition(request, action, successMessage) {
    setWorkingId(request.id);
    setError("");
    setNotice("");
    try {
      await api.post(`/requests/${request.id}/${action}`);
      setNotice(successMessage);
      await loadRequests();
    } catch (err) {
      setError(errorMessage(err, "The request could not be updated."));
    } finally {
      setWorkingId(null);
    }
  }

  async function loadHistory(request) {
    if (historyOpen === request.id) {
      setHistoryOpen(null);
      return;
    }
    setHistoryOpen(request.id);
    if (history[request.id]) return;
    try {
      const { data } = await api.get(`/requests/${request.id}/history`);
      setHistory((current) => ({ ...current, [request.id]: Array.isArray(data) ? data : [] }));
    } catch (err) {
      setError(errorMessage(err, "Request history is not available for this account."));
      setHistoryOpen(null);
    }
  }

  function actionButtons(request) {
    const me = user?.id;
    const isOwner = request.createdById === me;
    const isAssigned = request.assignedVolunteerId === me;
    const isResponder = ["VOLUNTEER", "RESPONDER"].includes(user?.role);
    const isModerator = ["ADMIN", "MODERATOR", "RESPONDER"].includes(user?.role);
    const busy = workingId === request.id;
    const status = request.status;
    return <div className="help-card-actions">
      {view === "open" && isResponder && status === "OPEN" && <button type="button" disabled={busy} onClick={() => transition(request, "accept", "Request accepted.")}>Accept request</button>}
      {isAssigned && status === "ACCEPTED" && <button type="button" disabled={busy} onClick={() => transition(request, "in-progress", "Request marked in progress.")}>Start work</button>}
      {isAssigned && ["ACCEPTED", "IN_PROGRESS"].includes(status) && <button type="button" disabled={busy} onClick={() => transition(request, "complete", "Request marked completed.")}>Mark completed</button>}
      {isOwner && ["OPEN", "ACCEPTED"].includes(status) && <button type="button" className="secondary-action" disabled={busy} onClick={() => transition(request, "cancel", "Request cancelled.")}>Cancel</button>}
      {isModerator && !terminalStates.has(status) && <button type="button" className="secondary-action" disabled={busy} onClick={async () => {
        const note = window.prompt("Add a status-change note (required):");
        if (!note?.trim()) return;
        const target = window.prompt("Enter RESOLVED, CLOSED or CANCELLED:", "RESOLVED");
        if (!["RESOLVED", "CLOSED", "CANCELLED"].includes((target || "").trim().toUpperCase())) return;
        setWorkingId(request.id);
        try {
          await api.patch(`/requests/${request.id}/status`, { status: target.trim().toUpperCase(), note: note.trim() });
          setNotice("Request status updated.");
          await loadRequests();
        } catch (err) { setError(errorMessage(err, "Could not update request status.")); }
        finally { setWorkingId(null); }
      }}>Manage status</button>}
      <button type="button" className="secondary-action" disabled={busy} onClick={() => loadHistory(request)}>{historyOpen === request.id ? "Hide history" : "View history"}</button>
    </div>;
  }

  return <main className="help-page">
    <header className="help-header"><div><p className="help-eyebrow">COMMUNEX COMMUNITY</p><h1>Help Requests</h1><p className="help-subtitle">Coordinate community support. CommuneX is not a substitute for local emergency services.</p></div><button className="help-refresh" type="button" onClick={loadRequests} disabled={loading}>Refresh</button></header>
    <section className="help-layout">
      <div className="help-form-card"><h2>Request help</h2><p>Explain what you need so community members can understand.</p>
        <form onSubmit={submitRequest}>
          <label htmlFor="help-title">Request title</label><input id="help-title" name="title" value={form.title} onChange={updateField} maxLength={150} placeholder="e.g. Help carrying groceries" required />
          <label htmlFor="help-category">Category</label><input id="help-category" name="category" value={form.category} onChange={updateField} maxLength={100} placeholder="e.g. Groceries, Transport" required />
          <label htmlFor="help-location">Location</label><input id="help-location" name="location" value={form.location} onChange={updateField} maxLength={255} placeholder="Area or neighbourhood (avoid posting exact private addresses publicly)" required />
          <label htmlFor="help-description">Description</label><textarea id="help-description" name="description" value={form.description} onChange={updateField} maxLength={3000} rows={5} placeholder="Describe the help you need..." required />
          <button className="help-submit" type="submit" disabled={submitting}>{submitting ? "Submitting..." : "Submit request"}</button>
        </form>
      </div>
      <div className="help-list-section"><div className="help-list-heading"><div><h2>Community requests</h2><p>Browse requests or see the ones you submitted.</p></div></div>
        <div className="help-tabs" role="group" aria-label="Request filter"><button type="button" className={view === "open" ? "active" : ""} onClick={() => { setNotice(""); setView("open"); }}>Open requests</button><button type="button" className={view === "my" ? "active" : ""} onClick={() => { setNotice(""); setView("my"); }}>My requests</button></div>
        {notice && <p className="help-notice" role="status">{notice}</p>}{error && <p className="help-error" role="alert">{error}</p>}
        {loading ? <p className="help-empty">Loading requests...</p> : requests.length === 0 ? <div className="help-empty"><h3>No requests to show</h3><p>{view === "my" ? "Your submitted requests will appear here." : "There are no open requests to display right now."}</p></div> : <div className="help-request-list">{requests.map((request) => <article className="help-request-card" key={request.id}>
          <div className="help-request-top"><h3>{request.title || "Untitled request"}</h3><span className="help-status">{request.status || "UNKNOWN"}</span></div>
          <p className="help-request-description">{request.description || "No description provided."}</p>
          <div className="help-request-meta"><span>Priority: {request.priority || "NORMAL"}</span>{request.createdAt && <span>Created: {String(request.createdAt).replace("T", " ").slice(0, 16)}</span>}{request.location && <span>Location: {request.location}</span>}{request.category && <span>Category: {request.category}</span>}{request.createdByName && <span>Requested by: {request.createdByName}</span>}{request.assignedVolunteerName && <span>Assigned to: {request.assignedVolunteerName}</span>}</div>
          {actionButtons(request)}
          {historyOpen === request.id && <div className="request-history"><h4>Status history</h4>{!history[request.id] ? <p>Loading history…</p> : history[request.id].length === 0 ? <p>No history available.</p> : history[request.id].map((entry) => <div className="request-history-item" key={entry.id}><strong>{entry.fromStatus || "NEW"} → {entry.toStatus}</strong><p>{entry.note}</p><small>{entry.actorName} · {String(entry.createdAt).replace("T", " ").slice(0, 16)}</small></div>)}</div>}
        </article>)}</div>}
      </div>
    </section>
  </main>;
}
