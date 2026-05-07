package com.dsi.studyhub.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommunityModerationDto {
    private List<BanEntry> bans;
    private List<WarningEntry> warnings;

    @Data
    @Builder
    public static class BanEntry {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String reason;
        private LocalDateTime bannedAt;
    }

    @Data
    @Builder
    public static class WarningEntry {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String reason;
        private LocalDateTime warnedAt;
    }
}