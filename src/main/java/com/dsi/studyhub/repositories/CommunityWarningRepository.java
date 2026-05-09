package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.CommunityWarning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityWarningRepository extends JpaRepository<CommunityWarning, Long> {
    Page<CommunityWarning> findByUserIdAndCommunityId(Long userId, Long communityId, Pageable pageable);
    long countByUserIdAndCommunityId(Long userId, Long communityId);

    List<CommunityWarning> findByCommunityId(Long communityId);
}
