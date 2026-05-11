package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.CommunityBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityBanRepository extends JpaRepository<CommunityBan, Long> {
    boolean existsByUserIdAndCommunityId(Long userId, Long communityId);
    Optional<CommunityBan> findByUserIdAndCommunityId(Long userId, Long communityId);
    void deleteByUserIdAndCommunityId(Long userId, Long communityId);
    List<CommunityBan> findByCommunityId(Long communityId);

    @Query("SELECT cb.community.id FROM CommunityBan cb WHERE cb.user.id = :userId")
    List<Long> findBannedCommunityIdsByUserId(@Param("userId") Long userId);
}