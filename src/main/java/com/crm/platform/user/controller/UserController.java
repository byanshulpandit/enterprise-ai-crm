package com.crm.platform.user.controller;

import com.crm.platform.common.dto.ApiResponse;
import com.crm.platform.common.dto.PageMetadata;
import com.crm.platform.user.dto.UserCreateRequest;
import com.crm.platform.user.dto.UserPasswordUpdateRequest;
import com.crm.platform.user.dto.UserResponse;
import com.crm.platform.user.dto.UserRoleUpdateRequest;
import com.crm.platform.user.entity.User;
import com.crm.platform.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest request) {
        User created = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(UserResponse.fromEntity(created)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
        Page<User> page = userService.listUsers(pageable);
        List<UserResponse> dtos = page.getContent().stream().map(UserResponse::fromEntity).toList();
        PageMetadata pagination = PageMetadata.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success(dtos, pagination));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(user)));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            Authentication authentication) {
        String callerUsername = authentication != null ? authentication.getName() : null;
        User updated = userService.updateUserRole(id, request.getRole(), callerUsername);
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(updated)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable Long id,
            Authentication authentication) {
        String callerUsername = authentication != null ? authentication.getName() : null;
        User updated = userService.deactivateUser(id, callerUsername);
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(updated)));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        userService.updateUserPassword(id, request.getPassword());
        Map<String, String> response = Collections.singletonMap("message", "Password updated successfully");
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
