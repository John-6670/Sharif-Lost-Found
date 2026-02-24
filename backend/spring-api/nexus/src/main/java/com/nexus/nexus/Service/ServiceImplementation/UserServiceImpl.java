package com.nexus.nexus.Service.ServiceImplementation;


import java.time.OffsetDateTime;
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

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime registrationDate = dto.getCreatedAt() != null ? dto.getCreatedAt() : now;
        OffsetDateTime lastSeen = dto.getLastSeen() != null ? dto.getLastSeen() : now;
        String password = dto.getPassword();
        if (password == null || password.isBlank()) {
            password = UUID.randomUUID().toString();
        }

        User user = User.builder()
                .email(dto.getEmail())
                .fullName(dto.getName())
                .password(password)
                .isSuperuser(false)
                .isVerified(dto.isVerified())
                .registrationDate(registrationDate)
                .lastSeen(lastSeen)
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

        if (dto.getLastSeen() != null) {
            user.setLastSeen(dto.getLastSeen());
        }

        user.setIsVerified(dto.isVerified());

        userRepository.save(user);
    }

}
