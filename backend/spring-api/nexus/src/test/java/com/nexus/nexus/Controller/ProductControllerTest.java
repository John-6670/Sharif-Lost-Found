package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.CategoryDto;
import com.nexus.nexus.Dto.ItemCountsDto;
import com.nexus.nexus.Dto.ProductListItemDto;
import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.UserItemCountsDto;
import com.nexus.nexus.Enumaration.TypeOfReport;
import com.nexus.nexus.Models.ResponseModel;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductPage;
import com.nexus.nexus.Service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllProducts_returnsList() {
        ProductPage<ProductListItemDto> page =
                new ProductPage<>(
                        List.of(ProductListItemDto.builder().id(1L).name("test").build()),
                        0, 5, 1, 1, false
                );
        when(productService.findAllProducts(0, 5)).thenReturn(page);

        ResponseEntity<ResponseModel<ProductPage<ProductListItemDto>>> response =
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

    @Test
    void searchProducts_returnsResults() {
        when(productService.searchProducts("phone"))
                .thenReturn(List.of(ProductResponseDto.builder().id(1L).build()));

        ResponseEntity<ResponseModel<List<ProductResponseDto>>> response = controller.searchProducts("phone");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void searchByLocation_returnsPagedResults() {
        ProductPage<ProductResponseDto> page = new ProductPage<>(
                List.of(ProductResponseDto.builder().id(1L).build()),
                0, 20, 1, 1, false
        );
        when(productService.searchByLocation(
                1.0, 2.0, 3.0, "name", TypeOfReport.FOUND, List.of(2L),
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                OffsetDateTime.parse("2024-01-02T00:00:00Z"),
                0, 20
        )).thenReturn(page);

        ResponseEntity<ResponseModel<ProductPage<ProductResponseDto>>> response =
                controller.searchByLocation(
                        1.0, 2.0, 3.0, "name", TypeOfReport.FOUND, List.of(2L),
                        OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                        OffsetDateTime.parse("2024-01-02T00:00:00Z"),
                        0, 20
                );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().items()).hasSize(1);
    }

    @Test
    void getItemCounts_returnsCounts() {
        when(productService.getItemCounts(any())).thenReturn(ItemCountsDto.builder()
                .todayReported(1)
                .allReported(2)
                .returned(3)
                .build());

        ResponseEntity<ResponseModel<ItemCountsDto>> response = controller.getItemCounts();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getAllReported()).isEqualTo(2);
    }

    @Test
    void getMyItemCounts_requiresPrincipal() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        when(productService.getUserItemCounts(principal))
                .thenReturn(UserItemCountsDto.builder().foundReported(1).lostReported(2).build());

        ResponseEntity<ResponseModel<UserItemCountsDto>> response = controller.getMyItemCounts();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getFoundReported()).isEqualTo(1);
    }

    @Test
    void getCategories_returnsList() {
        when(productService.getAllCategories())
                .thenReturn(List.of(CategoryDto.builder().id(1L).name("phones").build()));

        ResponseEntity<ResponseModel<List<CategoryDto>>> response = controller.getAllCategories();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    void addProduct_returnsOk() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        when(productService.addProduct(any(), eq(principal)))
                .thenReturn(ProductResponseDto.builder().id(3L).build());

        ResponseEntity<ResponseModel<ProductResponseDto>> response =
                controller.addProduct(ProductRequestDto.builder().name("x").build());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getId()).isEqualTo(3L);
    }

    @Test
    void updateProduct_returnsOk() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        when(productService.updateProduct(eq(4L), any(), eq(principal)))
                .thenReturn(ProductResponseDto.builder().id(4L).build());

        ResponseEntity<ResponseModel<ProductResponseDto>> response =
                controller.updateProduct(ProductRequestDto.builder().name("x").build(), 4L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getId()).isEqualTo(4L);
    }

    @Test
    void reportItem_returnsOk() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        ResponseEntity<ResponseModel<Void>> response = controller.reportItem(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getPublicProfileItems_returnsCounts() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        when(productService.getUserItemCounts(principal))
                .thenReturn(UserItemCountsDto.builder().foundReported(2).lostReported(1).build());

        ResponseEntity<ResponseModel<UserItemCountsDto>> response = controller.getPublicProfileItems(42L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getFoundReported()).isEqualTo(2);
    }
}
