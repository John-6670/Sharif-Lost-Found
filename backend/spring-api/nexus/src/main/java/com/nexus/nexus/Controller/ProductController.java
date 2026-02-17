package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.ReportItemRequestDto;
import com.nexus.nexus.Models.ResponseModel;
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

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ResponseModel<List<ProductResponseDto>>> getAllProducts() {
        List<ProductResponseDto> response = productService.findAllProducts();
        return ResponseEntity.ok(ResponseModel.<List<ProductResponseDto>>builder()
                .success(true)
                .message("Products fetched successfully")
                .data(response)
                .build());
    }

    @PostMapping
    public ResponseEntity<ResponseModel<ProductResponseDto>> addProduct(
            @RequestBody ProductRequestDto request) {

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUserEmail = authentication.getName();

        ProductResponseDto response = productService.updateProduct(productId, request, authenticatedUserEmail);
        return ResponseEntity.ok(ResponseModel.<ProductResponseDto>builder()
                .success(true)
                .message("Product updated successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{productId}/report")
    public ResponseEntity<ResponseModel<Void>> reportItem(
            @PathVariable Long productId,
            @RequestBody ReportItemRequestDto request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUserEmail = authentication.getName();

        productService.reportItem(productId, request, authenticatedUserEmail);
        return ResponseEntity.ok(ResponseModel.<Void>builder()
                .success(true)
                .message("Item reported successfully")
                .build());
    }
}
