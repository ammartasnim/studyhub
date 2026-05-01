package com.dsi.studyhub.gamification;

import com.dsi.studyhub.entities.Badge;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Called by other services to award XP
    public void awardXp(Long userId, int amount) {
        eventPublisher.publishEvent(new XpEarnedEvent(userId, amount));
    }

    @EventListener
    @Transactional
    public void handleXpEarned(XpEarnedEvent event) {
        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        int newXp = Math.max(0, user.getXpPts() + event.xpAmount()); // XP can't go below 0
        user.setXpPts(newXp);

        BadgeType newBadgeType = calculateBadge(newXp);
        boolean alreadyEarned = user.getBadges().stream()
                .anyMatch(b -> b.getType() == newBadgeType);

        int newLevel = newBadgeType.ordinal() + 1;
        if (newLevel > user.getLevel()) {
            user.setLevel(newLevel);
        }

        if (!alreadyEarned) {
            Badge badgeEntity = new Badge();
            badgeEntity.setType(newBadgeType);
            badgeEntity.setUser(user);

            user.getBadges().add(badgeEntity);
            // TODO: push a notification to the user via WebSocket or save to DB
            // e.g. notificationService.send(user.getId(), "You've earned the " + newBadge + " badge!");
        }

        userRepository.save(user);
    }

    private BadgeType calculateBadge(int totalXp) {
        if (totalXp >= 35000) return BadgeType.LEGEND;
        if (totalXp >= 20000) return BadgeType.MENTOR;
        if (totalXp >= 12000) return BadgeType.ACHIEVER;
        if (totalXp >= 7000)  return BadgeType.COLLABORATOR;
        if (totalXp >= 4000)  return BadgeType.CONSISTENT;
        if (totalXp >= 2000)  return BadgeType.HELPER;
        if (totalXp >= 1000)  return BadgeType.CONTRIBUTOR;
        if (totalXp >= 500)   return BadgeType.EXPLORER;
        if (totalXp >= 200)   return BadgeType.LEARNER;
        return BadgeType.BEGINNER;
    }

    private int calculateLevel(int totalXp) {
        if (totalXp >= 35000) return 10; // Legend
        if (totalXp >= 20000) return 9;  // Mentor
        if (totalXp >= 12000) return 8;  // Achiever
        if (totalXp >= 7000)  return 7;  // Collaborator
        if (totalXp >= 4000)  return 6;  // Consistent
        if (totalXp >= 2000)  return 5;  // Helper
        if (totalXp >= 1000)  return 4;  // Contributor
        if (totalXp >= 500)   return 3;  // Explorer
        if (totalXp >= 200)   return 2;  // Learner
        return 1; // Beginner
    }
}
