package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.BadgeType;

import java.io.Serializable;
public record BadgeReqDto(Long userId, BadgeType type) implements Serializable {
}
