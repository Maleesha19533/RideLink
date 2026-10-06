package com.ridelink.accountservice.service;

import com.ridelink.accountservice.entity.User;
import com.ridelink.accountservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ridelink.accountservice.exception.EmailAlreadyExistsException;
import com.ridelink.accountservice.exception.InvalidCredentialsException;
import com.ridelink.accountservice.exception.UserNotFoundException;
import com.ridelink.accountservice.exception.AccountInactiveException;



@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register User
    public User registerUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
// Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

// Normal registrations are RIDER accounts
        user.setRole("RIDER");

// New accounts are ACTIVE by default
        user.setAccountStatus("ACTIVE");

        return userRepository.save(user);
    }

    // Login User
    public User loginUser(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        ));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new AccountInactiveException("Account is not active");
        }

        return user;
    }

    // Get User by Email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));
    }
    // Update User Profile
    public User updateProfile(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        user.setName(name);
        return userRepository.save(user);
    }
    // Update Account Status
    public User updateAccountStatus(Long userId, String accountStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (accountStatus == null ||
                (!accountStatus.equalsIgnoreCase("ACTIVE") &&
                        !accountStatus.equalsIgnoreCase("INACTIVE"))) {

            throw new RuntimeException(
                    "Account status must be ACTIVE or INACTIVE"
            );
        }

        user.setAccountStatus(accountStatus.toUpperCase());

        return userRepository.save(user);
    }
}