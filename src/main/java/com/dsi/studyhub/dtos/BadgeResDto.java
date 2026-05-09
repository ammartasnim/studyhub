package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.BadgeType;

import java.io.Serializable;
public record BadgeResDto(Long id, Long userId, BadgeType type) implements Serializable {
}
