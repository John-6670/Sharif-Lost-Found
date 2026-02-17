package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Security.JwtPrincipal;

import java.util.List;

public interface ProductService {

    List<ProductResponseDto> findAllProducts();

    ProductResponseDto addProduct(ProductRequestDto request, JwtPrincipal principal);

    ProductResponseDto deleteProduct(Long productId, JwtPrincipal principal);

    ProductResponseDto updateProduct(Long productId, ProductRequestDto request, JwtPrincipal principal);

    void reportItem(Long itemId, JwtPrincipal principal);
}
