package com.nexus.nexus.Service.ServiceImplementation;


import java.util.List;

import org.springframework.stereotype.Service;

import com.nexus.nexus.Dto.UserRegisterDto;
import com.nexus.nexus.Entity.Otp;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;



    public String registerUser(UserRegisterDto dto){
        if(userRepository.findById(dto.getId()).isPresent())
            return "This id is not valid!";
        if(userRepository.findByEmail(dto.getEmail()).isPresent())
            return "This Email is not unique!";
        
        User user = User.builder()
                    .id(dto.getId())
                    .email(dto.getEmail())
                    .fullName(dto.getName())
                    .contact("")
                    .applicantItems(List.of())
                    .resolvedItems(List.of())
                    .registrationDate(dto.getCreated_at())
                    .lastSeen(dto.getLast_seen())
                    .otp(null)
                    .build();
        

        try{userRepository.save(user);

        }catch(Exception e){
            return "Error in saving new user : " + e.getMessage();
        }
        return "new user registered successfully!";

    }

}
