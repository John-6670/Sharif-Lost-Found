package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    @Test
    void getAllProducts_returnsList() {
        com.nexus.nexus.Service.ProductPage<com.nexus.nexus.Dto.ProductListItemDto> page =
                new com.nexus.nexus.Service.ProductPage<>(
                        List.of(com.nexus.nexus.Dto.ProductListItemDto.builder().id(1L).name("test").build()),
                        0, 5, 1, 1, false
                );
        when(productService.findAllProducts(0, 5)).thenReturn(page);

        ResponseEntity<com.nexus.nexus.Models.ResponseModel<com.nexus.nexus.Service.ProductPage<com.nexus.nexus.Dto.ProductListItemDto>>> response =
                controller.getAllProducts(0, 5);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().items()).hasSize(1);
        assertThat(response.getBody().getData().items().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getProductById_returnsItem() {
        when(productService.getProductById(2L))
                .thenReturn(ProductResponseDto.builder().id(2L).name("item").build());

        ResponseEntity<ProductResponseDto> response = controller.getProductById(2L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getId()).isEqualTo(2L);
    }

    @Test
    void deleteProduct_returnsNoContent() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        ResponseEntity<Void> response = controller.deleteProduct(5L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
