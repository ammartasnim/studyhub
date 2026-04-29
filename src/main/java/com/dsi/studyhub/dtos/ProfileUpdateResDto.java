package com.dsi.studyhub.dtos;

import java.io.Serializable;

public record ProfileUpdateResDto(
        UserResDto user,
        String token
) implements Serializable {}
