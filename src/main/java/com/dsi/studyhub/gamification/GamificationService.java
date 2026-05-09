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
        if (totalXp >= 5000) return BadgeType.LEGEND;
        if (totalXp >= 3500)  return BadgeType.MENTOR;
        if (totalXp >= 2500)  return BadgeType.ACHIEVER;
        if (totalXp >= 1500)  return BadgeType.COLLABORATOR;
        if (totalXp >= 900)  return BadgeType.CONSISTENT;
        if (totalXp >= 500)  return BadgeType.HELPER;
        if (totalXp >= 275)  return BadgeType.CONTRIBUTOR;
        if (totalXp >= 150)  return BadgeType.EXPLORER;
        if (totalXp >= 75)   return BadgeType.LEARNER;
        return BadgeType.BEGINNER;
    }

    private int calculateLevel(int totalXp) {
        if (totalXp >= 5000) return 100;
        if (totalXp >= 3500) return 75;
        if (totalXp >= 2500) return 50;
        if (totalXp >= 1500) return 30;
        if (totalXp >= 900)  return 20;
        if (totalXp >= 500)  return 15;
        if (totalXp >= 275)  return 10;
        if (totalXp >= 150)  return 5;
        if (totalXp >= 75)   return 2;
        return 1;
    }
}
