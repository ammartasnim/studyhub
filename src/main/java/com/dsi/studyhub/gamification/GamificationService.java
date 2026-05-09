package com.dsi.studyhub.gamification;

import com.dsi.studyhub.entities.Badge;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.services.NotificationService;
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
    private final NotificationService notificationService;

    // XP awarding
    public void awardXp(Long userId, int amount) {
        eventPublisher.publishEvent(new XpEarnedEvent(userId, amount));
    }

    @EventListener
    @Transactional
    public void handleXpEarned(XpEarnedEvent event) {
        // Applies XP, recalculates badge/level, and notifies on new badge.
        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        int newXp = Math.max(0, user.getXpPts() + event.xpAmount());
        user.setXpPts(newXp);

        BadgeType newBadgeType = calculateBadge(newXp);
        boolean alreadyEarned = user.getBadges().stream()
                .anyMatch(b -> b.getType() == newBadgeType);

        int newLevel= calculateLevel(newXp);
        user.setLevel(newLevel);

        System.out.println("DEBUG: User " + user.getUsername() + " now has " + newXp + " XP");
        System.out.println("DEBUG: Calculated Level: " + newLevel);

        if (!alreadyEarned) {
            Badge badgeEntity = new Badge();
            badgeEntity.setType(newBadgeType);
            badgeEntity.setUser(user);

            user.getBadges().add(badgeEntity);

            notificationService.createNotification(
                    user.getId(),
                    "BADGE",
                    "You earned the " + newBadgeType.name() + " badge!",
                    null,
                    user.getId()
            );
        }

        userRepository.save(user);
    }

    private BadgeType calculateBadge(int totalXp) {
        if (totalXp >= 100) return BadgeType.LEGEND;
        if (totalXp >= 80)  return BadgeType.MENTOR;
        if (totalXp >= 60)  return BadgeType.ACHIEVER;
        if (totalXp >= 45)  return BadgeType.COLLABORATOR;
        if (totalXp >= 30)  return BadgeType.CONSISTENT;
        if (totalXp >= 20)  return BadgeType.HELPER;
        if (totalXp >= 15)  return BadgeType.CONTRIBUTOR;
        if (totalXp >= 10)  return BadgeType.EXPLORER;
        if (totalXp >= 5)   return BadgeType.LEARNER;
        return BadgeType.BEGINNER;
    }

    private int calculateLevel(int totalXp) {
        if (totalXp >= 100) return 10;
        if (totalXp >= 80)  return 9;
        if (totalXp >= 60)  return 8;
        if (totalXp >= 45)  return 7;
        if (totalXp >= 30)  return 6;
        if (totalXp >= 20)  return 5;
        if (totalXp >= 15)  return 4;
        if (totalXp >= 10)  return 3;
        if (totalXp >= 5)   return 2;
        return 1;
    }
}
