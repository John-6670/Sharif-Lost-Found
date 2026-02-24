package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Security.JwtPrincipal;

import java.util.List;

public interface ProductService {

    List<ProductResponseDto> findAllProducts();

    ProductResponseDto getProductById(Long productId);

    ProductResponseDto addProduct(ProductRequestDto request, JwtPrincipal principal);

    ProductResponseDto deleteProduct(Long productId, JwtPrincipal principal);

    ProductResponseDto updateProduct(Long productId, ProductRequestDto request, JwtPrincipal principal);

    void reportItem(Long itemId, JwtPrincipal principal);

    List<ProductResponseDto> searchProducts(String keyword);

    List<ProductResponseDto> searchByLocation(Double centerLat, Double centerLon, Double radiusKm,
                                              String name, com.nexus.nexus.Enumaration.TypeOfReport type,
                                              java.time.OffsetDateTime from, java.time.OffsetDateTime to);
}
