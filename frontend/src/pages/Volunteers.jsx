import { useCallback, useEffect, useRef, useState } from "react";
import { useOutletContext } from "react-router-dom";
import api, { errorMessage } from "../services/api";

const PAGE_SIZE = 40;

export default function Volunteers() {
  const { user, setUser } = useOutletContext();
  const [profile, setProfile] = useState(null);
  const [items, setItems] = useState([]);
  const [query, setQuery] = useState("");
  const [skills, setSkills] = useState("");
  const [bio, setBio] = useState("");
  const [available, setAvailable] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [listBusy, setListBusy] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const listRequest = useRef(null);

  const loadList = useCallback(async ({ search = "", pageIndex = 0, append = false } = {}) => {
    listRequest.current?.abort();
    const controller = new AbortController();
    listRequest.current = controller;
    setListBusy(true);
    try {
      const params = { page: pageIndex, size: PAGE_SIZE };
      if (search.trim()) params.skills = search.trim();
      const { data } = await api.get("/volunteers/available", {
        params,
        signal: controller.signal,
      });
      if (controller.signal.aborted) return;
      const records = Array.isArray(data) ? data : [];
      setItems((current) => append ? [...current, ...records] : records);
      setPage(pageIndex);
      setHasMore(records.length === PAGE_SIZE);
    } catch (err) {
      if (err.code !== "ERR_CANCELED" && !controller.signal.aborted) {
        setError(errorMessage(err, "Could not load volunteer directory."));
      }
    } finally {
      if (!controller.signal.aborted) setListBusy(false);
    }
  }, []);

  const loadProfile = useCallback(async () => {
    try {
      const { data } = await api.get("/volunteers/me");
      setProfile(data);
      setSkills(data.skills || "");
      setBio(data.bio || "");
      setAvailable(Boolean(data.available));
    } catch (err) {
      if (err.response?.status !== 404 && err.response?.status !== 403) {
        setError(errorMessage(err, "Could not load volunteer profile."));
      }
    }
  }, []);

  useEffect(() => {
    loadList();
    return () => listRequest.current?.abort();
  }, [loadList]);

  useEffect(() => {
    if (user?.role === "VOLUNTEER") loadProfile();
  }, [loadProfile, user?.role]);

  async function register(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { data } = await api.post("/volunteers/register", { skills: skills.trim(), bio: bio.trim() });
      setProfile(data);
      setUser((current) => current ? { ...current, role: "VOLUNTEER" } : current);
      setNotice("Volunteer profile created. Welcome to the volunteer network.");
      await loadList({ search: query });
    } catch (err) {
      setError(errorMessage(err, "Could not create volunteer profile."));
    } finally {
      setBusy(false);
    }
  }

  async function update(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { data } = await api.put("/volunteers/me", { skills: skills.trim(), bio: bio.trim(), available });
      setProfile(data);
      setNotice("Volunteer profile updated.");
      await loadList({ search: query });
    } catch (err) {
      setError(errorMessage(err, "Could not update volunteer profile."));
    } finally {
      setBusy(false);
    }
  }

  async function search(event) {
    event.preventDefault();
    setError("");
    await loadList({ search: query, pageIndex: 0 });
  }

  async function clearSearch() {
    setQuery("");
    setError("");
    await loadList({ search: "", pageIndex: 0 });
  }

  const isVolunteer = user?.role === "VOLUNTEER";

  return (
    <div className="section-stack">
      <div className="page-heading"><div><p className="eyebrow">PEOPLE POWERING THE COMMUNITY</p><h1>Volunteer network</h1><p>Discover skills and availability. User contact details are not exposed in the directory; use messaging only where you know the recipient email and contact is permitted.</p></div><span className="badge">COMMUNITY SUPPORT</span></div>
      {error && <div className="status-message error" role="alert">{error}</div>}
      {notice && <div className="status-message success" role="status">{notice}</div>}
      {!isVolunteer ? (
        <section className="surface panel form-stack"><div className="panel-title"><div><h2>Become a volunteer</h2><p>Tell your community how you can help.</p></div><span className="feature-icon">♧</span></div>
          <form className="form-stack" onSubmit={register}><label><span className="field-label">Skills</span><input className="field" value={skills} onChange={(event) => setSkills(event.target.value)} required maxLength={1000} placeholder="e.g. first aid, tutoring, transport" /></label><label><span className="field-label">Bio</span><textarea className="field-textarea" value={bio} onChange={(event) => setBio(event.target.value)} maxLength={2000} placeholder="What would you like to contribute?" /></label><button className="btn" disabled={busy}>{busy ? "Saving…" : "Register as volunteer"}</button></form>
          <p className="muted small">Registration changes your platform role to VOLUNTEER. The backend performs that role transition; the form alone cannot grant roles.</p>
        </section>
      ) : (
        <section className="surface panel form-stack"><div className="panel-title"><div><h2>My volunteer profile</h2><p>Keep your skills and availability up to date.</p></div><span className={`badge ${available ? "" : "neutral"}`}>{available ? "AVAILABLE" : "NOT AVAILABLE"}</span></div>
          <form className="form-stack" onSubmit={update}><label><span className="field-label">Skills</span><input className="field" value={skills} onChange={(event) => setSkills(event.target.value)} required maxLength={1000} /></label><label><span className="field-label">Bio</span><textarea className="field-textarea" value={bio} onChange={(event) => setBio(event.target.value)} maxLength={2000} /></label><label className="small" style={{ display: "flex", gap: 8, alignItems: "center" }}><input type="checkbox" checked={available} onChange={(event) => setAvailable(event.target.checked)} /> Available to be contacted for appropriate community opportunities</label><button className="btn" disabled={busy}>{busy ? "Saving…" : "Update profile"}</button></form>
          {profile && <p className="muted small">Profile created {profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : "on CommuneX"}.</p>}
        </section>
      )}
      <section className="surface panel">
        <div className="panel-title"><div><h2>Find volunteers</h2><p>Showing {items.length} available profile(s)</p></div><span className="badge neutral">LIVE DIRECTORY</span></div>
        <form className="toolbar" onSubmit={search}><input className="field" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search skill keyword…" /><button className="btn" type="submit" disabled={listBusy}>{listBusy ? "Loading…" : "Search"}</button><button type="button" className="btn ghost" onClick={clearSearch} disabled={listBusy}>Clear</button></form>
        <div className="divider" />
        {listBusy && items.length === 0 ? <div className="empty-state"><h3>Loading volunteers…</h3></div> : items.length === 0 ? <div className="empty-state"><span className="empty-symbol">♧</span><h3>No matching volunteers</h3><p>Results reflect profiles marked available in CommuneX.</p></div> : <div className="feature-grid">{items.map((volunteer) => <article className="data-card" key={volunteer.id}><div className="data-card-head"><div className="feature-icon">{(volunteer.name || "V").slice(0, 1).toUpperCase()}</div><span className="badge">AVAILABLE</span></div><h3 style={{ marginTop: 12 }}>{volunteer.name}</h3><p><strong>Skills</strong> · {volunteer.skills}</p><p>{volunteer.bio || "No bio provided."}</p><span className="data-meta">Volunteer profile #{volunteer.id}</span></article>)}</div>}
        {hasMore && <div style={{ display: "flex", justifyContent: "center", marginTop: 16 }}><button type="button" className="btn secondary" disabled={listBusy} onClick={() => loadList({ search: query, pageIndex: page + 1, append: true })}>{listBusy ? "Loading…" : "Load more volunteers"}</button></div>}
      </section>
    </div>
  );
}
