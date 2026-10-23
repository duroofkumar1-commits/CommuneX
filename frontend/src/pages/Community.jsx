import { useCallback, useEffect, useState } from "react";
import api from "../services/api";
import "./Community.css";

function formatDate(value) {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      });
}

export default function Community() {
  const [feed, setFeed] = useState([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [openComments, setOpenComments] = useState({});
  const [comments, setComments] = useState({});
  const [commentDrafts, setCommentDrafts] = useState({});
  const [busy, setBusy] = useState({});
  const [reports, setReports] = useState([]);
  const [showModeration, setShowModeration] = useState(false);

  const loadFeed = useCallback(async (requestedPage = 0, append = false) => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get("/community/feed", {
        params: { page: requestedPage, size: 10 },
      });
      const result = response.data;
      setFeed((previous) =>
        append ? [...previous, ...(result.items || [])] : result.items || []
      );
      setPage(result.page || 0);
      setHasMore(Boolean(result.hasMore));
    } catch (err) {
      setError(
        err.response?.data?.detail ||
        err.response?.data?.message ||
        "Could not load the community feed. Please sign in and try again."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadFeed(0, false);
  }, [loadFeed]);

  async function createPost(event) {
    event.preventDefault();
    if (!content.trim() || posting) return;
    setPosting(true);
    setError("");
    setNotice("");
    try {
      await api.post("/community/posts", { content: content.trim() });
      setContent("");
      setNotice("Your post has been published.");
      await loadFeed(0, false);
    } catch (err) {
      setError(err.response?.data?.detail || "Could not publish your post.");
    } finally {
      setPosting(false);
    }
  }

  async function toggleLike(post) {
    setBusy((s) => ({ ...s, ["like-" + post.id]: true }));
    try {
      const { data } = await api.post(`/community/posts/${post.id}/like`);
      setFeed((items) => items.map((item) => item.id === post.id
        ? { ...item, likedByMe: data.liked, likeCount: data.likeCount }
        : item
      ));
    } catch (err) {
      setError(err.response?.data?.detail || "Could not update like.");
    } finally {
      setBusy((s) => ({ ...s, ["like-" + post.id]: false }));
    }
  }


  async function toggleBookmark(post) {
    setBusy((s) => ({ ...s, ["bookmark-" + post.id]: true }));
    try {
      const { data } = await api.post(`/community/posts/${post.id}/bookmark`);
      setFeed((items) => items.map((item) => item.id === post.id ? { ...item, bookmarkedByMe: data.bookmarked } : item));
      setNotice(data.bookmarked ? "Post saved to your bookmarks." : "Post removed from bookmarks.");
    } catch (err) {
      setError(err.response?.data?.detail || "Could not update bookmark.");
    } finally {
      setBusy((s) => ({ ...s, ["bookmark-" + post.id]: false }));
    }
  }

  async function toggleComments(postId) {
    const next = !openComments[postId];
    setOpenComments((s) => ({ ...s, [postId]: next }));
    if (!next || comments[postId]) return;
    try {
      const { data } = await api.get(`/community/posts/${postId}/comments`);
      setComments((s) => ({ ...s, [postId]: data }));
    } catch (err) {
      setError(err.response?.data?.detail || "Could not load comments.");
    }
  }

  async function addComment(event, postId) {
    event.preventDefault();
    const draft = (commentDrafts[postId] || "").trim();
    if (!draft) return;
    setBusy((s) => ({ ...s, ["comment-" + postId]: true }));
    try {
      const { data } = await api.post(`/community/posts/${postId}/comments`, {
        content: draft,
      });
      setComments((s) => ({
        ...s,
        [postId]: [...(s[postId] || []), {
          ...data,
          authorName: "You",
          createdAt: new Date().toISOString(),
        }],
      }));
      setCommentDrafts((s) => ({ ...s, [postId]: "" }));
      setFeed((items) => items.map((item) => item.id === postId
        ? { ...item, commentCount: Number(item.commentCount || 0) + 1 }
        : item
      ));
    } catch (err) {
      setError(err.response?.data?.detail || "Could not add comment.");
    } finally {
      setBusy((s) => ({ ...s, ["comment-" + postId]: false }));
    }
  }

  async function deleteComment(postId, commentId) {
    if (!window.confirm("Delete this comment?")) return;
    try {
      await api.delete(`/community/comments/${commentId}`);
      setComments((s) => ({
        ...s,
        [postId]: (s[postId] || []).filter((c) => c.id !== commentId),
      }));
      setFeed((items) => items.map((item) => item.id === postId
        ? { ...item, commentCount: Math.max(0, Number(item.commentCount || 0) - 1) }
        : item
      ));
    } catch (err) {
      setError(err.response?.data?.detail || "You cannot delete this comment.");
    }
  }

  async function deletePost(post) {
    if (!window.confirm("Delete this post and its comments?")) return;
    try {
      await api.delete(`/community/posts/${post.id}`);
      setFeed((items) => items.filter((item) => item.id !== post.id));
      setNotice("Post deleted.");
    } catch (err) {
      setError(err.response?.data?.detail || "You cannot delete this post.");
    }
  }

  async function reportPost(post) {
    const reason = window.prompt("Why are you reporting this post?");
    if (!reason?.trim()) return;
    try {
      await api.post(`/community/posts/${post.id}/report`, {
        reason: reason.trim(),
      });
      setNotice("Report submitted for moderator review.");
    } catch (err) {
      setError(err.response?.data?.detail || "Could not submit report.");
    }
  }

  async function loadReports() {
    try {
      const { data } = await api.get("/community/moderation/reports");
      setReports(data);
      setShowModeration(true);
      setError("");
    } catch (err) {
      setError(err.response?.status === 403
        ? "Moderator or admin access is required."
        : err.response?.data?.detail || "Could not load reports.");
    }
  }

  async function hidePost(postId) {
    if (!window.confirm("Hide this post from the community feed?")) return;
    try {
      await api.patch(`/community/moderation/posts/${postId}/hide`);
      setFeed((items) => items.filter((item) => item.id !== postId));
      setReports((items) => items.map((r) =>
        r.postId === postId ? { ...r, status: "REVIEWED" } : r
      ));
      setNotice("Post hidden.");
    } catch (err) {
      setError(err.response?.data?.detail || "Moderator access required.");
    }
  }

  async function reviewReport(reportId) {
    try {
      await api.patch(`/community/moderation/reports/${reportId}/review`);
      setReports((items) => items.map((r) =>
        r.id === reportId ? { ...r, status: "REVIEWED" } : r
      ));
    } catch (err) {
      setError(err.response?.data?.detail || "Could not review report.");
    }
  }

  return (
    <main className="community-page">
      <header className="community-header">
        <a className="community-brand" href="/dashboard">CommuneX</a>
        <div>
          <h1>Community</h1>
          <p>Share ideas, connect with neighbours, and help your community.</p>
        </div>
        <button className="community-secondary" onClick={loadReports}>
          Moderation
        </button>
      </header>

      {error && <div className="community-alert error" role="alert">
        {error} <button onClick={() => setError("")} aria-label="Dismiss">×</button>
      </div>}
      {notice && <div className="community-alert success" role="status">
        {notice} <button onClick={() => setNotice("")} aria-label="Dismiss">×</button>
      </div>}

      <section className="community-layout">
        <div className="community-main">
          <form className="post-composer" onSubmit={createPost}>
            <label htmlFor="community-post">Start a conversation</label>
            <textarea
              id="community-post"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="What's happening in your community?"
              maxLength={5000}
              rows={4}
              required
            />
            <div className="composer-footer">
              <span>{content.length}/5000</span>
              <button type="submit" disabled={posting || !content.trim()}>
                {posting ? "Publishing..." : "Publish post"}
              </button>
            </div>
          </form>

          <div className="feed-heading">
            <h2>Latest posts</h2>
            <button className="text-button" onClick={() => loadFeed(0, false)}>
              Refresh
            </button>
          </div>

          {loading && feed.length === 0 && <p className="community-muted">Loading community...</p>}
          {!loading && feed.length === 0 && !error && (
            <div className="community-empty">
              <span className="empty-icon">✳</span>
              <h3>Your community starts here</h3>
              <p>Be the first to share an update, ask a question, or start a discussion.</p>
            </div>
          )}

          {feed.map((post) => (
            <article className="community-post" key={post.id}>
              <div className="post-author-row">
                {post.authorImage
                  ? <img className="author-avatar" src={post.authorImage} alt="" />
                  : <div className="author-avatar avatar-fallback">
                      {(post.authorName || "U").charAt(0).toUpperCase()}
                    </div>}
                <div className="author-meta">
                  <strong>{post.authorName || "Community member"}</strong>
                  <time>{formatDate(post.createdAt)}</time>
                </div>
                <button className="post-menu-button" onClick={() => reportPost(post)}>
                  Report
                </button>
              </div>

              <p className="post-content">{post.content}</p>

              <div className="post-stats">
                <span>{Number(post.likeCount || 0)} likes</span>
                <span>{Number(post.commentCount || 0)} comments</span>
              </div>

              <div className="post-actions">
                <button
                  className={post.likedByMe ? "post-action liked" : "post-action"}
                  disabled={Boolean(busy["like-" + post.id])}
                  onClick={() => toggleLike(post)}
                >
                  <span aria-hidden="true">{post.likedByMe ? "♥" : "♡"}</span>
                  {post.likedByMe ? "Liked" : "Like"}
                </button>
                <button className="post-action" onClick={() => toggleComments(post.id)}>
                  <span aria-hidden="true">▢</span>
                  {openComments[post.id] ? "Hide comments" : "Comment"}
                </button>
                <button className={post.bookmarkedByMe ? "post-action liked" : "post-action"} disabled={Boolean(busy["bookmark-" + post.id])} onClick={() => toggleBookmark(post)}>
                  <span aria-hidden="true">{post.bookmarkedByMe ? "★" : "☆"}</span>
                  {post.bookmarkedByMe ? "Saved" : "Save"}
                </button>
                <button className="post-action delete-action" onClick={() => deletePost(post)}>
                  Delete
                </button>
              </div>

              {openComments[post.id] && (
                <section className="comments-section">
                  {(comments[post.id] || []).map((comment) => (
                    <div className="community-comment" key={comment.id}>
                      <div className="comment-bubble">
                        <strong>{comment.authorName || "Member"}</strong>
                        <p>{comment.content}</p>
                        <time>{formatDate(comment.createdAt)}</time>
                      </div>
                      <button
                        className="comment-delete"
                        onClick={() => deleteComment(post.id, comment.id)}
                        aria-label="Delete comment"
                      >×</button>
                    </div>
                  ))}
                  {comments[post.id]?.length === 0 && (
                    <p className="community-muted">No comments yet. Start the conversation.</p>
                  )}
                  <form className="comment-form" onSubmit={(e) => addComment(e, post.id)}>
                    <input
                      value={commentDrafts[post.id] || ""}
                      onChange={(e) => setCommentDrafts((s) => ({
                        ...s, [post.id]: e.target.value,
                      }))}
                      placeholder="Write a comment..."
                      maxLength={2000}
                      required
                    />
                    <button
                      type="submit"
                      disabled={Boolean(busy["comment-" + post.id])}
                    >Send</button>
                  </form>
                </section>
              )}
            </article>
          ))}

          {hasMore && (
            <button
              className="load-more"
              disabled={loading}
              onClick={() => loadFeed(page + 1, true)}
            >
              {loading ? "Loading..." : "Load more posts"}
            </button>
          )}
          {loading && feed.length > 0 && <p className="community-muted">Refreshing feed...</p>}
        </div>

        <aside className="community-sidebar">
          <section className="community-info-card">
            <span className="info-symbol">✳</span>
            <h3>Better together</h3>
            <p>Share helpful updates, ask neighbours for advice, and keep discussions respectful.</p>
          </section>
          <section className="community-info-card guidelines">
            <h3>Community guidelines</h3>
            <ul>
              <li>Be kind and respectful.</li>
              <li>Protect your personal information.</li>
              <li>Keep posts relevant and constructive.</li>
              <li>Report harmful or abusive content.</li>
            </ul>
          </section>
        </aside>
      </section>

      {showModeration && (
        <div className="moderation-overlay" role="presentation"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) setShowModeration(false);
          }}>
          <section className="moderation-dialog" role="dialog" aria-modal="true"
            aria-labelledby="moderation-title">
            <div className="moderation-heading">
              <div>
                <h2 id="moderation-title">Moderation queue</h2>
                <p>Review community reports and hide inappropriate posts.</p>
              </div>
              <button onClick={() => setShowModeration(false)} aria-label="Close">×</button>
            </div>
            {reports.length === 0 && <p className="community-muted">No reports to display.</p>}
            {reports.map((report) => (
              <article className="report-card" key={report.id}>
                <div className="report-meta">
                  <span>Report #{report.id}</span>
                  <span className={report.status === "OPEN" ? "report-open" : "report-reviewed"}>
                    {report.status}
                  </span>
                </div>
                <p><strong>Reported by:</strong> {report.reporterName}</p>
                <p><strong>Reason:</strong> {report.reason}</p>
                <p><strong>Post:</strong> {report.postContent}</p>
                {report.status === "OPEN" && (
                  <div className="report-actions">
                    <button onClick={() => hidePost(report.postId)}>Hide post</button>
                    <button className="community-secondary"
                      onClick={() => reviewReport(report.id)}>Mark reviewed</button>
                  </div>
                )}
              </article>
            ))}
          </section>
        </div>
      )}
    </main>
  );
}