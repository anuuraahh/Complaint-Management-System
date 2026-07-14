package com.examly.springapp.service;

import com.examly.springapp.dto.UserResponse;
import com.examly.springapp.dto.UserUpdateRequest;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // FR10.1 / Admin FR3: Get all users
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    // FR10.1: Get user by ID
    public UserResponse getUserById(Long id) {
        return toResponse(findById(id));
    }

    // FR10.1: Get own profile
    public UserResponse getMyProfile(String email) {
        return toResponse(findByEmail(email));
    }

    // FR10.2: Update own profile (or admin updates any)
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, String requesterEmail) {
        User requester = findByEmail(requesterEmail);
        User target = findById(id);

        boolean isSelf = requester.getId().equals(target.getId());
        boolean isAdmin = requester.getRole().name().equals("ADMIN");

        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        if (request.getName() != null) {
            target.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(target.getEmail())) {
            // FR10.3: email must be unique
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + request.getEmail());
            }
            target.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            target.setPhoneNumber(request.getPhoneNumber());
        }

        return toResponse(userRepository.save(target));
    }

    // Admin: Delete user
    @Transactional
    public void deleteUser(Long id, String adminEmail) {
        User admin = findByEmail(adminEmail);
        if (!admin.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Only admins can delete users");
        }
        userRepository.delete(findById(id));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
