package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;

import java.util.List;

public interface ProductService {

    List<ProductResponseDto> findAllProducts();

    ProductResponseDto addProduct(ProductRequestDto request, String authenticatedUserEmail);

    ProductResponseDto deleteProduct(Long productId, String authenticatedUserEmail);

    ProductResponseDto updateProduct(Long productId, ProductRequestDto request, String authenticatedUserEmail);

    void reportItem(Long itemId, String authenticatedUserEmail);
}
