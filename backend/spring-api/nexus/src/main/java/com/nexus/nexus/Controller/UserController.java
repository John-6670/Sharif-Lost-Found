package com.nexus.nexus.Controller;
import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.UserRegisterDto;
import com.nexus.nexus.Models.ResponseModel;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping()
    public ResponseEntity<ResponseModel<Void>> registerUser(
        @RequestBody UserRegisterDto dto
    ){
        

        return ResponseEntity.ok(ResponseModel.<Void>builder()
                .success(true)
                .message("user created in database successfully!")
                .build());
    }

    @PutMapping()
    public ResponseEntity<ResponseModel<Void>> updateUser(){
        return ResponseEntity.ok(ResponseModel.<Void>builder()
                .success(true)
                .message("user updated in database successfully!")
                .build());
    }

}