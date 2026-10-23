package com.communex.community;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.communex.users.User;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final JdbcTemplate jdbc;

    public CommunityController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private long userId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            String email = auth.getPrincipal() instanceof User user ? user.getEmail() : auth.getName();
            Long id = jdbc.queryForObject(
                "SELECT id FROM users WHERE lower(email)=lower(?)",
                Long.class, email
            );
            if (id == null) throw new IllegalStateException();
            return id;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user could not be resolved. Ensure the JWT subject is the user's email."
            );
        }
    }

    private boolean moderator(long id) {
        String role = jdbc.queryForObject(
            "SELECT role FROM users WHERE id=?",
            String.class, id
        );
        return "ADMIN".equalsIgnoreCase(role)
            || "MODERATOR".equalsIgnoreCase(role)
            || "ROLE_ADMIN".equalsIgnoreCase(role)
            || "ROLE_MODERATOR".equalsIgnoreCase(role);
    }

    private String requiredText(Map<String, String> body, String key, int max) {
        String value = body == null ? null : body.get(key);
        if (value == null || value.trim().isEmpty() || value.trim().length() > max) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                key + " is required and must be at most " + max + " characters"
            );
        }
        return value.trim();
    }

    private void requirePostAccess(long postId, long uid) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_posts WHERE id=? AND author_id=?",
            Integer.class, postId, uid
        );
        if (count == null || count == 0) {
            if (!moderator(uid)) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot modify this post"
                );
            }
            Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM community_posts WHERE id=?",
                Integer.class, postId
            );
            if (exists == null || exists == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
            }
        }
    }

    @GetMapping("/feed")
    public Map<String, Object> feed(
        Authentication auth,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        long uid = userId(auth);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;

        List<Map<String, Object>> posts = jdbc.queryForList("""
            SELECT p.id, p.content, p.created_at AS "createdAt",
                   p.updated_at AS "updatedAt",
                   u.id AS "authorId", u.name AS "authorName",
                   u.profile_image_url AS "authorImage",
                   (SELECT COUNT(*) FROM community_likes l
                    WHERE l.post_id=p.id) AS "likeCount",
                   (SELECT COUNT(*) FROM community_comments c
                    WHERE c.post_id=p.id) AS "commentCount",
                   EXISTS(SELECT 1 FROM community_likes l
                    WHERE l.post_id=p.id AND l.user_id=?) AS "likedByMe",
                   EXISTS(SELECT 1 FROM community_bookmarks b
                    WHERE b.post_id=p.id AND b.user_id=?) AS "bookmarkedByMe"
            FROM community_posts p
            JOIN users u ON u.id=p.author_id
            WHERE p.hidden=FALSE
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT ? OFFSET ?
            """, uid, uid, safeSize, offset);

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_posts WHERE hidden=FALSE",
            Long.class
        );

        return Map.of(
            "items", posts,
            "page", safePage,
            "size", safeSize,
            "total", total == null ? 0L : total,
            "hasMore", offset + posts.size() < (total == null ? 0 : total)
        );
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPost(
        Authentication auth, @RequestBody Map<String, String> body
    ) {
        long uid = userId(auth);
        String content = requiredText(body, "content", 5000);
        Long id = jdbc.queryForObject(
            "INSERT INTO community_posts(content,author_id) VALUES (?,?) RETURNING id",
            Long.class, content, uid
        );
        return Map.of("id", id, "content", content, "message", "Post created");
    }

    @PutMapping("/posts/{postId}")
    public Map<String, Object> updatePost(
        Authentication auth,
        @PathVariable long postId,
        @RequestBody Map<String, String> body
    ) {
        long uid = userId(auth);
        requirePostAccess(postId, uid);
        String content = requiredText(body, "content", 5000);
        jdbc.update(
            "UPDATE community_posts SET content=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
            content, postId
        );
        return Map.of("id", postId, "content", content, "message", "Post updated");
    }

    @DeleteMapping("/posts/{postId}")
    public Map<String, String> deletePost(
        Authentication auth, @PathVariable long postId
    ) {
        long uid = userId(auth);
        requirePostAccess(postId, uid);
        jdbc.update("DELETE FROM community_posts WHERE id=?", postId);
        return Map.of("message", "Post deleted");
    }

    @GetMapping("/posts/{postId}/comments")
    public List<Map<String, Object>> comments(
        Authentication auth, @PathVariable long postId
    ) {
        userId(auth);
        return jdbc.queryForList("""
            SELECT c.id, c.post_id AS "postId", c.content,
                   c.created_at AS "createdAt",
                   u.id AS "authorId", u.name AS "authorName",
                   u.profile_image_url AS "authorImage"
            FROM community_comments c
            JOIN users u ON u.id=c.author_id
            JOIN community_posts p ON p.id=c.post_id
            WHERE c.post_id=? AND p.hidden=FALSE
            ORDER BY c.created_at ASC, c.id ASC
            """, postId);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addComment(
        Authentication auth,
        @PathVariable long postId,
        @RequestBody Map<String, String> body
    ) {
        long uid = userId(auth);
        String content = requiredText(body, "content", 2000);
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_posts WHERE id=? AND hidden=FALSE",
            Integer.class, postId
        );
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        Long id = jdbc.queryForObject("""
            INSERT INTO community_comments(post_id,author_id,content)
            VALUES (?,?,?) RETURNING id
            """, Long.class, postId, uid, content);
        return Map.of("id", id, "postId", postId, "content", content);
    }

    @DeleteMapping("/comments/{commentId}")
    public Map<String, String> deleteComment(
        Authentication auth, @PathVariable long commentId
    ) {
        long uid = userId(auth);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT author_id FROM community_comments WHERE id=?", commentId
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
        long owner = ((Number) rows.get(0).get("author_id")).longValue();
        if (owner != uid && !moderator(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        jdbc.update("DELETE FROM community_comments WHERE id=?", commentId);
        return Map.of("message", "Comment deleted");
    }

    @PostMapping("/posts/{postId}/like")
    public Map<String, Object> toggleLike(
        Authentication auth, @PathVariable long postId
    ) {
        long uid = userId(auth);
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_posts WHERE id=? AND hidden=FALSE",
            Integer.class, postId
        );
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }

        Integer liked = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_likes WHERE post_id=? AND user_id=?",
            Integer.class, postId, uid
        );

        boolean nowLiked;
        if (liked != null && liked > 0) {
            jdbc.update(
                "DELETE FROM community_likes WHERE post_id=? AND user_id=?",
                postId, uid
            );
            nowLiked = false;
        } else {
            jdbc.update(
                "INSERT INTO community_likes(post_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",
                postId, uid
            );
            nowLiked = true;
        }

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_likes WHERE post_id=?",
            Integer.class, postId
        );
        return Map.of("liked", nowLiked, "likeCount", count == null ? 0 : count);
    }

    @PostMapping("/posts/{postId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> reportPost(
        Authentication auth,
        @PathVariable long postId,
        @RequestBody Map<String, String> body
    ) {
        long uid = userId(auth);
        String reason = requiredText(body, "reason", 1000);
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_posts WHERE id=?",
            Integer.class, postId
        );
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        Long id = jdbc.queryForObject("""
            INSERT INTO community_reports(post_id,reporter_id,reason)
            VALUES (?,?,?) RETURNING id
            """, Long.class, postId, uid, reason);
        return Map.of("id", id, "status", "OPEN", "message", "Report submitted");
    }

    @GetMapping("/moderation/reports")
    public List<Map<String, Object>> reports(Authentication auth) {
        long uid = userId(auth);
        if (!moderator(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderator access required");
        }
        return jdbc.queryForList("""
            SELECT r.id, r.post_id AS "postId", r.reason, r.status,
                   r.created_at AS "createdAt",
                   p.content AS "postContent",
                   reporter.name AS "reporterName"
            FROM community_reports r
            JOIN community_posts p ON p.id=r.post_id
            JOIN users reporter ON reporter.id=r.reporter_id
            ORDER BY CASE WHEN r.status='OPEN' THEN 0 ELSE 1 END,
                     r.created_at DESC
            """);
    }

    @PatchMapping("/moderation/posts/{postId}/hide")
    public Map<String, String> hidePost(
        Authentication auth, @PathVariable long postId
    ) {
        long uid = userId(auth);
        if (!moderator(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderator access required");
        }
        int changed = jdbc.update(
            "UPDATE community_posts SET hidden=TRUE, updated_at=CURRENT_TIMESTAMP WHERE id=?",
            postId
        );
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        jdbc.update(
            "UPDATE community_reports SET status='REVIEWED' WHERE post_id=? AND status='OPEN'",
            postId
        );
        return Map.of("message", "Post hidden and open reports reviewed");
    }

    @PatchMapping("/moderation/reports/{reportId}/review")
    public Map<String, String> reviewReport(
        Authentication auth, @PathVariable long reportId
    ) {
        long uid = userId(auth);
        if (!moderator(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderator access required");
        }
        int changed = jdbc.update(
            "UPDATE community_reports SET status='REVIEWED' WHERE id=?",
            reportId
        );
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
        }
        return Map.of("message", "Report marked as reviewed");
    }

    @PostMapping("/posts/{postId}/bookmark")
    public Map<String, Object> toggleBookmark(Authentication auth, @PathVariable long postId) {
        long uid = userId(auth);
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_posts WHERE id=? AND hidden=FALSE",
            Integer.class, postId
        );
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        Integer saved = jdbc.queryForObject(
            "SELECT COUNT(*) FROM community_bookmarks WHERE post_id=? AND user_id=?",
            Integer.class, postId, uid
        );
        boolean nowSaved;
        if (saved != null && saved > 0) {
            jdbc.update("DELETE FROM community_bookmarks WHERE post_id=? AND user_id=?", postId, uid);
            nowSaved = false;
        } else {
            jdbc.update("INSERT INTO community_bookmarks(post_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING", postId, uid);
            nowSaved = true;
        }
        return Map.of("bookmarked", nowSaved);
    }

    @GetMapping("/bookmarks")
    public List<Map<String, Object>> bookmarks(Authentication auth) {
        long uid = userId(auth);
        return jdbc.queryForList("""
            SELECT p.id, p.content, p.created_at AS "createdAt", p.updated_at AS "updatedAt",
                   u.id AS "authorId", u.name AS "authorName", u.profile_image_url AS "authorImage",
                   (SELECT COUNT(*) FROM community_likes l WHERE l.post_id=p.id) AS "likeCount",
                   (SELECT COUNT(*) FROM community_comments c WHERE c.post_id=p.id) AS "commentCount",
                   EXISTS(SELECT 1 FROM community_likes l WHERE l.post_id=p.id AND l.user_id=?) AS "likedByMe",
                   TRUE AS "bookmarkedByMe"
            FROM community_bookmarks b JOIN community_posts p ON p.id=b.post_id
            JOIN users u ON u.id=p.author_id
            WHERE b.user_id=? AND p.hidden=FALSE
            ORDER BY b.created_at DESC
            """, uid, uid);
    }
}
