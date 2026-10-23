import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api, { errorMessage } from "../services/api";

const MIN_QUERY_LENGTH = 2;
const SEARCH_DEBOUNCE_MS = 300;

export default function Search() {
  const [query, setQuery] = useState("");
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState("");
  const requestRef = useRef(null);
  const debounceTimer = useRef(null);

  const search = useCallback(async (value) => {
    const term = value.trim();
    if (term.length < MIN_QUERY_LENGTH) {
      requestRef.current?.abort();
      setItems([]);
      setError("");
      setBusy(false);
      setSearched(false);
      return;
    }

    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    setBusy(true);
    setError("");
    setSearched(true);

    try {
      const { data } = await api.get("/search", {
        params: { q: term },
        signal: controller.signal,
      });
      if (!controller.signal.aborted) setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err.code !== "ERR_CANCELED" && !controller.signal.aborted) {
        setError(errorMessage(err, "Search is unavailable."));
        setItems([]);
      }
    } finally {
      if (!controller.signal.aborted) setBusy(false);
    }
  }, []);

  useEffect(() => {
    requestRef.current?.abort();
    const term = query.trim();
    if (term.length < MIN_QUERY_LENGTH) {
      requestRef.current?.abort();
      setItems([]);
      setError("");
      setBusy(false);
      setSearched(false);
      return undefined;
    }

    debounceTimer.current = window.setTimeout(() => search(term), SEARCH_DEBOUNCE_MS);
    return () => {
      window.clearTimeout(debounceTimer.current);
      debounceTimer.current = null;
    };
  }, [query, search]);

  useEffect(() => () => requestRef.current?.abort(), []);

  function submit(event) {
    event.preventDefault();
    window.clearTimeout(debounceTimer.current);
    debounceTimer.current = null;
    search(query);
  }

  return (
    <div className="section-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">ONE SEARCH, MANY MODULES</p>
          <h1>Global search</h1>
          <p>Search facility records, community posts, resources and events visible to your account.</p>
        </div>
        <span className="badge neutral">CROSS-MODULE</span>
      </div>
      {error && <div className="status-message info" role="alert">{error}</div>}
      <section className="surface panel">
        <form className="toolbar" onSubmit={submit}>
          <input className="field" style={{ maxWidth: "none", flex: 1 }} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Try clinic, volunteer, food…" aria-label="Search CommuneX" />
          <button className="btn" disabled={busy || query.trim().length < MIN_QUERY_LENGTH}>{busy ? "Searching…" : "Search"}</button>
        </form>
        <div className="divider" />
        <div className="panel-title"><div><h2>Results</h2><p>{busy ? "Searching…" : `${items.length} result(s)`}</p></div></div>
        {!searched ? (
          <div className="empty-state"><span className="empty-symbol">⌕</span><h3>What are you looking for?</h3><p>Type at least two characters to search configured modules.</p></div>
        ) : items.length === 0 && !busy ? (
          <div className="empty-state"><h3>No matching records</h3><p>Try another phrase or browse a module directly.</p></div>
        ) : (
          <div className="data-list">
            {items.map((item, index) => (
              <article className="data-card" key={`${item.type}-${item.id}-${index}`}>
                <div className="data-card-head">
                  <div><div className="data-meta"><span className="badge neutral">{item.type}</span></div><h3 style={{ marginTop: 7 }}>{item.title}</h3></div>
                  <Link className="btn secondary small-btn" to={item.href || "/dashboard"}>Open ↗</Link>
                </div>
                <p>{item.description || "No description available."}</p>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
