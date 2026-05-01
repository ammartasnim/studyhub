package com.dsi.studyhub.repositories;

import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
            "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
            "(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:banned IS NULL OR u.banned = :banned)")
    Page<User> findWithFilters(@Param("firstName") String firstName,
                               @Param("lastName") String lastName,
                               @Param("email") String email,
                               @Param("banned") Boolean banned,
                               Pageable pageable);

    long countByBanned(boolean banned);

    @Query("SELECT b.type, COUNT(b) FROM User u JOIN u.badges b GROUP BY b.type")
    List<Object[]> countGroupedByBadgeRaw();

    // For growth chart — requires createdAt on User entity
//    @Query(value = """
//    SELECT CAST(created_at AS DATE) as date, COUNT(*) as count
//    FROM users
//    WHERE created_at BETWEEN :from AND :to
//    GROUP BY CAST(created_at AS DATE)
//    ORDER BY date
//    """, nativeQuery = true)
//    List<DailyCountDto> countByCreatedAtBetweenGroupByDay(LocalDate from, LocalDate to);
}