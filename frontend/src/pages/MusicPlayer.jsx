import { useEffect, useRef, useState } from "react";
import "./MusicPlayer.css";

class DoublyLinkedPlaylist {
  constructor(items = []) {
    this.nodes = [];
    this.index = 0;
    this.setItems(items);
  }

  setItems(items) {
    this.nodes = items.map((value, i) => ({ value, prev: null, next: null, index: i }));
    this.nodes.forEach((node, i) => {
      node.prev = this.nodes[i - 1] || null;
      node.next = this.nodes[i + 1] || null;
    });
    if (this.nodes.length) {
      this.nodes[0].prev = this.nodes.at(-1);
      this.nodes.at(-1).next = this.nodes[0];
    }
    this.index = Math.min(this.index, Math.max(0, this.nodes.length - 1));
  }

  current() {
    return this.nodes[this.index]?.value || null;
  }

  move(direction, wrap = true) {
    if (!this.nodes.length) return null;
    const node = this.nodes[this.index];
    let next = direction === 'next' ? node.next : node.prev;
    if (!wrap && ((direction === 'next' && this.index === this.nodes.length - 1) || (direction === 'previous' && this.index === 0))) {
      return node.value;
    }
    this.index = next?.index ?? this.index;
    return this.current();
  }

  select(index) {
    if (index >= 0 && index < this.nodes.length) {
      this.index = index;
    }
    return this.current();
  }
}

