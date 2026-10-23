package com.communex.messaging;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    @EntityGraph(attributePaths = {"sender", "recipient"})
    @Query("""
            select m
            from DirectMessage m
            where (m.sender.email = :current and m.recipient.email = :other)
               or (m.sender.email = :other and m.recipient.email = :current)
            order by m.sentAt desc, m.id desc
            """)
    List<DirectMessage> latestConversation(
            @Param("current") String current,
            @Param("other") String other,
            Pageable pageable);

    @Query(value = """
            WITH participant_messages AS (
                SELECT
                    CASE WHEN sender.email = :email THEN recipient.email ELSE sender.email END AS partner_email,
                    CASE WHEN sender.email = :email THEN recipient.name ELSE sender.name END AS partner_name,
                    dm.content AS content,
                    dm.sent_at AS sent_at,
                    dm.id AS id,
                    recipient.email AS recipient_email,
                    dm.read_at AS read_at
                FROM direct_messages dm
                JOIN users sender ON sender.id = dm.sender_id
                JOIN users recipient ON recipient.id = dm.recipient_id
                WHERE sender.email = :email OR recipient.email = :email
            ),
            latest_messages AS (
                SELECT DISTINCT ON (partner_email)
                    partner_email AS email,
                    partner_name AS name,
                    content AS last_message,
                    sent_at AS last_message_at
                FROM participant_messages
                ORDER BY partner_email, sent_at DESC, id DESC
            ),
            unread_messages AS (
                SELECT partner_email AS email, COUNT(*) AS unread_count
                FROM participant_messages
                WHERE recipient_email = :email AND read_at IS NULL
                GROUP BY partner_email
            )
            SELECT
                latest_messages.email AS email,
                latest_messages.name AS name,
                latest_messages.last_message AS "lastMessage",
                latest_messages.last_message_at AS "lastMessageAt",
                COALESCE(unread_messages.unread_count, 0) AS "unreadCount"
            FROM latest_messages
            LEFT JOIN unread_messages ON unread_messages.email = latest_messages.email
            ORDER BY latest_messages.last_message_at DESC
            """, nativeQuery = true)
    List<ConversationSummaryProjection> findConversationSummaries(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DirectMessage m
            set m.readAt = :readAt
            where m.recipient.email = :current
              and m.sender.email = :other
              and m.readAt is null
            """)
    int markConversationRead(
            @Param("current") String current,
            @Param("other") String other,
            @Param("readAt") LocalDateTime readAt);

    @Query("select count(m) from DirectMessage m where m.recipient.email = :email and m.readAt is null")
    long countUnread(@Param("email") String email);
}
