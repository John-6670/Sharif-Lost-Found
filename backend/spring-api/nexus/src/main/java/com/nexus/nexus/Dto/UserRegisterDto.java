package com.nexus.nexus.Dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDto{
    private long id;
    private String email;
    private String name;
    private boolean is_verified;
    private LocalDateTime last_seen;
    private LocalDateTime created_at;


}
