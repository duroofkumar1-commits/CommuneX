import { useCallback, useEffect, useState } from "react";
import api from "../services/api";
import { errorMessage } from "../services/api";
import "./Notifications.css";

export default function Notifications() {
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async (signal, pageIndex = 0, append = false) => {
    if (append) setLoadingMore(true);
    else setLoading(true);
    setError("");
    try {
      const { data } = await api.get("/notifications/summary", {
        params: { page: pageIndex },
        signal,
      });
      if (signal?.aborted) return;
      const records = Array.isArray(data?.items) ? data.items : [];
      setItems((current) => append ? [...current, ...records] : records);
      setPage(pageIndex);
      setHasMore(records.length === 50);
      const unreadCount = Number(data?.unreadCount ?? 0);
      setUnread(unreadCount);
      window.dispatchEvent(new CustomEvent("communex:notifications-updated", {
        detail: { unreadCount },
      }));
    } catch (err) {
      if (err.code === "ERR_CANCELED" || signal?.aborted) return;
      setError(errorMessage(err, "Could not load notifications. Please try again."));
    } finally {
      if (!signal?.aborted) {
        setLoading(false);
        setLoadingMore(false);
      }
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  async function markRead(id) {
    const previousItems = items;
    const wasUnread = previousItems.some((item) => item.id === id && !item.read);
    setItems((current) => current.map((item) => item.id === id ? { ...item, read: true } : item));
    if (wasUnread) setUnread((count) => Math.max(0, count - 1));
    setError("");

    try {
      await api.patch(`/notifications/${id}/read`);
      // Fetch the authoritative count after a successful update so quick consecutive clicks cannot stale the badge.
      window.dispatchEvent(new CustomEvent("communex:notifications-updated"));
    } catch (err) {
      setError(errorMessage(err, "Could not mark this notification as read."));
      setItems(previousItems);
      if (wasUnread) setUnread((count) => count + 1);
    }
  }

  return (
    <main className="notifications-page">
      <header className="notifications-header">
        <div><p className="notifications-eyebrow">COMMUNEX COMMUNITY</p><h1>Notifications</h1><p>Stay updated with your community activity.</p></div>
        <button className="notifications-refresh" type="button" onClick={() => load()} disabled={loading || loadingMore}>Refresh</button>
      </header>
      <section className="notifications-panel">
        <div className="notifications-summary"><div><h2>Your updates</h2><p>Showing your latest {items.length} notifications.</p></div><span className="notifications-count">{unread} unread</span></div>
        {error && <p className="notifications-error" role="alert">{error}</p>}
        {loading ? (
          <p className="notifications-empty">Loading notifications...</p>
        ) : items.length === 0 ? (
          <div className="notifications-empty"><h3>You are all caught up!</h3><p>New notifications will appear here when available.</p></div>
        ) : (
          <div className="notifications-list">
            {items.map((item) => (
              <article className={`notification-card ${item.read ? "is-read" : "is-unread"}`} key={item.id}>
                <div className="notification-icon" aria-hidden="true">{item.read ? "✓" : "●"}</div>
                <div className="notification-content"><p>{item.message}</p>{item.createdAt && <time dateTime={item.createdAt}>{String(item.createdAt).replace("T", " ").slice(0, 16)}</time>}</div>
                {!item.read && <button className="notification-read-button" type="button" onClick={() => markRead(item.id)}>Mark as read</button>}
              </article>
            ))}
          </div>
        )}
        {!loading && hasMore && (
          <div style={{ display: "flex", justifyContent: "center", marginTop: 16 }}>
            <button
              className="notifications-refresh"
              type="button"
              onClick={() => load(undefined, page + 1, true)}
              disabled={loadingMore}
            >
              {loadingMore ? "Loading…" : "Load older notifications"}
            </button>
          </div>
        )}
      </section>
    </main>
  );
}
