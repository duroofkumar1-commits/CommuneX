import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import api from "../services/api";
import "./ProtectedLayout.css";

const navigation = [
  { group: "OVERVIEW", links: [
    ["/dashboard", "⌂", "Dashboard"], ["/search", "⌕", "Global search"], ["/map", "◎", "Map explorer"], ["/activity", "◷", "Activity history"], ["/impact", "✦", "Impact & challenges"]
  ]},
  { group: "COMMUNITY", links: [
    ["/community", "▤", "Community feed"], ["/groups", "♧", "Groups"], ["/announcements", "▣", "Announcements"], ["/events", "▦", "Events"], ["/polls", "◉", "Polls & feedback"], ["/messages", "✉", "Messages"], ["/help-requests", "✚", "Help requests"], ["/notifications", "♢", "Notifications"]
  ]},
  { group: "DISCOVER", links: [
    ["/facilities", "⌖", "Facilities"], ["/resources", "▦", "Resources & inventory"], ["/volunteers", "♧", "Volunteers"], ["/assistant", "✧", "AI assistant"]
  ]},
  { group: "TOOLS & PLAY", links: [
    ["/music", "♫", "Music player"], ["/editor", "▣", "Text editor"], ["/focus", "◌", "Focus mode"], ["/games", "▧", "Games & challenges"]
  ]},
  { group: "INSIGHTS", links: [
    ["/analytics", "▥", "Analytics"], ["/profile", "◉", "My profile"]
  ]},
];

function titleFromPath(path) {
  const found = navigation.flatMap((g) => g.links).find(([to]) => to === path);
  return found?.[2] || "CommuneX";
}

