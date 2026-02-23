package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.UserRegisterDto;
import com.nexus.nexus.Models.ResponseModel;
import com.nexus.nexus.Service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController{

    private final UserService userService;

    @PostMapping()
    public ResponseEntity<ResponseModel<Void>> registerUser(
        @RequestBody UserRegisterDto dto
    ){
        try {
            userService.registerUser(dto);
            return ResponseEntity.ok(ResponseModel.<Void>builder()
                    .success(true)
                    .message("User created in database successfully!")
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseModel.<Void>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseModel.<Void>builder()
                            .success(false)
                            .message("Failed to create user")
                            .build());
        }
    }

    @PutMapping()
    public ResponseEntity<ResponseModel<Void>> updateUser(
            @RequestBody UserRegisterDto dto
    ){
        try {
            userService.updateUser(dto);
            return ResponseEntity.ok(ResponseModel.<Void>builder()
                    .success(true)
                    .message("User updated in database successfully!")
                    .build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseModel.<Void>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseModel.<Void>builder()
                            .success(false)
                            .message("Failed to update user")
                            .build());
        }
    }

}
