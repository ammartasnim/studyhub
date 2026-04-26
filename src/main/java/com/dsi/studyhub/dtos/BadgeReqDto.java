package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.BadgeType;

import java.io.Serializable;

/**
 * DTO for {@link com.dsi.studyhub.entities.Badge}
 */
public record BadgeReqDto(Long userId, BadgeType type) implements Serializable {
}
