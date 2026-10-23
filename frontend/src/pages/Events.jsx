import { useCallback, useEffect, useState } from "react";
import api, { errorMessage } from "../services/api";
const blank={title:"",description:"",location:"",startsAt:"",capacity:""};
export default function Events(){
    const [items,setItems]=useState([]);
    const [showForm,setShowForm]=useState(false);
    const [form,setForm]=useState(blank);
    const [error,setError]=useState("");
    const [notice,setNotice]=useState("");
    const [busy,setBusy]=useState(false);
    const load=useCallback(async()=>{try{
        const {data}=await api.get('/events');
        setItems(Array.isArray(data)?data:[]);
        setError('')}catch(e){setError(errorMessage(e,'Could not load community events.'))}},[]);
        useEffect(()=>{load()},[load]);
        async function create(e){e.preventDefault();setBusy(true);
            setError('');try{await api.post('/events',{...form,startsAt:new Date(form.startsAt).toISOString(),capacity:form.capacity?Number(form.capacity):null});
            setNotice('Event published.');
            setForm(blank);
            setShowForm(false);
            await load()}catch(e){setError(errorMessage(e,'Could not create event.'))}finally{setBusy(false)}}
            async function register(item){try{await api.post(`/events/${item.id}/register`);
            setNotice(`You registered for ${item.title}.`);
            await load()}catch(e){setError(errorMessage(e,'Could not register for this event.'))}}
            async function unregister(item){try{await api.delete(`/events/${item.id}/registration`);
            setNotice(`Registration cancelled for ${item.title}.`);
            await load()}catch(e){setError(errorMessage(e,'Could not cancel registration.'))}}return <div className="section-stack"><div className="page-heading"><div><p className="eyebrow">MEET, ORGANIZE, PARTICIPATE</p><h1>Community events</h1><p>Discover workshops, neighbourhood meetups and volunteer activities. Event dates and places come from published platform records.</p></div><button className="btn" onClick={()=>setShowForm(!showForm)}>{showForm?'Cancel':'+ Create event'}</button></div>{error&&<div className="status-message error">{error}</div>}{notice&&<div className="status-message success">{notice}</div>}{showForm&&<form className="surface panel form-stack" onSubmit={create}><h2>Create an event</h2><div className="form-grid"><label className="form-field"><span className="field-label">Title</span><input className="field" value={form.title} required maxLength={160} onChange={e=>setForm({...form,title:e.target.value})}/></label><label className="form-field"><span className="field-label">Location</span><input className="field" value={form.location} required maxLength={255} onChange={e=>setForm({...form,location:e.target.value})}/></label><label className="form-field"><span className="field-label">Start date & time</span><input className="field" type="datetime-local" value={form.startsAt} required onChange={e=>setForm({...form,startsAt:e.target.value})}/></label><label className="form-field"><span className="field-label">Capacity (optional)</span><input className="field" type="number" min="1" value={form.capacity} onChange={e=>setForm({...form,capacity:e.target.value})}/></label><label className="form-field full"><span className="field-label">Description</span><textarea className="field-textarea" value={form.description} required maxLength={3000} onChange={e=>setForm({...form,description:e.target.value})}/></label></div><div className="form-footer"><span className="muted small">Your account is recorded as the organizer.</span><button className="btn" disabled={busy}>{busy?'Publishing…':'Publish event'}</button></div></form>}<section className="surface panel"><div className="panel-title"><div><h2>Events</h2><p>{items.length} published event(s)</p></div><span className="badge">COMMUNITY</span></div>{items.length===0?<div className="empty-state"><span className="empty-symbol">▦</span><h3>No events yet</h3><p>Be the first to organize an event for your community.</p></div>:<div className="data-list">{items.map(it=><article className="data-card" key={it.id}><div className="data-card-head"><div><h3>{it.title}</h3><div className="data-meta"><span>◷ {it.startsAt?new Date(it.startsAt).toLocaleString():'Time not provided'}</span><span>⌖ {it.location}</span></div></div><span className={`badge ${it.registeredByMe?'neutral':'success'}`}>{it.registeredByMe?'Registered':it.status||'PUBLISHED'}</span></div><p>{it.description}</p><div className="data-meta"><span>Organized by {it.organizerName||'Community member'}</span><span>{it.registrationCount||0}{it.capacity?` / ${it.capacity}`:''} registered</span></div><div className="inline-actions" style={{marginTop:12}}>{it.registeredByMe?<button className="btn secondary small-btn" onClick={()=>unregister(it)}>Cancel registration</button>:<button className="btn small-btn" onClick={()=>register(it)} disabled={it.capacity&&it.registrationCount>=it.capacity}>Register</button>}</div></article>)}</div>}</section></div>}
