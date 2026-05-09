package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.ChangePasswordDto;
import com.dsi.studyhub.dtos.ProfileUpdateResDto;
import com.dsi.studyhub.dtos.UserReqDto;
import com.dsi.studyhub.dtos.UserResDto;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.enums.BadgeType;
import com.dsi.studyhub.mappers.UserMapper;
import com.dsi.studyhub.repositories.UserRepository;
import com.dsi.studyhub.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;

@Service
public class UserService {
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private NotificationService notificationService;


        // Current user lookup
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public UserResDto getMe() {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("user not found"));
            return userMapper.toDto(user);
        }

     @org.springframework.transaction.annotation.Transactional
     public ProfileUpdateResDto editUser(UserReqDto userReqDto) {
            User user = authenticatedUserService.getAuthenticatedUser();
            if (userReqDto.username() != null && !userReqDto.username().isBlank()) {
                user.setUsername(userReqDto.username());
            }
            if (userReqDto.firstName() != null && !userReqDto.firstName().isBlank()) {
                user.setFirstName(userReqDto.firstName());
            }
            if (userReqDto.lastName() != null && !userReqDto.lastName().isBlank()) {
                user.setLastName(userReqDto.lastName());
            }
            if (userReqDto.email() != null && !userReqDto.email().isBlank()) {
                user.setEmail(userReqDto.email());
            }
            if (userReqDto.phone() != null) {
                user.setPhone(userReqDto.phone());
            }
            User saved = userRepository.save(user);
            String newToken = jwtService.generateToken(saved);
            UserResDto dto = userMapper.toDto(saved);
            return new ProfileUpdateResDto(dto, newToken);
     }
    // Password and profile updates
    public void changePassword(ChangePasswordDto dto) {
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found with id: " + userId));
        user.setBanned(true);
        userRepository.save(user);

        notificationService.createNotification(
                userId,
                "BAN",
                "Your account has been banned.",
                null,
                userId
        );
    }

    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found with id: " + userId));
        user.setBanned(false);
        userRepository.save(user);
    }

    // Admin user retrieval and listing
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserResDto getuserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found with id: " + userId));
        return userMapper.toDto(user);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<UserResDto> getAllusers(String firstName, String lastName,
                              String email, Boolean banned,
                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("first_name").ascending());
        String fn = (firstName != null && firstName.isBlank()) ? null : firstName;
        String ln = (lastName  != null && lastName.isBlank())  ? null : lastName;
        String em = (email     != null && email.isBlank())     ? null : email;

        Page<User> userPage = userRepository.findWithFilters(fn, ln, em, banned, pageable);
        List<UserResDto> dtos = userPage.getContent()
                .stream()
                .map(userMapper::toDto)
                .toList();
        return new PageImpl<>(dtos, pageable, userPage.getTotalElements());
    }
    // Avatar updates
    @org.springframework.transaction.annotation.Transactional
    public UserResDto updatePfp(MultipartFile file) throws IOException {
        User currentUser = getMeEntity();
        fileStorageService.deleteFile(currentUser.getPfp());
        String filename = fileStorageService.storeFile(file, "pfp");
        currentUser.setPfp(filename);
        userRepository.save(currentUser);
        return userMapper.toDto(userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new RuntimeException("user not found")));
    }

    private User getMeEntity() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("user not found"));
    }

    // Stats and badges
    public Map<String, Long> getUserStats() {
        return Map.of(
                "total", userRepository.count(),
                "banned", userRepository.countByBanned(true)
        );
    }

    public Map<BadgeType, Long> getBadgeDistribution() {
        List<Object[]> results = userRepository.countGroupedByBadgeRaw();

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (BadgeType) result[0],
                        result -> (Long) result[1]
                ));
    }
    // Search and growth
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<UserResDto> searchByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<User> userPage = userRepository.findByUsernameContainingIgnoreCase(username, pageable);
        List<UserResDto> dtos = userPage.getContent()
                .stream()
                .map(userMapper::toDto)
                .toList();
        return new PageImpl<>(dtos, pageable, userPage.getTotalElements());
    }


    public List<Map<String, Object>> getUserGrowth() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = userRepository.countByCreatedAtBetween(
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()
            );
            result.add(Map.of("date", date.toString(), "count", count));
        }
        return result;
    }


}
