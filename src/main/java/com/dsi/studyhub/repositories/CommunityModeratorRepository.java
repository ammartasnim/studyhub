package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.CommunityModerator;
import com.dsi.studyhub.enums.CommunityPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityModeratorRepository extends JpaRepository<CommunityModerator, Long> {

    Optional<CommunityModerator> findByUserIdAndCommunityId(Long userId, Long communityId);

    boolean existsByUserIdAndCommunityId(Long userId, Long communityId);

    @Query("SELECT COUNT(cm) > 0 FROM CommunityModerator cm " +
            "WHERE cm.user.id = :userId " +
            "AND cm.community.id = :communityId " +
            "AND :permission MEMBER OF cm.permissions")
    boolean hasPermission(@Param("userId") Long userId,
                          @Param("communityId") Long communityId,
                          @Param("permission") CommunityPermission permission);

    void deleteByCommunityIdAndUserId(Long communityId, Long userId);
}