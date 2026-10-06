package com.ridelink.accountservice;

import com.ridelink.accountservice.repository.UserRepository;
import com.ridelink.accountservice.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ridelink.accountservice.entity.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {


        MockitoAnnotations.openMocks(this);

        userService = new UserService(
                userRepository,
                passwordEncoder
        );
    }
    @Test
    void registerUserSuccessfully() {

        // 1. Create test user
        User user = new User();
        user.setName("Pabasara");
        user.setEmail("test@gmail.com");
        user.setPassword("123456");

        // 2. Mock repository behavior
        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        // 3. Mock password encryption
        when(passwordEncoder.encode("123456"))
                .thenReturn("encryptedPassword");

        // 4. Mock save
        when(userRepository.save(user))
                .thenReturn(user);

        // 5. Call actual service method
        User result = userService.registerUser(user);

        // 6. Check results
        assertNotNull(result);
        assertEquals("Pabasara", result.getName());
        assertEquals("test@gmail.com", result.getEmail());
        assertEquals("encryptedPassword", result.getPassword());
        assertEquals("RIDER", result.getRole());
        assertEquals("ACTIVE", result.getAccountStatus());

        // 7. Verify save happened
        verify(userRepository, times(1)).save(user);
    }
    @Test
    void registerUserWithDuplicateEmail() {

        // 1. Create test user
        User user = new User();
        user.setName("Pabasara");
        user.setEmail("test@gmail.com");
        user.setPassword("123456");

        // 2. Pretend email already exists
        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);

        // 3. Check exception is thrown
        assertThrows(
                com.ridelink.accountservice.exception.EmailAlreadyExistsException.class,
                () -> userService.registerUser(user)
        );

        // 4. User must NOT be saved
        verify(userRepository, never()).save(any(User.class));
    }
    @Test
    void loginUserSuccessfully() {

        // 1. Create existing user
        User user = new User();
        user.setId(1L);
        user.setName("Pabasara");
        user.setEmail("test@gmail.com");
        user.setPassword("encryptedPassword");
        user.setRole("RIDER");
        user.setAccountStatus("ACTIVE");

        // 2. Pretend user exists in database
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(java.util.Optional.of(user));

        // 3. Pretend password is correct
        when(passwordEncoder.matches(
                "123456",
                "encryptedPassword"
        )).thenReturn(true);

        // 4. Call login service
        User result = userService.loginUser(
                "test@gmail.com",
                "123456"
        );

        // 5. Check result
        assertNotNull(result);
        assertEquals("test@gmail.com", result.getEmail());
        assertEquals("RIDER", result.getRole());
        assertEquals("ACTIVE", result.getAccountStatus());

        // 6. Verify password was checked
        verify(passwordEncoder, times(1))
                .matches("123456", "encryptedPassword");
    }
    @Test
    void loginWithWrongPassword() {

        // 1. Create existing ACTIVE user
        User user = new User();
        user.setId(1L);
        user.setName("Pabasara");
        user.setEmail("test@gmail.com");
        user.setPassword("encryptedPassword");
        user.setRole("RIDER");
        user.setAccountStatus("ACTIVE");

        // 2. Pretend user exists
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(java.util.Optional.of(user));

        // 3. Pretend entered password is wrong
        when(passwordEncoder.matches(
                "wrongPassword",
                "encryptedPassword"
        )).thenReturn(false);

        // 4. Login must throw InvalidCredentialsException
        assertThrows(
                com.ridelink.accountservice.exception.InvalidCredentialsException.class,
                () -> userService.loginUser(
                        "test@gmail.com",
                        "wrongPassword"
                )
        );
    }
    @Test
    void loginWithInactiveAccount() {

        // 1. Create INACTIVE user
        User user = new User();
        user.setId(1L);
        user.setName("Pabasara");
        user.setEmail("test@gmail.com");
        user.setPassword("encryptedPassword");
        user.setRole("RIDER");
        user.setAccountStatus("INACTIVE");

        // 2. Pretend user exists
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(java.util.Optional.of(user));

        // 3. Password is correct
        when(passwordEncoder.matches(
                "123456",
                "encryptedPassword"
        )).thenReturn(true);

        // 4. Login must be rejected because account is INACTIVE
        assertThrows(
                com.ridelink.accountservice.exception.AccountInactiveException.class,
                () -> userService.loginUser(
                        "test@gmail.com",
                        "123456"
                )
        );
    }
    @Test
    void updateProfileSuccessfully() {

        // 1. Create existing user
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("test@gmail.com");
        user.setPassword("encryptedPassword");
        user.setRole("RIDER");
        user.setAccountStatus("ACTIVE");

        // 2. Pretend user exists
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(java.util.Optional.of(user));

        // 3. Pretend repository saves updated user
        when(userRepository.save(user))
                .thenReturn(user);

        // 4. Update profile
        User result = userService.updateProfile(
                "test@gmail.com",
                "Pabasara"
        );

        // 5. Check updated name
        assertNotNull(result);
        assertEquals("Pabasara", result.getName());
        assertEquals("test@gmail.com", result.getEmail());

        // 6. Verify save happened once
        verify(userRepository, times(1)).save(user);
    }
    @Test
    void updateAccountStatusSuccessfully() {

        // 1. Create existing user
        User user = new User();
        user.setId(1L);
        user.setName("Pabasara");
        user.setEmail("test@gmail.com");
        user.setRole("RIDER");
        user.setAccountStatus("ACTIVE");

        // 2. Pretend user exists
        when(userRepository.findById(1L))
                .thenReturn(java.util.Optional.of(user));

        // 3. Pretend repository saves updated user
        when(userRepository.save(user))
                .thenReturn(user);

        // 4. Change status
        User result = userService.updateAccountStatus(
                1L,
                "INACTIVE"
        );

        // 5. Check new status
        assertNotNull(result);
        assertEquals("INACTIVE", result.getAccountStatus());

        // 6. Verify database save
        verify(userRepository, times(1)).save(user);
    }
    @Test
    void getUserByEmailWhenUserNotFound() {

        // Pretend user does not exist
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(java.util.Optional.empty());

        // UserNotFoundException must be thrown
        assertThrows(
                com.ridelink.accountservice.exception.UserNotFoundException.class,
                () -> userService.getUserByEmail("unknown@gmail.com")
        );
    }
}