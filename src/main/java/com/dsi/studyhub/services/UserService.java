package com.dsi.studyhub.services;

import com.dsi.studyhub.dtos.ChangePasswordDto;
import com.dsi.studyhub.dtos.UserReqDto;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.mappers.UserMapper;
import com.dsi.studyhub.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticatedUserService authenticatedUserService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;


        public User getMe() {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("user not found"));
        }

     public User  editUser(UserReqDto userReqDto) {
            User user = authenticatedUserService.getAuthenticatedUser();
            userMapper.partialUpdate(userReqDto, user);
            return  userRepository.save(user);



     }
    public void changePassword(ChangePasswordDto dto) {
        User user = authenticatedUserService.getAuthenticatedUser();

        // Verify current password
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Check new passwords match
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
    }

    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found with id: " + userId));
        user.setBanned(false);
        userRepository.save(user);
    }

    public User getuserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found with id: " + userId));
    }

    public Page<User> getAllusers(String firstName, String lastName,
                                      String email, Boolean banned,
                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());
        return userRepository.findWithFilters(firstName, lastName, email, banned, pageable);
    }

}