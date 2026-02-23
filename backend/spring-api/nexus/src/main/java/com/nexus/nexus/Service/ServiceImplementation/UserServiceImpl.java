package com.nexus.nexus.Service.ServiceImplementation;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.nexus.nexus.Dto.UserRegisterDto;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;



    @Override
    public void registerUser(UserRegisterDto dto){
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("This Email is not unique!");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime registrationDate = dto.getCreated_at() != null ? dto.getCreated_at() : now;
        LocalDateTime lastSeen = dto.getLast_seen() != null ? dto.getLast_seen() : now;
        String password = dto.getPassword();
        if (password == null || password.isBlank()) {
            password = UUID.randomUUID().toString();
        }

        User user = User.builder()
                .email(dto.getEmail())
                .fullName(dto.getName())
                .password(password)
                .contact("")
                .applicantItems(List.of())
                .resolvedItems(List.of())
                .registrationDate(registrationDate)
                .lastSeen(lastSeen)
                .otp(null)
                .build();

        userRepository.save(user);
    }

    @Override
    public void updateUser(UserRegisterDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (dto.getId() <= 0) {
            throw new IllegalArgumentException("Valid user id is required");
        }

        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new IllegalArgumentException("This Email is not unique!");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setFullName(dto.getName());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(dto.getPassword());
        }

        if (dto.getLast_seen() != null) {
            user.setLastSeen(dto.getLast_seen());
        }

        userRepository.save(user);
    }

}