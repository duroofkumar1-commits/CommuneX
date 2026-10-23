import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

const NOTES_KEY = "communex.focus.notes";

export default function FocusMode() {
  const [notes, setNotes] = useState(() => localStorage.getItem(NOTES_KEY) || "");
  const [audioName, setAudioName] = useState("");
  const [audioUrl, setAudioUrl] = useState("");
  const [savedAt, setSavedAt] = useState("");
  const audioRef = useRef(null);

  useEffect(() => {
    localStorage.setItem(NOTES_KEY, notes);
    setSavedAt(new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }));
  }, [notes]);

  useEffect(() => () => {
    if (audioUrl) URL.revokeObjectURL(audioUrl);
  }, [audioUrl]);

  function chooseAudio(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    if (audioUrl) URL.revokeObjectURL(audioUrl);
    setAudioUrl(URL.createObjectURL(file));
    setAudioName(file.name);
  }

  return (
    <div className="section-stack">
      <div className="page-heading">
        <div><p className="eyebrow">ONE CALM WORKSPACE</p><h1>Focus mode</h1><p>Plan a community project, keep quick notes and listen to audio you own or are licensed to use.</p></div>
        <span className="badge neutral">LOCAL DRAFT</span>
      </div>
      <div className="two-column">
        <section className="surface panel form-stack">
          <div className="panel-title"><div><h2>Focus notes</h2><p>Drafts save automatically in this browser.</p></div><span className="badge">{notes.trim() ? `${notes.trim().split(/\s+/).length} words` : "0 words"}</span></div>
          <label><span className="field-label">Your notes</span><textarea className="field-textarea" rows={16} value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Write your goals, meeting agenda, volunteer plan or ideas…" aria-label="Focus notes" /></label>
          <div className="form-footer"><span className="muted small">{savedAt ? `Saved on this device at ${savedAt}` : "Ready to save"}</span><button className="btn ghost small-btn" onClick={() => { if (window.confirm("Clear the saved focus notes from this browser?")) setNotes(""); }}>Clear notes</button></div>
          <p className="muted small">These focus notes are browser-local and are not the same as a cloud document. Use the <Link className="text-link" to="/editor">Text Editor</Link> to create and sync documents to your CommuneX account.</p>
        </section>
        <section className="surface panel form-stack">
          <div className="panel-title"><div><h2>Local audio</h2><p>Audio files stay on your device.</p></div><span className="badge neutral">NO UPLOAD</span></div>
          <label><span className="field-label">Choose an audio file</span><input className="field" type="file" accept="audio/*" onChange={chooseAudio} /></label>
          {audioUrl ? <><div className="info"><strong>{audioName}</strong><p className="muted small">Playback from your selected local file.</p></div><audio ref={audioRef} controls src={audioUrl} style={{ width: "100%" }}>Your browser does not support audio playback.</audio><button className="btn ghost" onClick={() => { audioRef.current?.pause(); setAudioName(""); setAudioUrl(""); }}>Remove audio</button></> : <div className="empty-state"><span className="empty-symbol">♫</span><h3>Bring your own soundtrack</h3><p>Select an audio file to play it here. Nothing is uploaded to CommuneX.</p></div>}
          <div className="divider" />
          <div><h3>Quick links</h3><div className="inline-actions"><Link className="btn secondary" to="/music">Open full music player</Link><Link className="btn ghost" to="/editor">Open text editor</Link></div></div>
        </section>
      </div>
    </div>
  );
}
