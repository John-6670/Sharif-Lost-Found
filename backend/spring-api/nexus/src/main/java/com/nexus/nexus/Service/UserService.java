package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.UserRegisterDto;

public interface UserService {
    void registerUser(UserRegisterDto dto);
    void updateUser(UserRegisterDto dto);
}
