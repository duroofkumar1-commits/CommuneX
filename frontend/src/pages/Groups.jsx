import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import api, { errorMessage } from "../services/api";
import "./Groups.css";

const blankGroup = { name: "", description: "", visibility: "PUBLIC" };
const reactionOptions = ["👍", "❤️", "😂", "🎉", "🙏"];
const CHAT_PAGE_SIZE = 60;
const MAX_CACHED_MESSAGES = 500;
const POLL_FAST_MS = 3500;
const POLL_IDLE_MS = 8500;
const POLL_HIDDEN_MS = 15000;

function messageSort(a, b) {
  const aLocal = String(a.id).startsWith("local-");
  const bLocal = String(b.id).startsWith("local-");
  if (aLocal !== bLocal) return aLocal ? 1 : -1;
  if (!aLocal && !bLocal) {
    const aId = Number(a.id);
    const bId = Number(b.id);
    if (Number.isFinite(aId) && Number.isFinite(bId) && aId !== bId) return aId - bId;
  }
  const timeDifference = new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime();
  if (Number.isFinite(timeDifference) && timeDifference !== 0) return timeDifference;
  const aId = Number(a.id);
  const bId = Number(b.id);
  if (Number.isFinite(aId) && Number.isFinite(bId)) return aId - bId;
  return String(a.id).localeCompare(String(b.id));
}

function mergeMessages(existing, incoming) {
  const byId = new Map(existing.map((message) => [String(message.id), message]));
  for (const serverMessage of incoming) {
    const pendingMatch = [...byId.values()].find((message) =>
      String(message.id).startsWith("local-") &&
      message.senderId != null && String(message.senderId) === String(serverMessage.senderId) &&
      message.content === serverMessage.content &&
      ["sending", "failed"].includes(message.deliveryStatus),
    );
    if (pendingMatch) byId.delete(String(pendingMatch.id));
    const current = byId.get(String(serverMessage.id));
    byId.set(String(serverMessage.id), {
      ...current,
      ...serverMessage,
      reactions: Array.isArray(serverMessage.reactions) ? serverMessage.reactions : current?.reactions || [],
      deliveryStatus: "sent",
    });
  }
  return [...byId.values()].sort(messageSort).slice(-MAX_CACHED_MESSAGES);
}

function initials(name = "Community") {
  return name.trim().split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("") || "C";
}

function messageTime(value) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

