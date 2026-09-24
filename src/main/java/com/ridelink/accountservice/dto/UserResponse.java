package com.ridelink.accountservice.dto;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String accountStatus;

    public UserResponse(Long id, String name, String email,
                        String role, String accountStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getAccountStatus() {
        return accountStatus;
    }
}