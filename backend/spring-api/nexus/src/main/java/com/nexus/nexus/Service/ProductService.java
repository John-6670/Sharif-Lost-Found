package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.CategoryDto;
import com.nexus.nexus.Dto.ItemCountsDto;
import com.nexus.nexus.Dto.ProductListItemDto;
import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.UserItemCountsDto;
import com.nexus.nexus.Enumaration.TypeOfReport;
import com.nexus.nexus.Security.JwtPrincipal;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public interface ProductService {

    ProductPage<ProductListItemDto> findAllProducts(int page, int size);

    ProductResponseDto getProductById(Long productId);

    ProductResponseDto addProduct(ProductRequestDto request, JwtPrincipal principal);

    ProductResponseDto deleteProduct(Long productId, JwtPrincipal principal);

    ProductResponseDto updateProduct(Long productId, ProductRequestDto request, JwtPrincipal principal);

    void reportItem(Long itemId, JwtPrincipal principal);

    List<ProductResponseDto> searchProducts(String keyword);

    ProductPage<ProductResponseDto> searchByLocation(Double centerLat, Double centerLon, Double radiusKm,
                                                     String name, TypeOfReport type,
                                                     List<Long> categoryIds,
                                                     OffsetDateTime from, OffsetDateTime to,
                                                     int page, int size);

    ItemCountsDto getItemCounts(ZoneId zoneId);

    UserItemCountsDto getUserItemCounts(JwtPrincipal principal);

    UserItemCountsDto getUserItemCounts(Long userId);

    List<CategoryDto> getAllCategories();
}
