import { lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import VerifyEmail from "./pages/VerifyEmail";
import ProtectedLayout from "./components/ProtectedLayout";

const Dashboard = lazy(() => import("./pages/Dashboard"));
const Community = lazy(() => import("./pages/Community"));
const HelpRequests = lazy(() => import("./pages/HelpRequests"));
const Notifications = lazy(() => import("./pages/Notifications"));
const Facilities = lazy(() => import("./pages/Facilities"));
const Resources = lazy(() => import("./pages/Resources"));
const Volunteers = lazy(() => import("./pages/Volunteers"));
const MapExplorer = lazy(() => import("./pages/MapExplorer"));
const Events = lazy(() => import("./pages/Events"));
const Messages = lazy(() => import("./pages/Messages"));
const Assistant = lazy(() => import("./pages/Assistant"));
const MusicPlayer = lazy(() => import("./pages/MusicPlayer"));
const TextEditor = lazy(() => import("./pages/TextEditor"));
const Games = lazy(() => import("./pages/Games"));
const Analytics = lazy(() => import("./pages/Analytics"));
const Activity = lazy(() => import("./pages/Activity"));
const Admin = lazy(() => import("./pages/Admin"));
const Profile = lazy(() => import("./pages/Profile"));
const Search = lazy(() => import("./pages/Search"));
const Groups = lazy(() => import("./pages/Groups"));
const Announcements = lazy(() => import("./pages/Announcements"));
const Polls = lazy(() => import("./pages/Polls"));
const FocusMode = lazy(() => import("./pages/FocusMode"));
const Impact = lazy(() => import("./pages/Impact"));

function RouteLoading() {
  return (
    <div className="app-loading" role="status" aria-live="polite">
      <span className="loading-orbit" />
      Loading section…
    </div>
  );
}

function App() {
  const token = localStorage.getItem("token");

  return (
    <BrowserRouter>
      <Suspense fallback={<RouteLoading />}>
        <Routes>
          <Route path="/" element={<Navigate to={token ? "/dashboard" : "/login"} replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route element={<ProtectedLayout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/community" element={<Community />} />
            <Route path="/help-requests" element={<HelpRequests />} />
            <Route path="/facilities" element={<Facilities />} />
            <Route path="/resources" element={<Resources />} />
            <Route path="/volunteers" element={<Volunteers />} />
            <Route path="/map" element={<MapExplorer />} />
            <Route path="/events" element={<Events />} />
            <Route path="/groups" element={<Groups />} />
            <Route path="/announcements" element={<Announcements />} />
            <Route path="/polls" element={<Polls />} />
            <Route path="/messages" element={<Messages />} />
            <Route path="/notifications" element={<Notifications />} />
            <Route path="/search" element={<Search />} />
            <Route path="/assistant" element={<Assistant />} />
            <Route path="/music" element={<MusicPlayer />} />
            <Route path="/editor" element={<TextEditor />} />
            <Route path="/games" element={<Games />} />
            <Route path="/focus" element={<FocusMode />} />
            <Route path="/impact" element={<Impact />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/activity" element={<Activity />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/admin" element={<Admin />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