export default function Groups() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(blankGroup);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [messages, setMessages] = useState([]);
  const [messageText, setMessageText] = useState("");
  const [replyTo, setReplyTo] = useState(null);
  const [search, setSearch] = useState("");
  const [currentUser, setCurrentUser] = useState(null);
  const [requestsForGroup, setRequestsForGroup] = useState(null);
  const [joinRequests, setJoinRequests] = useState([]);
  const [requestsBusy, setRequestsBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [chatLoading, setChatLoading] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [hasOlderMessages, setHasOlderMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [pollStatus, setPollStatus] = useState("connected");

  // Cache only lives inside this authenticated page instance, never in shared module/global storage.
  const messageCacheRef = useRef(new Map());
  const activeGroupRef = useRef(null);
  const messagesRef = useRef([]);
  const sendingRef = useRef(false);
  const chatScrollRef = useRef(null);
  const nearBottomRef = useRef(true);

  const cacheKey = useCallback((groupId) => `${currentUser?.id ?? "anon"}:${groupId}`, [currentUser?.id]);

  const commitMessages = useCallback((groupId, incoming) => {
    const key = cacheKey(groupId);
    const previous = messageCacheRef.current.get(key) || [];
    const next = mergeMessages(previous, incoming);
    messageCacheRef.current.set(key, next);
    if (String(activeGroupRef.current?.id) === String(groupId)) {
      messagesRef.current = next;
      setMessages(next);
    }
    return next;
  }, [cacheKey]);

  const loadGroups = useCallback(async (quiet = false) => {
    try {
      const { data } = await api.get("/groups");
      setItems(Array.isArray(data) ? data : []);
      if (!quiet) setError("");
    } catch (e) {
      setError(errorMessage(e, "Groups API is unavailable."));
    }
  }, []);

  useEffect(() => {
    let alive = true;
    Promise.allSettled([api.get("/users/me"), api.get("/groups")]).then(([userResult, groupResult]) => {
      if (!alive) return;
      if (userResult.status === "fulfilled") setCurrentUser(userResult.value.data);
      else setError(errorMessage(userResult.reason, "Could not load your account details."));
      if (groupResult.status === "fulfilled") setItems(Array.isArray(groupResult.value.data) ? groupResult.value.data : []);
      else setError(errorMessage(groupResult.reason, "Groups API is unavailable."));
    });
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    messagesRef.current = messages;
  }, [messages]);

  useEffect(() => {
    if (!nearBottomRef.current) return;
    const frame = requestAnimationFrame(() => {
      const node = chatScrollRef.current;
      if (node) node.scrollTop = node.scrollHeight;
    });
    return () => cancelAnimationFrame(frame);
  }, [messages]);

  const requestMessages = useCallback(async (group, params = { limit: CHAT_PAGE_SIZE }) => {
    if (!group?.id) return [];
    const { data } = await api.get(`/groups/${group.id}/messages`, { params });
    return Array.isArray(data) ? data : [];
  }, []);

  const loadMessages = useCallback(async (group, { initial = false, quiet = false } = {}) => {
    if (!group?.id) return;
    if (initial) {
      const cached = messageCacheRef.current.get(cacheKey(group.id)) || [];
      messagesRef.current = cached;
      setMessages(cached);
      setChatLoading(cached.length === 0);
    }
    try {
      const incoming = await requestMessages(group, { limit: CHAT_PAGE_SIZE });
      if (initial && String(activeGroupRef.current?.id) === String(group.id)) {
        setHasOlderMessages(incoming.length >= CHAT_PAGE_SIZE);
      }
      commitMessages(group.id, incoming);
      setPollStatus("connected");
      if (!quiet) setError("");
    } catch (e) {
      setPollStatus("offline");
      if (!quiet) setError(errorMessage(e, "Could not load this group's chat."));
    } finally {
      if (initial) setChatLoading(false);
    }
  }, [cacheKey, commitMessages, requestMessages]);

  async function createGroup(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { data } = await api.post("/groups", {
        name: form.name.trim(),
        description: form.description.trim(),
        visibility: form.visibility,
      });
      setNotice("Group created. Its chat is ready to open.");
      setShowCreate(false);
      setForm(blankGroup);
      await loadGroups(true);
      if (data?.id) openChat(data);
    } catch (e) {
      setError(errorMessage(e, "Could not create group."));
    } finally {
      setBusy(false);
    }
  }

  async function joinGroup(group) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const { data } = await api.post(`/groups/${group.id}/join`);
      setNotice(data.status === "PENDING"
        ? `Join request sent for ${group.name}. The owner must approve it before you can chat.`
        : `You joined ${group.name}.`);
      await loadGroups(true);
    } catch (e) {
      setError(errorMessage(e, "Could not join group."));
    } finally {
      setBusy(false);
    }
  }

  async function leaveGroup(group) {
    setBusy(true);
    setError("");
    try {
      await api.delete(`/groups/${group.id}/membership`);
      if (String(activeGroupRef.current?.id) === String(group.id)) {
        activeGroupRef.current = null;
        setSelectedGroup(null);
        setMessages([]);
        messagesRef.current = [];
      }
      messageCacheRef.current.delete(cacheKey(group.id));
      setNotice(`You left ${group.name}.`);
      await loadGroups(true);
    } catch (e) {
      setError(errorMessage(e, "Could not leave group."));
    } finally {
      setBusy(false);
    }
  }

  function openChat(group) {
    activeGroupRef.current = group;
    setSelectedGroup(group);
    setMessageText("");
    setReplyTo(null);
    setHasOlderMessages(false);
    setError("");
    nearBottomRef.current = true;
    void loadMessages(group, { initial: true });
  }


  async function loadOlderMessages() {
    const group = activeGroupRef.current;
    if (!group?.id || loadingOlder || messagesRef.current.length >= MAX_CACHED_MESSAGES) {
      setHasOlderMessages(false);
      return;
    }
    const serverMessages = messagesRef.current.filter((message) => !String(message.id).startsWith("local-") && Number.isFinite(Number(message.id)));
    if (serverMessages.length === 0) {
      setHasOlderMessages(false);
      return;
    }
    const beforeId = serverMessages.reduce((lowest, message) => Math.min(lowest, Number(message.id)), Number.POSITIVE_INFINITY);
    const node = chatScrollRef.current;
    const previousHeight = node?.scrollHeight || 0;
    const previousTop = node?.scrollTop || 0;
    setLoadingOlder(true);
    try {
      const older = await requestMessages(group, { beforeId, limit: CHAT_PAGE_SIZE });
      if (older.length === 0) {
        setHasOlderMessages(false);
        return;
      }
      commitMessages(group.id, older);
      setHasOlderMessages(older.length >= CHAT_PAGE_SIZE && messagesRef.current.length < MAX_CACHED_MESSAGES);
      requestAnimationFrame(() => {
        if (node) node.scrollTop = previousTop + (node.scrollHeight - previousHeight);
      });
    } catch (e) {
      setError(errorMessage(e, "Could not load earlier messages."));
    } finally {
      setLoadingOlder(false);
    }
  }

  async function reviewJoinRequests(group) {
    if (requestsForGroup === group.id) {
      setRequestsForGroup(null);
      setJoinRequests([]);
      return;
    }
    setRequestsBusy(true);
    setError("");
    try {
      const { data } = await api.get(`/groups/${group.id}/join-requests`);
      setJoinRequests(Array.isArray(data) ? data : []);
      setRequestsForGroup(group.id);
    } catch (e) {
      setError(errorMessage(e, "Could not load group join requests."));
    } finally {
      setRequestsBusy(false);
    }
  }

  async function approveJoinRequest(request) {
    setRequestsBusy(true);
    setError("");
    try {
      await api.put(`/groups/memberships/${request.id}/approve`);
      setNotice(`${request.userName || "Member"} was approved to join the group.`);
      const group = items.find((item) => String(item.id) === String(request.groupId));
      if (group) {
        const { data } = await api.get(`/groups/${request.groupId}/join-requests`);
        setJoinRequests(Array.isArray(data) ? data : []);
      }
      await loadGroups(true);
    } catch (e) {
      setError(errorMessage(e, "Could not approve the join request."));
    } finally {
      setRequestsBusy(false);
    }
  }

  function updateLocalMessage(messageId, update) {
    const groupId = activeGroupRef.current?.id;
    if (!groupId) return;
    const key = cacheKey(groupId);
    const current = messageCacheRef.current.get(key) || [];
    const next = current.map((message) => String(message.id) === String(messageId)
      ? { ...message, ...update(message) }
      : message);
    messageCacheRef.current.set(key, next);
    messagesRef.current = next;
    setMessages(next);
  }

  async function sendMessageText(rawText, retryMessage = null, replyTarget = replyTo) {
    const group = activeGroupRef.current;
    const content = rawText.trim();
    if (!group?.id || !content || sendingRef.current) return;
    sendingRef.current = true;
    setSending(true);
    setError("");

    const localId = retryMessage?.id || `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    const optimistic = {
      id: localId,
      groupId: group.id,
      senderId: currentUser?.id ?? null,
      senderName: currentUser?.name || "You",
      content,
      parentMessageId: retryMessage?.parentMessageId ?? replyTarget?.id ?? null,
      createdAt: retryMessage?.createdAt || new Date().toISOString(),
      reactions: retryMessage?.reactions || [],
      deliveryStatus: "sending",
    };
    if (retryMessage) {
      updateLocalMessage(localId, () => ({ deliveryStatus: "sending" }));
    } else {
      commitMessages(group.id, [optimistic]);
    }

    try {
      const payload = { content, parentMessageId: optimistic.parentMessageId || null };
      const { data } = await api.post(`/groups/${group.id}/messages`, payload);
      commitMessages(group.id, [{ ...data, deliveryStatus: "sent" }]);
      setMessageText("");
      setPollStatus("connected");
    } catch (e) {
      updateLocalMessage(localId, () => ({ deliveryStatus: "failed" }));
      setError(errorMessage(e, "Message was not accepted by the server. Check the chat API error and try again."));
    } finally {
      sendingRef.current = false;
      setSending(false);
    }
  }

  async function sendMessage(event) {
    event.preventDefault();
    if (sendingRef.current) return;
    const content = messageText.trim();
    if (!content) return;
    const replyTarget = replyTo;
    setMessageText("");
    setReplyTo(null);
    await sendMessageText(content, null, replyTarget);
  }

  async function retryMessage(message) {
    await sendMessageText(message.content, message, null);
  }

  async function toggleReaction(message, emoji) {
    const group = activeGroupRef.current;
    if (!group || String(message.id).startsWith("local-")) return;
    const existing = message.reactions?.find((item) => item.emoji === emoji);
    const selected = Boolean(existing?.reactedByMe);
    const optimisticReactions = [...(message.reactions || [])];
    const index = optimisticReactions.findIndex((item) => item.emoji === emoji);
    if (selected && index >= 0) {
      if (optimisticReactions[index].count <= 1) optimisticReactions.splice(index, 1);
      else optimisticReactions[index] = { ...optimisticReactions[index], count: optimisticReactions[index].count - 1, reactedByMe: false };
    } else if (index >= 0) {
      optimisticReactions[index] = { ...optimisticReactions[index], count: optimisticReactions[index].count + 1, reactedByMe: true };
    } else {
      optimisticReactions.push({ emoji, count: 1, reactedByMe: true });
    }
    updateLocalMessage(message.id, () => ({ reactions: optimisticReactions }));
    try {
      const config = { params: { emoji } };
      if (selected) await api.delete(`/groups/messages/${message.id}/reactions`, config);
      else await api.post(`/groups/messages/${message.id}/reactions`, null, config);
    } catch (e) {
      updateLocalMessage(message.id, () => ({ reactions: message.reactions || [] }));
      setError(errorMessage(e, "Could not update the reaction."));
      if (activeGroupRef.current) void loadMessages(activeGroupRef.current, { quiet: true });
    }
  }

  // Incremental polling: ask only for messages newer than the current highest server ID.
  // Every fourth visible poll fetches the recent window too, so reactions by other members sync.
  useEffect(() => {
    const group = selectedGroup;
    if (!group?.id) return undefined;
    let cancelled = false;
    let timer;
    let tick = 0;
    let delay = POLL_FAST_MS;

    async function poll() {
      if (cancelled) return;
      if (document.visibilityState === "hidden") {
        timer = window.setTimeout(poll, POLL_HIDDEN_MS);
        return;
      }
      tick += 1;
      const key = cacheKey(group.id);
      const cached = messageCacheRef.current.get(key) || [];
      const serverIds = cached.map((m) => Number(m.id)).filter(Number.isFinite);
      const afterId = serverIds.reduce((maxId, id) => Math.max(maxId, id), 0);
      const refreshRecent = tick % 4 === 0;
      try {
        const params = refreshRecent ? { limit: CHAT_PAGE_SIZE } : { afterId, limit: 100 };
        const { data } = await api.get(`/groups/${group.id}/messages`, { params });
        const incoming = Array.isArray(data) ? data : [];
        if (!cancelled) {
          commitMessages(group.id, incoming);
          setPollStatus("connected");
          delay = incoming.length ? POLL_FAST_MS : Math.min(delay + 1000, POLL_IDLE_MS);
        }
      } catch {
        if (!cancelled) {
          setPollStatus("offline");
          delay = Math.min(Math.max(delay * 2, POLL_IDLE_MS), 30000);
        }
      } finally {
        if (!cancelled) timer = window.setTimeout(poll, delay);
      }
    }

    timer = window.setTimeout(poll, POLL_FAST_MS);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [selectedGroup, cacheKey, commitMessages]);

  const filteredGroups = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return items;
    return items.filter((group) => `${group.name} ${group.description || ""} ${group.ownerName || ""}`.toLowerCase().includes(query));
  }, [items, search]);

  const membershipLabel = (group) => {
    if (group.ownedByMe) return "Owner";
    if (group.membershipStatus === "PENDING") return "Request pending";
    if (group.memberOfMe) return "Member";
    return group.visibility === "PRIVATE" ? "Private" : "Public";
  };

  return (
    <div className="groups-page">
      <header className="groups-page-heading">
        <div>
          <p className="eyebrow">YOUR COMMUNITY, IN ONE PLACE</p>
          <h1>Community groups</h1>
          <p>Discover a group, join the conversation, and keep your community connected.</p>
        </div>
        <button className="btn groups-create-trigger" type="button" onClick={() => setShowCreate((value) => !value)}>
          {showCreate ? "Cancel" : "+ Create group"}
        </button>
      </header>

      {error && <div className="status-message error groups-alert" role="alert">{error}</div>}
      {notice && <div className="status-message success groups-alert" role="status">{notice}</div>}

      {showCreate && (
        <form className="surface groups-create-form" onSubmit={createGroup}>
          <div className="groups-form-heading"><div><span className="groups-section-kicker">START A CONVERSATION</span><h2>Create a group</h2></div></div>
          <div className="groups-form-grid">
            <label className="form-field"><span className="field-label">Group name</span><input className="field" required maxLength={120} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="e.g. Mithi Community Updates" /></label>
            <label className="form-field"><span className="field-label">Visibility</span><select className="field-select" value={form.visibility} onChange={(event) => setForm({ ...form, visibility: event.target.value })}><option value="PUBLIC">Public group</option><option value="PRIVATE">Private — owner approval required</option></select></label>
            <label className="form-field groups-form-full"><span className="field-label">Purpose</span><textarea className="field-textarea" required maxLength={1500} rows={2} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder="What will members discuss here?" /></label>
          </div>
          <div className="groups-form-actions"><span className="muted small">Choose a clear name so people can find your group.</span><button className="btn" disabled={busy}>{busy ? "Creating…" : "Create group"}</button></div>
        </form>
      )}

      <div className={`groups-chat-layout ${selectedGroup ? "has-active-chat" : ""}`}>
        <aside className="groups-sidebar surface" aria-label="Community group list">
          <div className="groups-sidebar-head"><div><span className="groups-section-kicker">MESSAGES</span><h2>Your groups</h2><p>{items.length} communities to explore</p></div><span className="groups-count-badge">{items.length}</span></div>
          <label className="groups-search"><span aria-hidden="true">⌕</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search groups" aria-label="Search groups" /><kbd>⌘ K</kbd></label>
          <div className="groups-list" role="list">
            {filteredGroups.length === 0 ? <div className="groups-list-empty"><span>◎</span><strong>No matching groups</strong><p>Try another search or create a group.</p></div> : filteredGroups.map((group) => {
              const canChat = Boolean(group.ownedByMe || group.memberOfMe);
              const isSelected = String(selectedGroup?.id) === String(group.id);
              return (
                <article className={`groups-list-item ${isSelected ? "is-selected" : ""}`} key={group.id} role="listitem">
                  <button type="button" className="groups-list-open" onClick={() => canChat ? openChat(group) : group.visibility === "PUBLIC" ? joinGroup(group) : joinGroup(group)} aria-label={canChat ? `Open ${group.name} chat` : `Join ${group.name}`}>
                    <span className={`groups-avatar ${group.visibility === "PRIVATE" ? "is-private" : ""}`}>{initials(group.name)}</span>
                    <span className="groups-list-copy"><span className="groups-list-title">{group.name}</span><span className="groups-list-preview">{group.description || "Community group"}</span><span className="groups-list-meta">{group.memberCount || 0} members · {membershipLabel(group)}</span></span>
                    <span className="groups-list-indicator">{isSelected ? "●" : canChat ? "›" : "＋"}</span>
                  </button>
                  {group.ownedByMe && group.pendingRequestCount > 0 && <button type="button" className="groups-request-link" onClick={() => reviewJoinRequests(group)} disabled={requestsBusy}>{requestsForGroup === group.id ? "Hide join requests" : `Review ${group.pendingRequestCount} request(s)`}</button>}
                  {requestsForGroup === group.id && <div className="groups-requests-box">{joinRequests.length === 0 ? <p>No pending requests.</p> : joinRequests.map((request) => <div className="groups-request-row" key={request.id}><span>{request.userName || request.userEmail || `User #${request.userId}`}</span><button type="button" onClick={() => approveJoinRequest(request)} disabled={requestsBusy}>{requestsBusy ? "…" : "Approve"}</button></div>)}</div>}
                  {canChat && !group.ownedByMe && <button type="button" className="groups-leave-link" onClick={() => leaveGroup(group)} disabled={busy}>Leave group</button>}
                </article>
              );
            })}
          </div>
          <div className="groups-sidebar-footer"><span className="groups-online-dot" /> Signed in as <strong>{currentUser?.name || "community member"}</strong></div>
        </aside>

        <section className="groups-chat-pane surface" aria-label={selectedGroup ? `Chat for ${selectedGroup.name}` : "Group chat"}>
          {!selectedGroup ? (
            <div className="groups-chat-welcome"><div className="groups-welcome-art"><span>✳</span><span>☏</span><span>✦</span></div><h2>Good conversations build good communities.</h2><p>Select one of your joined groups to read messages and share an update, or choose a public group from the list to join.</p><button type="button" className="btn" onClick={() => setShowCreate(true)}>Create your first group</button><span className="groups-encryption-note">A little kindness goes a long way.</span></div>
          ) : (
            <>
              <header className="groups-chat-header"><span className="groups-avatar groups-header-avatar">{initials(selectedGroup.name)}</span><div className="groups-chat-header-copy"><h2>{selectedGroup.name}</h2><p><span className="groups-online-dot" /> {selectedGroup.memberCount || 1} members · {selectedGroup.visibility === "PRIVATE" ? "Private group" : "Community group"}</p></div><div className="groups-chat-header-actions"><span className={`groups-sync-state ${pollStatus === "connected" ? "is-connected" : "is-offline"}`}><span />{pollStatus === "connected" ? "Live updates" : "Reconnecting…"}</span><button type="button" className="groups-icon-button" title="Refresh messages" aria-label="Refresh messages" onClick={() => loadMessages(selectedGroup, { quiet: false })}>↻</button><button type="button" className="groups-icon-button groups-close-chat" title="Close chat" aria-label="Close chat" onClick={() => { activeGroupRef.current = null; setSelectedGroup(null); setMessages([]); messagesRef.current = []; }}>×</button></div></header>

              <div className="groups-chat-subheader"><span>✦</span> Keep the conversation helpful, respectful, and on topic.</div>
              <div className="groups-chat-scroll" ref={chatScrollRef} onScroll={(event) => { const node = event.currentTarget; nearBottomRef.current = node.scrollHeight - node.scrollTop - node.clientHeight < 100; }}>
                {hasOlderMessages && <div className="groups-load-older-wrap"><button type="button" className="groups-load-older" onClick={loadOlderMessages} disabled={loadingOlder}>{loadingOlder ? "Loading older messages…" : "↑ Load earlier messages"}</button></div>}
                {chatLoading && messages.length === 0 ? <div className="groups-chat-state"><span className="groups-loader" /><p>Loading conversation…</p></div> : messages.length === 0 ? <div className="groups-chat-empty"><span>☏</span><h3>No messages yet</h3><p>Say hello and get the conversation started.</p></div> : <div className="groups-message-stack">{messages.map((message, index) => {
                  const mine = (currentUser?.id != null && String(message.senderId) === String(currentUser.id)) || (currentUser?.id == null && message.senderName === "You");
                  const isLocal = String(message.id).startsWith("local-");
                  const previous = messages[index - 1];
                  const showDay = !previous || new Date(previous.createdAt).toDateString() !== new Date(message.createdAt).toDateString();
                  const reacted = new Map((message.reactions || []).map((reaction) => [reaction.emoji, reaction]));
                  return <div key={message.id} className="groups-message-item">{showDay && <div className="groups-day-divider"><span>{new Date(message.createdAt).toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })}</span></div>}<div className={`groups-message-row ${mine ? "is-mine" : ""}`}><span className="groups-message-avatar">{initials(mine ? currentUser?.name : message.senderName)}</span><article className={`groups-message-bubble ${mine ? "is-mine" : ""} ${message.deliveryStatus === "failed" ? "is-failed" : ""}`}><div className="groups-message-sender">{mine ? "You" : message.senderName || "Community member"}</div>{message.parentMessageId && <div className="groups-reply-quote"><strong>Replying to</strong><span>{messages.find((candidate) => String(candidate.id) === String(message.parentMessageId))?.content || "an earlier message"}</span></div>}<p>{message.content}</p><div className="groups-message-footer"><span>{messageTime(message.createdAt)}</span>{mine && <span className={`groups-message-tick ${message.deliveryStatus === "failed" ? "is-failed" : ""}`} title={message.deliveryStatus === "sending" ? "Sending" : message.deliveryStatus === "failed" ? "Not sent" : "Sent"}>{message.deliveryStatus === "sending" ? "◷" : message.deliveryStatus === "failed" ? "!" : "✓✓"}</span>}</div>{message.deliveryStatus === "failed" && <button className="groups-retry-message" type="button" onClick={() => retryMessage(message)}>Retry sending</button>}<div className="groups-message-tools"><button type="button" className="groups-reply-button" onClick={() => setReplyTo(message)} disabled={isLocal}>↶ Reply</button></div><div className="groups-reactions">{reactionOptions.map((emoji) => { const reaction = reacted.get(emoji); return <button key={emoji} type="button" className={`groups-reaction ${reaction?.reactedByMe ? "is-active" : ""}`} title={`React ${emoji}`} aria-label={`React with ${emoji}`} aria-pressed={Boolean(reaction?.reactedByMe)} disabled={isLocal} onClick={() => toggleReaction(message, emoji)}>{emoji}{reaction?.count > 0 && <span>{reaction.count}</span>}</button>; })}</div></article></div></div>;
                })}</div>}
              </div>

              <form className="groups-composer" onSubmit={sendMessage}>{replyTo && <div className="groups-reply-preview"><span><strong>Replying to {replyTo.senderName || "community member"}</strong><small>{replyTo.content}</small></span><button type="button" aria-label="Cancel reply" onClick={() => setReplyTo(null)}>×</button></div>}<div className="groups-composer-icon" aria-hidden="true">＋</div><textarea value={messageText} rows={1} maxLength={4000} placeholder={`Message ${selectedGroup.name}`} aria-label="Write a group message" onChange={(event) => setMessageText(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); } }} /><span className="groups-composer-hint">Enter to send · Shift+Enter for a new line</span><button type="submit" className="groups-send-button" aria-label="Send message" disabled={sending || !messageText.trim()}>{sending ? <span className="groups-send-spinner" /> : <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 12 20 4l-4 16-4-6-8-2Z" /><path d="m12 14 8-10" /></svg>}</button></form>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
