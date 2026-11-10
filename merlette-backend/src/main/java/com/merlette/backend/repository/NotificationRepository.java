package com.merlette.backend.repository;

import com.merlette.backend.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query(value = """
            SELECT n FROM Notification n LEFT JOIN FETCH n.actor
            WHERE n.recipient.id = :userId AND (:onlyUnread = FALSE OR n.isRead = FALSE)
            """,
            countQuery = """
            SELECT COUNT(n) FROM Notification n
            WHERE n.recipient.id = :userId AND (:onlyUnread = FALSE OR n.isRead = FALSE)
            """)
    Page<Notification> findForUser(@Param("userId") Integer userId,
                                   @Param("onlyUnread") boolean onlyUnread,
                                   Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(Integer id, Integer recipientId);

    long countByRecipientIdAndIsReadFalse(Integer recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = TRUE WHERE n.recipient.id = :userId AND n.isRead = FALSE")
    int markAllAsRead(@Param("userId") Integer userId);
}
