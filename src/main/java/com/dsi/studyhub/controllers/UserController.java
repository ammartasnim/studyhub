package com.dsi.studyhub.controllers;

import com.dsi.studyhub.dtos.ChangePasswordDto;
import com.dsi.studyhub.dtos.UserReqDto;
import com.dsi.studyhub.dtos.UserResDto;
import com.dsi.studyhub.entities.User;
import com.dsi.studyhub.mappers.UserMapper;
import com.dsi.studyhub.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@RestController
@RequestMapping("/api/clients")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;  


    @GetMapping("/me")
    public ResponseEntity<UserResDto> getMe() {
        return ResponseEntity.ok(userMapper.toDto(userService.getMe()));
    }
    @PutMapping("/edit")
    public ResponseEntity<UserResDto> editUser(@RequestBody UserReqDto userReqDto) {
        User u=userService.editUser(userReqDto);
        return ResponseEntity.ok(userMapper.toDto(u));
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResDto>> getAllClients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean banned,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> users = userService.getAllusers(firstName, lastName, email, banned, page, size);
        Page<UserResDto> mapped = new PageImpl<>(
                users.getContent().stream().map(userMapper::toDto).toList(),
                users.getPageable(),
                users.getTotalElements()
        );
        return ResponseEntity.ok(mapped);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResDto> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(userMapper.toDto(userService.getuserById(id)));
    }

    @PatchMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> banUser(@PathVariable Long id) {
        userService.banUser(id);
        return ResponseEntity.ok("User banned successfully");
    }

    @PatchMapping("/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.ok("User unbanned successfully");
    }
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordDto dto) {
        userService.changePassword(dto);
        return ResponseEntity.noContent().build();
    }
}
