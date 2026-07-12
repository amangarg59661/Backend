package com.edss.notifications.infrastructure;

import com.edss.notifications.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Limit limit);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Limit limit);

    long countByUserIdAndReadFalse(UUID userId);

    /**
     * A-08 keyset pagination. Boundary is the {@code (createdAt, id)} tuple of
     * the last item on the previous page. Descending scan means "next page"
     * means "strictly older than the boundary".
     */
    @Query(
            "SELECT n FROM Notification n WHERE n.userId = :userId AND ("
                    + "n.createdAt < :cursorTs OR "
                    + "(n.createdAt = :cursorTs AND n.id < :cursorId)) "
                    + "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findPageAfter(
            @Param("userId") UUID userId,
            @Param("cursorTs") Instant cursorTs,
            @Param("cursorId") UUID cursorId,
            Limit limit);

    @Query(
            "SELECT n FROM Notification n WHERE n.userId = :userId AND n.read = false AND ("
                    + "n.createdAt < :cursorTs OR "
                    + "(n.createdAt = :cursorTs AND n.id < :cursorId)) "
                    + "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findUnreadPageAfter(
            @Param("userId") UUID userId,
            @Param("cursorTs") Instant cursorTs,
            @Param("cursorId") UUID cursorId,
            Limit limit);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllRead(UUID userId);
}
