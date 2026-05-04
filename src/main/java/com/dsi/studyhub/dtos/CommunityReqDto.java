package com.dsi.studyhub.dtos;

import com.dsi.studyhub.enums.CommunityPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Set;

public record CommunityReqDto(
        @NotBlank String title,
        @NotBlank String description,
        Integer nbrMembers,
        String category
) implements Serializable {

    public record AddModeratorReq(
            @NotNull Long userId,
            Set<CommunityPermission> permissions
    ) implements Serializable {}

    public record UpdatePermissionsReq(
            @NotNull Set<CommunityPermission> permissions
    ) implements Serializable {}

    public record TransferOwnershipReq(
            @NotNull Long newOwnerId
    ) implements Serializable {}
    public record BanMemberReq(
            @NotNull Long userId,
            String reason
    ) implements Serializable {}

    public record WarnMemberReq(
            @NotNull Long userId,
            String reason
    ) implements Serializable {}
}