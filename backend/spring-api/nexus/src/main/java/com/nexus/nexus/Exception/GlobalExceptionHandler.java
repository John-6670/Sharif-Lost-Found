package com.nexus.nexus.Exception;

import com.nexus.nexus.Models.ResponseModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends RuntimeException{

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseModel<Void>> illegalArgumentException(IllegalArgumentException ex) {

        ResponseModel<Void> response = ResponseModel.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(response);
    }
}
