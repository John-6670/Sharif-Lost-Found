package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Models.ResponseModel;
import com.nexus.nexus.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ResponseModel<ProductResponseDto>> addProduct(
            @RequestBody ProductRequestDto request) {

        // Extract authenticated user email from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUserEmail = authentication.getName();

        ProductResponseDto response = productService.addProduct(request, authenticatedUserEmail);
        return ResponseEntity.ok(ResponseModel.<ProductResponseDto>builder()
                .success(true)
                .message("Product added successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseModel<ProductResponseDto>> deleteProduct(
            @PathVariable Long productId) {

        // Extract authenticated user email for authorization check
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUserEmail = authentication.getName();

        ProductResponseDto response = productService.deleteProduct(productId, authenticatedUserEmail);
        return ResponseEntity.ok(ResponseModel.<ProductResponseDto>builder()
                .success(true)
                .message("Product deleted successfully")
                .data(response)
                .build());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ResponseModel<ProductResponseDto>> updateProduct(
            @RequestBody ProductRequestDto request,
            @PathVariable Long productId) {

        // Extract authenticated user email for authorization check
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUserEmail = authentication.getName();

        ProductResponseDto response = productService.updateProduct(productId, request, authenticatedUserEmail);
        return ResponseEntity.ok(ResponseModel.<ProductResponseDto>builder()
                .success(true)
                .message("Product updated successfully")
                .data(response)
                .build());
    }
}
