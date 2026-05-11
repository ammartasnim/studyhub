package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.BadgeType;

import java.io.Serializable;
import java.time.LocalDateTime;

public record BadgeResDto(Long id, Long userId, BadgeType type, LocalDateTime earnedAt) implements Serializable {}
