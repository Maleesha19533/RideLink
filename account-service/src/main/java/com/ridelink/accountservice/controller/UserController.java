package com.ridelink.accountservice.controller;

import com.ridelink.accountservice.dto.LoginRequest;
import com.ridelink.accountservice.dto.LoginResponse;
import com.ridelink.accountservice.dto.UserResponse;
import com.ridelink.accountservice.entity.User;
import com.ridelink.accountservice.security.JwtService;
import com.ridelink.accountservice.service.UserService;
import com.ridelink.accountservice.dto.UpdateProfileRequest;
import com.ridelink.accountservice.dto.AccountStatusRequest;
import com.ridelink.accountservice.dto.RegisterRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        User savedUser = userService.registerUser(user);

        UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getAccountStatus()
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest) {

        User user = userService.loginUser(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }




    // =========================
    // GET PROFILE
    // =========================
    @Operation(
            summary = "Get user profile",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader) {

        // Check Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }

        // Remove "Bearer " from token
        String token = authorizationHeader.substring(7);

        // Validate JWT token
        if (!jwtService.isTokenValid(token)) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
        }

        // Get email from JWT
        String email = jwtService.extractEmail(token);

        // Find user
        User user = userService.getUserByEmail(email);

        // Return safe response without password
        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus()
        );

        return ResponseEntity.ok(response);
    }
    // =========================
// UPDATE PROFILE
// =========================
    @Operation(
            summary = "Update user profile",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @Valid @RequestBody UpdateProfileRequest request) {

        // Check Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }

        // Get JWT token
        String token = authorizationHeader.substring(7);

        // Validate token
        if (!jwtService.isTokenValid(token)) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
        }

        // Get logged-in user's email from JWT
        String email = jwtService.extractEmail(token);

        // Update profile
        User updatedUser = userService.updateProfile(
                email,
                request.getName()
        );

        // Safe response - no password
        UserResponse response = new UserResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole(),
                updatedUser.getAccountStatus()
        );

        return ResponseEntity.ok(response);
    }
    // =========================
// UPDATE ACCOUNT STATUS - ADMIN ONLY
// =========================
    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateAccountStatus(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,
            @RequestBody AccountStatusRequest request) {

        // 1. Check Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid Authorization header");
        }

        // 2. Get JWT token
        String token = authorizationHeader.substring(7);

        // 3. Validate JWT token
        if (!jwtService.isTokenValid(token)) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
        }

        // 4. Get role from JWT
        String role = jwtService.extractRole(token);

        // 5. Only ADMIN can change account status
        if (!"ADMIN".equalsIgnoreCase(role)) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access denied. ADMIN role required.");
        }

        // 6. Update account status
        User updatedUser = userService.updateAccountStatus(
                userId,
                request.getAccountStatus()
        );

        // 7. Safe response
        UserResponse response = new UserResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole(),
                updatedUser.getAccountStatus()
        );

        return ResponseEntity.ok(response);
    }
}