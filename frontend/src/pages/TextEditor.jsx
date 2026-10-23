import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import api, { errorMessage } from "../services/api";

const STORAGE_KEY = "communex.documents.v1";
function readLocalDocuments() {
  try { const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"); return Array.isArray(saved) ? saved : []; }
  catch { return []; }
}
function makeId() { return globalThis.crypto?.randomUUID?.() || `local-${Date.now()}-${Math.random().toString(36).slice(2)}`; }

export default function TextEditor() {
  const [docs, setDocs] = useState(readLocalDocuments);
  const [activeId, setActiveId] = useState(() => readLocalDocuments()[0]?.localId || null);
  const [title, setTitle] = useState(() => readLocalDocuments()[0]?.title || "Untitled document");
  const [content, setContent] = useState(() => readLocalDocuments()[0]?.content || "");
  const [undoStack, setUndoStack] = useState([]);
  const [redoStack, setRedoStack] = useState([]);
  const [notice, setNotice] = useState("Drafts auto-save in this browser.");
  const [error, setError] = useState("");
  const [cloudSync, setCloudSync] = useState(false);
  const [remoteLoading, setRemoteLoading] = useState(false);
  const saveTimer = useRef(null);
  const editorRef = useRef(null);
  const wordCount = useMemo(() => content.trim() ? content.trim().split(/\s+/).length : 0, [content]);
  const active = docs.find((doc) => doc.localId === activeId);

  const persistLocal = useCallback((nextDocs) => {
    setDocs(nextDocs);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextDocs));
  }, []);

  useEffect(() => {
    let mounted = true;
    setRemoteLoading(true);
    api.get("/documents").then(({ data }) => {
      if (!mounted || !Array.isArray(data)) return;
      setCloudSync(true);
      const current = readLocalDocuments();
      const knownRemoteIds = new Set(current.map((doc) => String(doc.remoteId || "")));
      const imported = data.filter((doc) => !knownRemoteIds.has(String(doc.id))).map((doc) => ({
        localId: `remote-${doc.id}`, remoteId: doc.id, title: doc.title, content: doc.content || "",
        updatedAt: doc.updatedAt || new Date().toISOString(), createdAt: doc.createdAt,
      }));
      if (imported.length) {
        const merged = [...imported, ...current];
        localStorage.setItem(STORAGE_KEY, JSON.stringify(merged));
        setDocs(merged);
      }
    }).catch(() => { if (mounted) setCloudSync(false); }).finally(() => { if (mounted) setRemoteLoading(false); });
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    if (!activeId) return undefined;
    window.clearTimeout(saveTimer.current);
    saveTimer.current = window.setTimeout(() => {
      const current = readLocalDocuments();
      const existing = current.find((doc) => doc.localId === activeId) || {};
      const saved = { ...existing, localId: activeId, title: title.trim() || "Untitled document", content, updatedAt: new Date().toISOString() };
      persistLocal([saved, ...current.filter((doc) => doc.localId !== activeId)]);
      setNotice("Draft auto-saved locally.");
    }, 700);
    return () => window.clearTimeout(saveTimer.current);
  }, [activeId, title, content, persistLocal]);

  const changeContent = useCallback((nextContent) => {
    setUndoStack((stack) => [...stack.slice(-79), content]);
    setRedoStack([]);
    setContent(nextContent);
  }, [content]);

  const chooseDocument = useCallback((doc) => {
    setActiveId(doc.localId); setTitle(doc.title || "Untitled document"); setContent(doc.content || "");
    setUndoStack([]); setRedoStack([]); setNotice("Document opened."); setError("");
  }, []);

  function createDocument() {
    const id = makeId();
    const doc = { localId: id, title: "Untitled document", content: "", createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
    const next = [doc, ...docs]; persistLocal(next); setActiveId(id); setTitle(doc.title); setContent(""); setUndoStack([]); setRedoStack([]); setNotice("New document created."); setError("");
  }

  const undoChange = useCallback(() => {
    if (!undoStack.length) return;
    const previous = undoStack[undoStack.length - 1];
    setUndoStack((stack) => stack.slice(0, -1)); setRedoStack((stack) => [...stack, content].slice(-80)); setContent(previous); setNotice("Change undone.");
  }, [undoStack, content]);
  const redoChange = useCallback(() => {
    if (!redoStack.length) return;
    const next = redoStack[redoStack.length - 1];
    setRedoStack((stack) => stack.slice(0, -1)); setUndoStack((stack) => [...stack, content].slice(-80)); setContent(next); setNotice("Change restored.");
  }, [redoStack, content]);
  function insertText(before, after = "") {
    const element = editorRef.current;
    const start = element?.selectionStart ?? content.length;
    const end = element?.selectionEnd ?? content.length;
    const next = content.slice(0, start) + before + content.slice(start, end) + after + content.slice(end);
    changeContent(next);
    requestAnimationFrame(() => { element?.focus(); const caret = start + before.length + (end - start); element?.setSelectionRange(caret, caret); });
  }

  const save = useCallback(async () => {
    if (!activeId) { setNotice("Create a document before saving."); return; }
    setError("");
    const current = readLocalDocuments();
    const existing = current.find((doc) => doc.localId === activeId) || {};
    const draft = { ...existing, localId: activeId, title: title.trim() || "Untitled document", content, updatedAt: new Date().toISOString() };
    persistLocal([draft, ...current.filter((doc) => doc.localId !== activeId)]);
    setNotice(`Saved locally at ${new Date().toLocaleTimeString()}.`);
    if (!cloudSync) return;
    try {
      if (draft.remoteId) {
        const { data } = await api.put(`/documents/${draft.remoteId}`, { title: draft.title, content: draft.content });
        const synced = { ...draft, remoteId: data.id || draft.remoteId, updatedAt: data.updatedAt || draft.updatedAt };
        persistLocal([synced, ...readLocalDocuments().filter((doc) => doc.localId !== activeId)]);
      } else {
        const { data } = await api.post("/documents", { title: draft.title, content: draft.content });
        const synced = { ...draft, remoteId: data.id, updatedAt: data.updatedAt || draft.updatedAt };
        persistLocal([synced, ...readLocalDocuments().filter((doc) => doc.localId !== activeId)]);
      }
      setNotice("Saved in this browser and synced to your CommuneX account.");
    } catch (e) { setError(errorMessage(e, "Cloud sync failed. Your browser draft is still safe.")); }
  }, [activeId, title, content, persistLocal, cloudSync]);

  const saveRef = useRef(save);
  const undoRef = useRef(undoChange);
  const redoRef = useRef(redoChange);
  useEffect(() => {
    saveRef.current = save;
    undoRef.current = undoChange;
    redoRef.current = redoChange;
  }, [save, undoChange, redoChange]);
  useEffect(() => {
    function onKey(event) {
      if (!(event.ctrlKey || event.metaKey)) return;
      const key = event.key.toLowerCase();
      if (key === "s") { event.preventDefault(); saveRef.current(); }
      else if (key === "z" && event.shiftKey) { event.preventDefault(); redoRef.current(); }
      else if (key === "z") { event.preventDefault(); undoRef.current(); }
      else if (key === "y") { event.preventDefault(); redoRef.current(); }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  async function deleteDocument(doc) {
    if (!window.confirm(`Delete “${doc.title}”?`)) return;
    if (doc.remoteId && cloudSync) { try { await api.delete(`/documents/${doc.remoteId}`); } catch (e) { setError(errorMessage(e, "Could not delete synced document.")); return; } }
    const next = docs.filter((item) => item.localId !== doc.localId); persistLocal(next);
    if (activeId === doc.localId) { const first = next[0]; setActiveId(first?.localId || null); setTitle(first?.title || "Untitled document"); setContent(first?.content || ""); setUndoStack([]); setRedoStack([]); }
    setNotice("Document deleted.");
  }

  return <div className="section-stack">
    <div className="page-heading"><div><p className="eyebrow">THINK, PLAN, WRITE</p><h1>Smart text editor</h1><p>Write meeting notes, community plans or ideas. Changes auto-save to this browser; use Save to sync signed-in documents when the API is available.</p></div><button className="btn" onClick={createDocument}>＋ New document</button></div>
    {error && <div className="status-message error" role="alert">{error}</div>}{notice && <div className="status-message info" role="status">{notice}</div>}
    <div className="two-column"><aside className="surface panel"><div className="panel-title"><div><h2>Your documents</h2><p>{docs.length} browser draft(s)</p></div><button className="btn ghost small-btn" onClick={createDocument}>＋</button></div>
      {docs.length === 0 ? <div className="empty-state"><span className="empty-symbol">▣</span><h3>No documents yet</h3><p>Create a document to start writing.</p></div> : <div className="data-list">{docs.map((doc) => <div className="data-card" key={doc.localId} style={{ borderColor: doc.localId === activeId ? "#8bbca5" : undefined, background: doc.localId === activeId ? "#f3faf4" : "#fff" }}><button className="document-open" onClick={() => chooseDocument(doc)} style={{ textAlign: "left", border: 0, background: "transparent", padding: 0, width: "100%" }}><h3>{doc.title || "Untitled document"}</h3><p>{(doc.content || "Empty document").slice(0, 80)}</p><span className="data-meta">Saved {doc.updatedAt ? new Date(doc.updatedAt).toLocaleString() : "Not saved yet"}</span></button><div className="inline-actions" style={{ marginTop: 8 }}><button className="btn ghost small-btn" onClick={() => deleteDocument(doc)}>Delete</button>{doc.remoteId && <span className="badge">Cloud copy</span>}</div></div>)}</div>}
      <div className="divider"/><p className="muted small">{remoteLoading ? "Checking cloud sync…" : cloudSync ? "Cloud sync available for this account." : "Browser-local mode. API sync is optional."}</p></aside>
      <section className="surface panel"><label className="form-field full"><span className="field-label">Document title</span><input className="field" value={title} onChange={(event) => setTitle(event.target.value)} maxLength={160}/></label>
        <div className="editor-toolbar" style={{ marginTop: 13 }}><button className="btn ghost small-btn" onClick={undoChange} disabled={!undoStack.length} title="Ctrl+Z">↶ Undo</button><button className="btn ghost small-btn" onClick={redoChange} disabled={!redoStack.length} title="Ctrl+Y">↷ Redo</button><span className="divider" style={{ width: 1, height: 23, margin: "0 2px" }}/><button className="btn ghost small-btn" onClick={() => insertText("**", "**")}>Bold markdown</button><button className="btn ghost small-btn" onClick={() => insertText("## ")}>Heading</button><button className="btn ghost small-btn" onClick={() => insertText("\n- ")}>Bullet</button><button className="btn ghost small-btn" onClick={() => { if (window.confirm("Clear this document?")) changeContent(""); }}>Clear</button><button className="btn secondary small-btn" onClick={save}>Save now</button></div>
        <textarea ref={editorRef} className="editor-body" value={content} onChange={(event) => changeContent(event.target.value)} placeholder="Start writing your community plan, meeting notes or ideas…" spellCheck="true" aria-label="Document content"/>
        <div className="form-footer" style={{ marginTop: 12 }}><span className="muted small">{wordCount} words · {content.length} characters · Ctrl+S to save</span><span className="badge neutral">{active?.remoteId ? "CLOUD + LOCAL" : "LOCAL DRAFT"}</span></div>
      </section></div>
  </div>;
}