export default function MusicPlayer() {
  const [songs, setSongs] = useState([]);
  const [current, setCurrent] = useState(0);
  const [shuffle, setShuffle] = useState(false);
  const [repeat, setRepeat] = useState('all');
  const [volume, setVolume] = useState(0.8);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [playlistName, setPlaylistName] = useState('My local playlist');
  const [savedLists, setSavedLists] = useState(() => JSON.parse(localStorage.getItem('communex.music.playlists') || '[]'));

  const audioRef = useRef(null);
  const listRef = useRef(new DoublyLinkedPlaylist());
  const objectUrls = useRef(new Set());
  const song = songs[current] || null;

  useEffect(() => {
    listRef.current.setItems(songs);
    listRef.current.select(current);
  }, [songs, current]);

  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = volume;
    }
  }, [volume]);

  useEffect(() => {
    return () => {
      objectUrls.current.forEach(url => URL.revokeObjectURL(url));
      objectUrls.current.clear();
    };
  }, []);

  function addFiles(event) {
    const files = Array.from(event.target.files || []);
    const newSongs = files.filter(f => f.type.startsWith('audio/')).map(file => {
      const url = URL.createObjectURL(file);
      objectUrls.current.add(url);
      return {
        id: `${file.name}-${file.size}-${file.lastModified}`,
        title: file.name.replace(/\.[^/.]+$/, ''),
        fileName: file.name,
        size: file.size,
        url
      };
    });
    if (newSongs.length) {
      setSongs(s => [...s, ...newSongs]);
      if (!song) setCurrent(0);
    }
    event.target.value = '';
  }

  function playIndex(index) {
    if (!songs.length) return;
    const next = ((index % songs.length) + songs.length) % songs.length;
    setCurrent(next);
    setPlaying(true);
    window.setTimeout(() => audioRef.current?.play().catch(() => setPlaying(false)), 0);
  }

  function next() {
    if (!songs.length) return;
    if (repeat === 'one' && audioRef.current) {
      audioRef.current.currentTime = 0;
      audioRef.current.play().catch(() => setPlaying(false));
      return;
    } else if (shuffle && songs.length > 1) {
      let n = current;
      while (n === current) n = Math.floor(Math.random() * songs.length);
      playIndex(n);
    } else if (current === songs.length - 1 && repeat === 'off') {
      setPlaying(false);
      setProgress(0);
      return;
    } else {
      playIndex(current + 1);
    }
  }

  function previous() {
    if (progress > 3 && audioRef.current) {
      audioRef.current.currentTime = 0;
      setProgress(0);
      return;
    }
    playIndex(current - 1);
  }

  function savePlaylist() {
    const entry = {
      name: playlistName.trim() || 'My playlist',
      tracks: songs.map(s => ({ title: s.title, fileName: s.fileName }))
    };
    const lists = [...savedLists, entry];
    setSavedLists(lists);
    localStorage.setItem('communex.music.playlists', JSON.stringify(lists));
  }

  const formatTime = seconds => `${Math.floor((seconds || 0) / 60)}:${String(Math.floor((seconds || 0) % 60)).padStart(2, '0')}`;

  return (
    <div className="section-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">YOUR SPACE, YOUR SOUND</p>
          <h1>Music player</h1>
          <p>Play audio files you own or are licensed to use. Audio stays in your browser session; playlist names and track metadata are saved locally.</p>
        </div>
        <span className="badge">LOCAL AUDIO</span>
      </div>
      <div className="two-column">
        <section className="surface panel music-player-panel">
          <div className="record-art">
            <div className="record-ring"><span>♫</span></div>
            <span className="record-glow"/>
          </div>
          <div className="music-now-playing">NOW PLAYING</div>
          <h2 className="music-song-title">{song?.title || 'Choose your soundtrack'}</h2>
          <p className="music-song-subtitle">{song?.fileName || 'Select local audio files to begin'}</p>
          {song && (
            <audio
              ref={audioRef}
              src={song.url}
              onTimeUpdate={e => setProgress(e.currentTarget.currentTime)}
              onLoadedMetadata={e => setDuration(e.currentTarget.duration || 0)}
              onEnded={next}
              onPlay={() => setPlaying(true)}
              onPause={() => setPlaying(false)}
            />
          )}
          <div className="music-progress">
            <input
              aria-label="Playback progress"
              type="range"
              min="0"
              max={duration || 100}
              value={Math.min(progress, duration || 100)}
              onChange={e => {
                const val = Number(e.target.value);
                setProgress(val);
                if (audioRef.current) audioRef.current.currentTime = val;
              }}
            />
            <div className="music-times">
              <span>{formatTime(progress)}</span>
              <span>{formatTime(duration)}</span>
            </div>
          </div>
          <div className="music-controls">
            <button className="music-toggle" onClick={previous} disabled={!songs.length} aria-label="Previous track">◀◀</button>
            <button
              className="music-play"
              onClick={() => {
                if (!song) return;
                if (playing) {
                  audioRef.current?.pause();
                } else {
                  audioRef.current?.play().catch(() => {});
                }
              }}
              disabled={!song}
              aria-label={playing ? 'Pause' : 'Play'}
            >
              {playing ? 'Ⅱ' : '▶'}
            </button>
            <button className="music-toggle" onClick={next} disabled={!songs.length} aria-label="Next track">▶▶</button>
          </div>
          <div className="music-options">
            <button className={`btn small-btn ${shuffle ? '' : 'secondary'}`} onClick={() => setShuffle(!shuffle)}>
              {shuffle ? '🔀 Shuffle on' : 'Shuffle'}
            </button>
            <button className="btn secondary small-btn" onClick={() => setRepeat(r => r === 'all' ? 'one' : r === 'one' ? 'off' : 'all')}>
              Repeat: {repeat === 'all' ? 'All' : repeat === 'one' ? 'One' : 'Off'}
            </button>
            <label className="volume-control">
              🔊 <input aria-label="Volume" type="range" min="0" max="1" step="0.05" value={volume} onChange={e => setVolume(Number(e.target.value))} />
            </label>
          </div>
          <div className="audio-drop">
            <strong>＋ Add local audio</strong>
            <p className="muted small">MP3, WAV, OGG and other browser-supported formats</p>
            <input type="file" accept="audio/*" multiple onChange={addFiles} />
          </div>
        </section>
        <section className="surface panel">
          <div className="panel-title">
            <div>
              <h2>Playing queue</h2>
              <p>{songs.length} track(s) in this session</p>
            </div>
            <span className="badge neutral">DOUBLY LINKED LIST</span>
          </div>
          {songs.length === 0 ? (
            <div className="empty-state">
              <span className="empty-symbol">♫</span>
              <h3>Your playlist is empty</h3>
              <p>Add local audio files. CommuneX does not bundle copyrighted music.</p>
            </div>
          ) : (
            <div className="data-list">
              {songs.map((s, i) => (
                <button className="music-track-row" key={s.id} onClick={() => playIndex(i)}>
                  <span className={`music-track-index ${i === current ? 'current' : ''}`}>
                    {i === current && playing ? '♫' : String(i + 1).padStart(2, '0')}
                  </span>
                  <span className="music-track-title">
                    <strong>{s.title}</strong>
                    <small>{s.fileName}</small>
                  </span>
                  <span className="music-track-size">{(s.size / 1048576).toFixed(1)} MB</span>
                  {i === current && <span className="badge">NOW</span>}
                </button>
              ))}
            </div>
          )}
          <div className="divider" />
          <form className="form-stack" onSubmit={e => { e.preventDefault(); savePlaylist(); }}>
            <label>
              <span className="field-label">Save playlist metadata</span>
              <input className="field" value={playlistName} onChange={e => setPlaylistName(e.target.value)} maxLength={80} />
            </label>
            <button className="btn secondary" disabled={!songs.length}>Save playlist locally</button>
          </form>
          {savedLists.length > 0 && (
            <>
              <div className="divider" />
              <h3>Saved playlists</h3>
              <div className="data-list">
                {savedLists.slice(-5).reverse().map((l, i) => (
                  <article className="data-card" key={`${l.name}-${i}`}>
                    <h3>{l.name}</h3>
                    <p>{l.tracks.length} saved track reference(s)</p>
                    <span className="muted small">Re-select audio files after a page reload; browsers do not retain access to local file data.</span>
                  </article>
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}