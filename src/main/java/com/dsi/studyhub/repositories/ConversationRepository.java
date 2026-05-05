package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("""
        SELECT c FROM Conversation c
        WHERE (c.userOneId = :userA AND c.userTwoId = :userB)
           OR (c.userOneId = :userB AND c.userTwoId = :userA)
    """)
    Optional<Conversation> findBetweenUsers(@Param("userA") Long userA, @Param("userB") Long userB);

    List<Conversation> findByUserOneIdOrUserTwoId(Long userOneId, Long userTwoId);
}
