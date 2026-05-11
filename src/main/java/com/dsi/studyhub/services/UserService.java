package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.ChangePasswordDto;
import com.dsi.studyhub.dtos.ProfileUpdateResDto;
import com.dsi.studyhub.dtos.UserReqDto;
import com.dsi.studyhub.dtos.UserResDto;
import com.dsi.studyhub.enums.BadgeType;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface UserService {
    UserResDto getMe();
    ProfileUpdateResDto editUser(UserReqDto userReqDto);
    void changePassword(ChangePasswordDto dto);
    void banUser(Long userId);
    void unbanUser(Long userId);
    UserResDto getuserById(Long userId);
    Page<UserResDto> getAllusers(String firstName, String lastName, String email, Boolean banned, int page, int size);
    UserResDto updatePfp(MultipartFile file) throws IOException;
    Map<String, Long> getUserStats();
    Map<BadgeType, Long> getBadgeDistribution();
    Page<UserResDto> searchByUsername(String username, int page, int size);
    List<Map<String, Object>> getUserGrowth();
}
