import { useCallback, useEffect, useRef, useState } from "react";
import api, { errorMessage } from "../services/api";

export default function Messages() {
  const [withEmail, setWithEmail] = useState("");
  const [message, setMessage] = useState("");
  const [items, setItems] = useState([]);
  const [threads, setThreads] = useState([]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [threadsLoading, setThreadsLoading] = useState(true);
  const [conversationLoading, setConversationLoading] = useState(false);
  const [olderMessagesLoading, setOlderMessagesLoading] = useState(false);
  const [messagePage, setMessagePage] = useState(0);
  const [hasOlderMessages, setHasOlderMessages] = useState(false);
  const conversationRequest = useRef(null);
  const activeConversation = useRef("");

  const loadThreads = useCallback(async () => {
    setThreadsLoading(true);
    try {
      const { data } = await api.get("/messages/conversations");
      setThreads(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(errorMessage(err, "Could not load conversations."));
    } finally {
      setThreadsLoading(false);
    }
  }, []);

  const loadConversation = useCallback(async (email) => {
    const recipient = email.trim();
    if (!recipient) return;

    conversationRequest.current?.abort();
    const controller = new AbortController();
    conversationRequest.current = controller;
    activeConversation.current = recipient;
    setWithEmail(recipient);
    setOlderMessagesLoading(false);
    setItems([]);
    setError("");
    setNotice("");
    setConversationLoading(true);

    try {
      const { data } = await api.get("/messages", {
        params: { with: recipient, page: 0 },
        signal: controller.signal,
      });
      if (!controller.signal.aborted) {
        const records = Array.isArray(data) ? data : [];
        setItems(records);
        setMessagePage(0);
        setHasOlderMessages(records.length === 200);
      }
      // Marking messages read should not hold the conversation UI open.
      api.post("/messages/read", { withEmail: recipient })
        .then(() => window.dispatchEvent(new CustomEvent("communex:notifications-updated")))
        .catch(() => {});
      setThreads((current) => current.map((thread) => (
        thread.email === recipient ? { ...thread, unreadCount: 0 } : thread
      )));
    } catch (err) {
      if (err.code !== "ERR_CANCELED" && !controller.signal.aborted) {
        setError(errorMessage(err, "Could not load this conversation."));
      }
    } finally {
      if (!controller.signal.aborted) setConversationLoading(false);
    }
  }, []);

  async function loadOlderMessages() {
    const recipient = activeConversation.current;
    if (!recipient || olderMessagesLoading || !hasOlderMessages) return;
    const nextPage = messagePage + 1;
    setOlderMessagesLoading(true);
    setError("");
    try {
      const { data } = await api.get("/messages", {
        params: { with: recipient, page: nextPage },
      });
      // The user may have switched conversations while this request was in flight.
      if (activeConversation.current !== recipient) return;
      const older = Array.isArray(data) ? data : [];
      setItems((current) => [...older, ...current]);
      setMessagePage(nextPage);
      setHasOlderMessages(older.length === 200);
    } catch (err) {
      if (activeConversation.current === recipient) {
        setError(errorMessage(err, "Could not load earlier messages."));
      }
    } finally {
      if (activeConversation.current === recipient) setOlderMessagesLoading(false);
    }
  }

  useEffect(() => {
    loadThreads();
    return () => conversationRequest.current?.abort();
  }, [loadThreads]);

  async function send(event) {
    event.preventDefault();
    const recipient = withEmail.trim();
    const content = message.trim();
    if (!recipient || !content || busy) return;

    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { data } = await api.post("/messages", {
        recipientEmail: recipient,
        content,
      });
      if (activeConversation.current === recipient) {
        setItems((current) => [...current, data]);
      } else {
        // A new recipient can be typed and messaged without opening an existing thread first.
        loadConversation(recipient);
      }
      setMessage("");
      setNotice("Message sent.");
      // Refresh the conversation preview in the background; no second history download.
      loadThreads();
    } catch (err) {
      setError(errorMessage(err, "Could not send message."));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="section-stack">
      <div className="page-heading">
        <div><p className="eyebrow">DIRECT CONNECTIONS</p><h1>Messages</h1><p>Send private messages to other CommuneX members. Use respectful communication and report abuse when needed.</p></div>
        <span className="badge neutral">PRIVATE</span>
      </div>
      {error && <div className="status-message error" role="alert">{error}</div>}
      {notice && <div className="status-message success" role="status">{notice}</div>}
      <div className="two-column">
        <section className="surface panel">
          <div className="panel-title"><div><h2>Conversations</h2><p>Choose a recent contact or enter an email.</p></div><button className="btn ghost small-btn" type="button" onClick={loadThreads} disabled={threadsLoading}>Refresh</button></div>
          <form className="toolbar" onSubmit={(event) => { event.preventDefault(); loadConversation(withEmail); }}>
            <input className="field" type="email" required placeholder="Member email address" value={withEmail} onChange={(event) => setWithEmail(event.target.value)} />
            <button className="btn" type="submit" disabled={!withEmail.trim()}>Open</button>
          </form>
          <div className="divider" />
          {threadsLoading && threads.length === 0 ? (
            <div className="empty-state"><h3>Loading conversations…</h3></div>
          ) : threads.length === 0 ? (
            <div className="empty-state"><span className="empty-symbol">✉</span><h3>No conversations yet</h3><p>Start a conversation using a member's email address.</p></div>
          ) : (
            <div className="data-list">
              {threads.map((thread) => (
                <button key={thread.email} type="button" className="data-card" onClick={() => loadConversation(thread.email)} style={{ textAlign: "left" }}>
                  <div className="data-card-head"><h3>{thread.name || thread.email}</h3>{thread.unreadCount > 0 && <span className="badge warning">{thread.unreadCount} unread</span>}</div>
                  <p>{thread.lastMessage || "Open conversation"}</p><span className="data-meta">{thread.email}</span>
                </button>
              ))}
            </div>
          )}
        </section>
        <section className="surface panel">
          <div className="panel-title"><div><h2>{withEmail ? `Conversation with ${withEmail}` : "Message thread"}</h2><p>Only participants can read their conversation.</p></div></div>
          <div style={{ minHeight: 270, maxHeight: 410, overflowY: "auto", display: "flex", flexDirection: "column", gap: 4 }} aria-live="polite">
            {hasOlderMessages && !conversationLoading && (
              <button type="button" className="btn ghost small-btn" onClick={loadOlderMessages} disabled={olderMessagesLoading}>
                {olderMessagesLoading ? "Loading earlier messages…" : "Load earlier messages"}
              </button>
            )}
            {conversationLoading ? (
              <div className="empty-state"><h3>Loading messages…</h3></div>
            ) : items.length === 0 ? (
              <div className="empty-state"><h3>{withEmail ? "No messages yet" : "Select a conversation"}</h3><p>{withEmail ? "Send the first message below." : "Choose a contact from the left panel."}</p></div>
            ) : items.map((item) => (
              <div className={`message-bubble ${item.senderEmail === localStorage.getItem("communex.email") ? "me" : ""}`} key={item.id}>
                <strong>{item.senderName || item.senderEmail}</strong><p>{item.content}</p><span className="message-meta">{item.sentAt ? new Date(item.sentAt).toLocaleString() : ""}</span>
              </div>
            ))}
          </div>
          <div className="divider" />
          <form className="form-stack" onSubmit={send}>
            <label><span className="field-label">Your message</span><textarea className="field-textarea" required maxLength={4000} value={message} onChange={(event) => setMessage(event.target.value)} placeholder="Write a message…" /></label>
            <button className="btn" type="submit" disabled={busy || !withEmail.trim() || !message.trim()}>{busy ? "Sending…" : "Send message"}</button>
          </form>
        </section>
      </div>
    </div>
  );
}
