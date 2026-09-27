# RideLink Account Service

## Overview

The Account Service is one of the backend microservices of the RideLink ride-sharing platform.

It is responsible for user registration, authentication, JWT token generation, user profile management, role management, and account status management.

## Technologies

- Java 23
- Spring Boot 4.1.1
- Spring Data JPA
- MySQL
- Maven
- JWT (JSON Web Token)
- BCrypt Password Encryption
- Swagger / OpenAPI
- JUnit
- Mockito
- GitHub Actions

## Main Features

- User Registration
- User Login
- JWT Token Generation
- Password Encryption using BCrypt
- User Profile Retrieval
- User Profile Update
- Role-Based Authorization
- Account Status Management
- Input Validation
- Global Exception Handling
- Swagger API Documentation
- Automated Unit Testing
- Continuous Integration with GitHub Actions

## User Roles

The Account Service supports role-based access.

Current roles include:

- RIDER
- ADMIN

New users are registered as `RIDER` by default.

## Account Status

Supported account statuses:

- ACTIVE
- INACTIVE

Inactive users are prevented from logging in.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users/register` | Register a new user |
| POST | `/api/users/login` | Login and receive JWT token |
| GET | `/api/users/profile` | Get authenticated user profile |
| PUT | `/api/users/profile` | Update authenticated user profile |
| PUT | `/api/users/{userId}/status` | Update account status (ADMIN only) |

## Security

Passwords are encrypted using BCrypt.

Protected endpoints require a JWT token:

```text
Authorization: Bearer <JWT_TOKEN>
```

Sensitive values such as the database password and JWT secret are provided using environment variables.

Required environment variables:

```text
DB_PASSWORD
JWT_SECRET
```

## Database

Database name:

```text
ridelink_account_db
```

The Account Service uses its own MySQL persistence.

## Running the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The service runs on:

```text
http://localhost:8081
```

## Swagger API Documentation

After starting the application, Swagger UI is available at:

```text
http://localhost:8081/swagger-ui/index.html
```

## Running Tests

On Windows:

```powershell
.\mvnw.cmd test
```

The project includes automated unit tests using JUnit and Mockito.

## Continuous Integration

GitHub Actions automatically runs the Maven test suite for changes configured by the CI workflow.

Workflow file:

```text
.github/workflows/maven.yml
```

## Project Structure

```text
src/main/java/com/ridelink/accountservice
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
├── security
└── service
```

## RideLink

This service is developed as part of the RideLink microservices-based ride-sharing backend system.