export default function ProtectedLayout() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const lastUnreadFetchAt = useRef(0);
  const [theme, setTheme] = useState(() => localStorage.getItem("communex.theme") || "light");
  const [language, setLanguage] = useState(() => localStorage.getItem("communex.language") || "en");
  const location = useLocation();
  const navigate = useNavigate();

  const loadUser = useCallback(async () => {
    if (!localStorage.getItem("token")) {
      navigate("/login", { replace: true });
      setLoading(false);
      return;
    }
    try {
      const { data } = await api.get("/users/me");
      setUser(data);
      localStorage.setItem("communex.email", data.email || "");
    } catch (error) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem("token");
        navigate("/login", { replace: true });
      }
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => { loadUser(); }, [loadUser]);
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("communex.theme", theme);
  }, [theme]);
  useEffect(() => {
    document.documentElement.lang = language === "ur" ? "ur" : "en";
    localStorage.setItem("communex.language", language);
  }, [language]);
  useEffect(() => {
    const expired = () => navigate("/login", { replace: true });
    window.addEventListener("communex:session-expired", expired);
    return () => window.removeEventListener("communex:session-expired", expired);
  }, [navigate]);
  useEffect(() => {
    const handler = () => loadUser();
    window.addEventListener("communex:user-updated", handler);
    return () => window.removeEventListener("communex:user-updated", handler);
  }, [loadUser]);
  const refreshUnreadCount = useCallback(async (force = false) => {
    const now = Date.now();
    if (!force && now - lastUnreadFetchAt.current < 15000) return;
    lastUnreadFetchAt.current = now;

    try {
      const { data } = await api.get("/notifications/unread-count");
      setUnread(Number(data.count || 0));
    } catch {
      // Keep the last known badge count if a short background refresh fails.
    }
  }, []);

  useEffect(() => {
    // The Notifications page gets the unread count from its combined summary request.
    if (location.pathname !== "/notifications") refreshUnreadCount();
  }, [location.pathname, refreshUnreadCount]);

  useEffect(() => {
    const refresh = (event) => {
      const count = event.detail?.unreadCount;
      if (Number.isFinite(count)) {
        setUnread(Math.max(0, count));
        lastUnreadFetchAt.current = Date.now();
        return;
      }
      refreshUnreadCount(true);
    };
    window.addEventListener("communex:notifications-updated", refresh);
    return () => window.removeEventListener("communex:notifications-updated", refresh);
  }, [refreshUnreadCount]);

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("communex.email");
    navigate("/login", { replace: true });
  }

  if (loading) return <div className="app-loading"><span className="loading-orbit" /> Loading CommuneX…</div>;
  if (!user) return null;
  const ur = language === "ur";
  const navTranslations = { "Dashboard":"ڈیش بورڈ", "Global search":"عالمی تلاش", "Map explorer":"نقشہ", "Activity history":"سرگرمی کی تاریخ", "Impact & challenges":"اثر اور چیلنجز", "Focus mode":"فوکس موڈ", "Community feed":"کمیونٹی فیڈ", "Groups":"گروپس", "Announcements":"اعلانات", "Events":"تقریبات", "Polls & feedback":"رائے شماری", "Messages":"پیغامات", "Help requests":"مدد کی درخواستیں", "Notifications":"اطلاعات", "Facilities":"سہولیات", "Resources & inventory":"وسائل اور انوینٹری", "Volunteers":"رضاکار", "AI assistant":"اے آئی معاون", "Music player":"موسیقی پلیئر", "Text editor":"ٹیکسٹ ایڈیٹر", "Games & challenges":"گیمز اور چیلنجز", "Analytics":"تجزیات", "My profile":"میری پروفائل", "Admin console":"ایڈمن کنسول", "Sign out":"لاگ آؤٹ" };

  return (
    <div className="app-frame">
      {mobileOpen && <button aria-label="Close navigation" className="sidebar-scrim" onClick={() => setMobileOpen(false)} />}
      <aside className={`app-sidebar ${mobileOpen ? "is-open" : ""}`}>
        <NavLink to="/dashboard" className="brand-lockup brand-lockup--light" onClick={() => setMobileOpen(false)}>
          <span className="brand-symbol" aria-hidden="true">
            <svg viewBox="0 0 64 64" role="img">
              <path
                fillRule="evenodd"
                fill="#99DAB5"
                stroke="rgba(160, 220, 198, 0.2)"
                strokeWidth="1"
                d="M32.31 62L6.09 46.74 6.07 16.78 31.82 1.96 57.87 16.94 47.34 23.31 31.82 14.37 16.86 23.03 16.87 40.48 30.04 48.19C30.15 47.59 30.06 46.95 30.06 46.34 30.06 45.32 30.13 44.31 30.3 43.3 30.53 41.93 30.96 40.63 31.33 39.29 32.1 36.48 32.75 34.15 34.65 31.83 36.52 29.55 39.17 28.02 41.99 27.24 43.3 26.88 45.53 26.37 46.86 26.59 47.07 27.9 46.94 29.31 46.73 30.61 45.97 35.29 43.31 39.09 38.73 40.65 37.72 40.99 36.09 41.48 35.04 41.36 35.24 40.52 35.61 39.72 36 38.96 37.4 36.22 39.42 34.03 41.64 31.95 40.56 32.08 38.18 33.99 37.31 34.76 36.41 35.55 35.61 36.46 34.91 37.44 31.18 42.66 31.89 48.48 33.78 54.23L33.66 58.65 53.87 46.49 47.38 42.71 34.52 50.35 34.12 48.04 47.35 40.18 58.18 46.47 32.31 62ZM30.71 12.47L30.7 5.13 9.38 17.42 15.75 21.13 30.71 12.47ZM53.53 16.98L32.9 5.12 32.9 12.46 47.31 20.76 53.53 16.98ZM14.66 40.48L14.65 23.03 8.29 19.35 8.3 44.18 14.66 40.48ZM31.59 54.73L30.57 51.1 15.79 42.35 9.34 46.1 31.44 58.96 31.59 54.73Z"
              />
              <path
                fill="#99DAB5"
                d="M30.75 34.17C29.9 33.62 29.14 32.31 28.66 31.43 26.67 27.75 27.2 23.58 29.75 20.33 30.28 19.65 30.86 18.94 31.57 18.44 32.15 18.8 32.62 19.31 33.07 19.82 34.92 21.9 35.97 24.7 35.25 27.46 35.1 28.03 34.92 28.64 34.58 29.1L32.3 31.92 31.55 24.52 30.75 34.17ZM20.24 31.88C21.23 31.71 22.37 31.86 23.35 32.07 26.91 32.82 28.8 34.63 29.84 38.09L29.28 39.97C28.86 39.56 28.56 39.02 28.18 38.57 27.27 37.48 25.72 36.18 24.33 35.82 24.66 36.21 25.07 36.51 25.41 36.88 26.52 38.04 27.65 39.55 28.03 41.14 27.13 41.24 26.07 40.88 25.24 40.55 22.52 39.44 20.95 37.23 20.38 34.4 20.22 33.59 20.1 32.7 20.24 31.88Z"
              />
            </svg>
          </span>
          <span className="brand-copy">
            <strong>Commune<span>X</span></strong>
            <small>SMART COMMUNITIES. CONNECTED SOLUTIONS.</small>
          </span>
        </NavLink>

        <div className="sidebar-profile">
          <div className="avatar">
            {user.profileImageUrl ? (
              <img src={user.profileImageUrl} alt={user.name} />
            ) : (
              (user.name || user.email || "C").slice(0, 1).toUpperCase()
            )}
          </div>
          <div className="sidebar-profile-copy">
            <strong>{user.name}</strong>
            <span>{user.role?.replaceAll("_", " ")}</span>
          </div>
          <span className="status-dot" title="Signed in" />
        </div>

        <nav className="side-navigation" aria-label="Main navigation">
          {navigation.map((section) => <section className="nav-section" key={section.group}>
            <p className="nav-section-title">{section.group}</p>
            {section.links.map(([to, icon, label]) => <NavLink key={to} to={to} onClick={() => setMobileOpen(false)} className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
              <span className="nav-icon" aria-hidden="true">{icon}</span><span>{ur ? (navTranslations[label] || label) : label}</span>
              {to === "/notifications" && unread > 0 && <span className="nav-count">{unread > 9 ? "9+" : unread}</span>}
            </NavLink>)}
          </section>)}
          {user.role === "ADMIN" && <section className="nav-section"><p className="nav-section-title">MANAGEMENT</p><NavLink to="/admin" onClick={() => setMobileOpen(false)} className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}><span className="nav-icon">⚙</span><span>Admin console</span></NavLink></section>}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-tip"><span>✦</span><strong>Small actions, big impact.</strong><p>Good communities are built together.</p></div>
          <button className="nav-link logout-link" onClick={logout}><span className="nav-icon">↪</span><span>Sign out</span></button>
          <small className="sidebar-version">CommuneX · Community platform</small>
        </div>
      </aside>

      <div className="app-main-column">
        <header className="app-topbar">
          <button className="mobile-menu-button" aria-label="Open navigation" onClick={() => setMobileOpen(true)}>☰</button>
          <div className="topbar-breadcrumb"><span>CommuneX</span><span className="breadcrumb-separator">/</span><strong>{titleFromPath(location.pathname)}</strong></div>
          <div className="topbar-actions">
            <button className="topbar-icon" aria-label="Toggle light or dark mode" title="Toggle theme" onClick={() => setTheme(t => t === "dark" ? "light" : "dark")}>{theme === "dark" ? "☀" : "☾"}</button>
            <button className="topbar-icon language-toggle" aria-label="Switch language" title="English / Urdu" onClick={() => setLanguage(l => l === "en" ? "ur" : "en")}>{language === "en" ? "اردو" : "EN"}</button>
            <NavLink aria-label="Open global search" to="/search" className="topbar-icon">⌕</NavLink>
            <NavLink aria-label="Open notifications" to="/notifications" className="topbar-icon notification-top-icon">♢{unread > 0 && <i />}</NavLink>

            <div className="topbar-user">
              <div className="avatar avatar-small">
                {user.profileImageUrl ? (
                  <img src={user.profileImageUrl} alt={user.name} />
                ) : (
                  (user.name || "C").slice(0, 1).toUpperCase()
                )}
              </div>
              <span>{user.name}</span>
            </div>
          </div>
        </header>

        <main className="app-content"><Suspense fallback={<div className="app-loading" role="status"><span className="loading-orbit" /> Loading section…</div>}><Outlet context={{ user, setUser }} /></Suspense></main>
        <footer className="app-footer"><span>© {new Date().getFullYear()} CommuneX</span><span>Smart Communities. Connected Solutions.</span></footer>
      </div>
    </div>
  );
}
