package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Security.JwtPrincipal;

import java.util.List;

public interface ProductService {

    com.nexus.nexus.Service.ProductPage<com.nexus.nexus.Dto.ProductListItemDto> findAllProducts(int page, int size);

    ProductResponseDto getProductById(Long productId);

    ProductResponseDto addProduct(ProductRequestDto request, JwtPrincipal principal);

    ProductResponseDto deleteProduct(Long productId, JwtPrincipal principal);

    ProductResponseDto updateProduct(Long productId, ProductRequestDto request, JwtPrincipal principal);

    void reportItem(Long itemId, JwtPrincipal principal);

    List<ProductResponseDto> searchProducts(String keyword);

    com.nexus.nexus.Service.ProductPage<ProductResponseDto> searchByLocation(Double centerLat, Double centerLon, Double radiusKm,
                                                                             String name, com.nexus.nexus.Enumaration.TypeOfReport type,
                                                                             java.util.List<Long> categoryIds,
                                                                             java.time.OffsetDateTime from, java.time.OffsetDateTime to,
                                                                             int page, int size);

    com.nexus.nexus.Dto.ItemCountsDto getItemCounts(java.time.ZoneId zoneId);

    com.nexus.nexus.Dto.UserItemCountsDto getUserItemCounts(JwtPrincipal principal);

    com.nexus.nexus.Dto.UserItemCountsDto getUserItemCounts(Long userId);

    java.util.List<com.nexus.nexus.Dto.CategoryDto> getAllCategories();
}
