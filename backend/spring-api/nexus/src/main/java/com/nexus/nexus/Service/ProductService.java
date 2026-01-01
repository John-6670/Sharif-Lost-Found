package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface ProductService {

    ProductResponseDto addProduct(ProductRequestDto request);

    ProductResponseDto deleteProduct(Long ProductId);

    ProductResponseDto updateProduct(Long productId, ProductRequestDto request);
}
