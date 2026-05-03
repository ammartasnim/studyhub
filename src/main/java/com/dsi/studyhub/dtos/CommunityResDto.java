package com.dsi.studyhub.dtos;

import java.io.Serializable;

/**
 * DTO for {@link com.dsi.studyhub.entities.Community}
 */

public record CommunityResDto(
        Long id,
        String title,
        String description,
        int nbrMembers,
        Long moderatorId
) implements Serializable {
}
