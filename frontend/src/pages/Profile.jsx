//import { useEffect,useState } from "react";import api,{errorMessage} from "../services/api";
//export default function Profile(){const [user,setUser]=useState(null);const [name,setName]=useState('');const [profileImageUrl,setProfileImageUrl]=useState('');const [error,setError]=useState('');const [notice,setNotice]=useState('');const [busy,setBusy]=useState(false);useEffect(()=>{api.get('/users/me').then(({data})=>{setUser(data);setName(data.name||'');setProfileImageUrl(data.profileImageUrl||'')}).catch(e=>setError(errorMessage(e)))},[]);async function save(e){e.preventDefault();setBusy(true);setError('');setNotice('');try{const {data}=await api.put('/users/me',{name:name.trim(),profileImageUrl:profileImageUrl.trim()||null});setUser(data);window.dispatchEvent(new CustomEvent('communex:user-updated'));setNotice('Profile updated.')}catch(e){setError(errorMessage(e,'Could not update profile.'))}finally{setBusy(false)}}return <div className="section-stack"><div className="page-heading"><div><p className="eyebrow">YOUR ACCOUNT</p><h1>Profile & settings</h1><p>Manage your visible profile details. Your role and account identity are controlled by the backend.</p></div></div>{error&&<div className="status-message error">{error}</div>}{notice&&<div className="status-message success">{notice}</div>}<div className="two-column"><form className="surface panel form-stack" onSubmit={save}><div className="panel-title"><div><h2>Public profile</h2><p>Keep information accurate and respectful.</p></div></div><label><span className="field-label">Display name</span><input className="field" required maxLength={100} value={name} onChange={e=>setName(e.target.value)}/></label><label><span className="field-label">Profile image URL (optional)</span><input className="field" type="url" maxLength={500} value={profileImageUrl} onChange={e=>setProfileImageUrl(e.target.value)} placeholder="https://…"/></label><button className="btn" disabled={busy||!user}>{busy?'Saving…':'Save profile'}</button></form><section className="surface panel"><div className="panel-title"><div><h2>Account information</h2><p>Managed by CommuneX authentication.</p></div></div>{user?<><div className="info" style={{padding:16,borderRadius:12,background:'#f3f8f4'}}><strong>{user.name}</strong><p>{user.email}</p><span className="badge">{user.role}</span></div><p className="muted small">Authentication provider: {user.provider}</p><p className="muted small">Never share your password, authentication token or account recovery links. Password recovery and email verification need a configured email provider before production use.</p></>:<p className="muted small">Loading profile…</p>}</section></div></div>}
import { useEffect, useState } from "react";
import api, { errorMessage } from "../services/api";

export default function Profile() {
  const [user, setUser] = useState(null);
  const [name, setName] = useState('');
  const [profileImageUrl, setProfileImageUrl] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get('/users/me')
      .then(({ data }) => {
        setUser(data);
        setName(data.name || '');
        setProfileImageUrl(data.profileImageUrl || '');
      })
      .catch(e => setError(errorMessage(e)));
  }, []);

  // Convert local images into compact data URLs. V5 widens the DB column to TEXT.
  function handleFileChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;

    setError('');
    setNotice('');
    if (!file.type.startsWith('image/')) {
      setError('Please select an image file.');
      e.target.value = '';
      return;
    }
    if (file.size > 8 * 1024 * 1024) {
      setError('Please choose an image smaller than 8 MB.');
      e.target.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onerror = () => setError('The selected image could not be read. Please try another file.');
    reader.onload = (event) => {
      const img = new Image();
      img.onerror = () => setError('This image format could not be processed. Try a JPG or PNG image.');
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const maxSize = 256;
        const scale = Math.min(1, maxSize / Math.max(img.width, img.height));
        canvas.width = Math.max(1, Math.round(img.width * scale));
        canvas.height = Math.max(1, Math.round(img.height * scale));
        const context = canvas.getContext('2d');
        if (!context) {
          setError('Your browser could not process this image.');
          return;
        }
        // JPEG needs a solid background when the source image contains transparency.
        context.fillStyle = '#ffffff';
        context.fillRect(0, 0, canvas.width, canvas.height);
        context.drawImage(img, 0, 0, canvas.width, canvas.height);
        const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.78);
        setProfileImageUrl(compressedDataUrl);
      };
      img.src = String(event.target?.result || '');
    };
    reader.readAsDataURL(file);
  }

  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const { data } = await api.put('/users/me', {
        name: name.trim(),
        profileImageUrl: profileImageUrl.trim() || null
      });
      setUser(data);
      window.dispatchEvent(new CustomEvent('communex:user-updated'));
      setNotice('Profile updated.');
    } catch (e) {
      setError(errorMessage(e, 'Could not update profile.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="section-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">YOUR ACCOUNT</p>
          <h1>Profile & settings</h1>
          <p>Manage your visible profile details. Your role and account identity are controlled by the backend.</p>
        </div>
      </div>

      {error && <div className="status-message error">{error}</div>}
      {notice && <div className="status-message success">{notice}</div>}

      <div className="two-column">
        <form className="surface panel form-stack" onSubmit={save}>
          <div className="panel-title">
            <div>
              <h2>Public profile</h2>
              <p>Keep information accurate and respectful.</p>
            </div>
          </div>

          <label>
            <span className="field-label">Display name</span>
            <input className="field" required maxLength={100} value={name} onChange={e => setName(e.target.value)} />
          </label>

          <label>
            <span className="field-label">Upload image from computer (optional)</span>
            <input className="field" type="file" accept="image/*" onChange={handleFileChange} />
          </label>

          <label>
            <span className="field-label">Or Profile image URL</span>
            <input className="field" type="url" maxLength={500} value={profileImageUrl.startsWith('data:') ? '' : profileImageUrl} onChange={e => setProfileImageUrl(e.target.value)} placeholder="https://…" />
          </label>

          {profileImageUrl && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
              <img src={profileImageUrl} alt="Profile preview" style={{ width: '42px', height: '42px', borderRadius: '10px', objectFit: 'cover', border: '1px solid #d1dedb' }} />
              <span className="muted small">Image preview (save profile to upload it)</span>
            </div>
          )}

          <button className="btn" disabled={busy || !user}>
            {busy ? 'Saving…' : 'Save profile'}
          </button>
        </form>

        <section className="surface panel">
          <div className="panel-title">
            <div>
              <h2>Account information</h2>
              <p>Managed by CommuneX authentication.</p>
            </div>
          </div>
          {user ? (
            <>
              <div className="info" style={{ padding: 16, borderRadius: 12, background: '#f3f8f4' }}>
                <strong>{user.name}</strong>
                <p>{user.email}</p>
                <span className="badge">{user.role}</span>
              </div>
              <p className="muted small">Authentication provider: {user.provider}</p>
              <p className="muted small">Never share your password, authentication token or account recovery links. Password recovery and email verification need a configured email provider before production use.</p>
            </>
          ) : (
            <p className="muted small">Loading profile…</p>
          )}
        </section>
      </div>
    </div>
  );
}