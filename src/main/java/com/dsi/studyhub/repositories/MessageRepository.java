package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Message;
import com.dsi.studyhub.entities.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Message m
           SET m.status = :status
         WHERE m.conversation.id = :conversationId
           AND m.sender.id <> :userId
           AND m.status <> :status
    """)
    int markConversationMessages(@Param("conversationId") Long conversationId,
                                 @Param("userId") Long userId,
                                 @Param("status") MessageStatus status);

    @Query("""
        SELECT m FROM Message m
        WHERE m.conversation.id = :conversationId
          AND m.sender.id <> :userId
          AND m.status IN :statuses
    """)
    List<Message> findUnreadMessages(@Param("conversationId") Long conversationId,
                                     @Param("userId") Long userId,
                                     @Param("statuses") List<MessageStatus> statuses);
}
