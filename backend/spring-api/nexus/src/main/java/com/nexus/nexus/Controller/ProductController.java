package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.ProductListItemDto;
import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.ItemCountsDto;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ResponseModel<com.nexus.nexus.Service.ProductPage<ProductListItemDto>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        com.nexus.nexus.Service.ProductPage<ProductListItemDto> response = productService.findAllProducts(page, size);
        String message = response.items().isEmpty() ? "No items found" : "Items fetched successfully";
        return ResponseEntity.ok(ResponseModel.<com.nexus.nexus.Service.ProductPage<ProductListItemDto>>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(
            @PathVariable Long productId) {
        ProductResponseDto response = productService.getProductById(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseModel<List<ProductResponseDto>>> searchProducts(
            @RequestParam String keyword) {
        List<ProductResponseDto> response = productService.searchProducts(keyword);
        String message = response.isEmpty()
                ? "No items found matching '" + keyword + "'"
                : "Search results fetched successfully";
        return ResponseEntity.ok(ResponseModel.<List<ProductResponseDto>>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    @GetMapping("/search/location")
    public ResponseEntity<ResponseModel<com.nexus.nexus.Service.ProductPage<ProductResponseDto>>> searchByLocation(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) com.nexus.nexus.Enumaration.TypeOfReport type,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.OffsetDateTime from,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        com.nexus.nexus.Service.ProductPage<ProductResponseDto> response =
                productService.searchByLocation(lat, lon, radiusKm, name, type, categoryIds, from, to, page, size);
        String message = response.items().isEmpty()
                ? "No items found in the specified area"
                : "Location search results fetched successfully";
        return ResponseEntity.ok(ResponseModel.<com.nexus.nexus.Service.ProductPage<ProductResponseDto>>builder()
                .success(true)
                .message(message)
                .data(response)
                .build());
    }

    @GetMapping("/counts")
    public ResponseEntity<ResponseModel<ItemCountsDto>> getItemCounts() {
        ItemCountsDto counts = productService.getItemCounts(ZoneId.of("Asia/Tehran"));
        return ResponseEntity.ok(ResponseModel.<ItemCountsDto>builder()
                .success(true)
                .message("Counts fetched successfully")
                .data(counts)
                .build());
    }

    @GetMapping("/counts/me")
    public ResponseEntity<ResponseModel<com.nexus.nexus.Dto.UserItemCountsDto>> getMyItemCounts() {
        JwtPrincipal principal = getJwtPrincipal();
        com.nexus.nexus.Dto.UserItemCountsDto counts = productService.getUserItemCounts(principal);
        return ResponseEntity.ok(ResponseModel.<com.nexus.nexus.Dto.UserItemCountsDto>builder()
                .success(true)
                .message("User counts fetched successfully")
                .data(counts)
                .build());
    }

    @GetMapping("/counts/{userId}")
    public ResponseEntity<ResponseModel<com.nexus.nexus.Dto.UserItemCountsDto>> getPublicProfileItems(
        @PathVariable Long userId
    ) {
        JwtPrincipal principal = getJwtPrincipal();
        com.nexus.nexus.Dto.UserItemCountsDto counts = productService.getUserItemCounts(principal);
        return ResponseEntity.ok(ResponseModel.<com.nexus.nexus.Dto.UserItemCountsDto>builder()
                .success(true)
                .message("User counts fetched successfully")
                .data(counts)
                .build());
    }

    @GetMapping("/categories")
    public ResponseEntity<ResponseModel<List<com.nexus.nexus.Dto.CategoryDto>>> getAllCategories() {
        List<com.nexus.nexus.Dto.CategoryDto> categories = productService.getAllCategories();
        String message = categories.isEmpty() ? "No categories found" : "Categories fetched successfully";
        return ResponseEntity.ok(ResponseModel.<List<com.nexus.nexus.Dto.CategoryDto>>builder()
                .success(true)
                .message(message)
                .data(categories)
                .build());
    }

    @PostMapping
    public ResponseEntity<ResponseModel<ProductResponseDto>> addProduct(
            @RequestBody ProductRequestDto request) {

        JwtPrincipal principal = getJwtPrincipal();

        ProductResponseDto response = productService.addProduct(request, principal);
        return ResponseEntity.ok(ResponseModel.<ProductResponseDto>builder()
                .success(true)
                .message("Product added successfully")
                .data(response)
                .build());
    }

    @DeleteMapping({"/{productId}", "/{productId}/"})
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId) {

        JwtPrincipal principal = getJwtPrincipal();

        productService.deleteProduct(productId, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ResponseModel<ProductResponseDto>> updateProduct(
            @RequestBody ProductRequestDto request,
            @PathVariable Long productId) {

        JwtPrincipal principal = getJwtPrincipal();

        ProductResponseDto response = productService.updateProduct(productId, request, principal);
        return ResponseEntity.ok(ResponseModel.<ProductResponseDto>builder()
                .success(true)
                .message("Product updated successfully")
                .data(response)
                .build());
    }

    @PostMapping("/{productId}/report")
    public ResponseEntity<ResponseModel<Void>> reportItem(
            @PathVariable Long productId) {

        JwtPrincipal principal = getJwtPrincipal();

        productService.reportItem(productId, principal);
        return ResponseEntity.ok(ResponseModel.<Void>builder()
                .success(true)
                .message("Item reported successfully")
                .build());
    }

    private JwtPrincipal getJwtPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new SecurityException("Missing or invalid JWT principal");
        }
        return principal;
    }
}
