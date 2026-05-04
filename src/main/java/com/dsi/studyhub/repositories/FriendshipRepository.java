package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.Friendship;
import com.dsi.studyhub.entities.FriendshipId;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
        AND f.status = :status
    """)
    Page<Friendship> findAcceptedFriends(@Param("userId") Long userId,
                                         @Param("status") FriendshipStatus status,
                                         Pageable pageable);

    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId AND f.addressee.id = :friendId)
           OR (f.requester.id = :friendId AND f.addressee.id = :userId)
    """)
    Optional<Friendship> findBetweenUsers(@Param("userId") Long userId,
                                          @Param("friendId") Long friendId);

    Page<Friendship> findByAddresseeAndStatus(User addressee,
                                              FriendshipStatus status,
                                              Pageable pageable);

    Page<Friendship> findByRequesterAndStatus(User requester,
                                              FriendshipStatus status,
                                              Pageable pageable);

    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE ((f.requester.id = :userId AND f.addressee.id = :friendId)
           OR (f.requester.id = :friendId AND f.addressee.id = :userId))
        AND f.status = 'ACCEPTED'
    """)
    boolean existsAcceptedFriendship(@Param("userId") Long userId,
                                     @Param("friendId") Long friendId);

    @Query("""
        SELECT f FROM Friendship f
        WHERE f.requester.id = :userId
        AND f.status = 'BLOCKED'
    """)
    Page<Friendship> findBlockedByRequester(@Param("userId") Long userId,
                                            Pageable pageable);
}
