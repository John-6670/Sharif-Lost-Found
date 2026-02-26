package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.Category;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Enumaration.TypeOfReport;
import com.nexus.nexus.Mapper.ProductMapper;
import com.nexus.nexus.Repository.CategoryRepository;
import com.nexus.nexus.Repository.ItemReportRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ServiceImplementation.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemReportRepository itemReportRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl service;

    @Captor
    private ArgumentCaptor<Item> itemCaptor;

    private JwtPrincipal principal;
    private User reporter;
    private Category category;

    @BeforeEach
    void setUp() {
        principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        reporter = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("User")
                .registrationDate(OffsetDateTime.now())
                .isVerified(true)
                .build();
        category = Category.builder()
                .id(2L)
                .name("phones")
                .build();
    }

    @Test
    void findAllProducts_returnsMappedDtos() {
        Item item = Item.builder().id(10L).build();

        when(reportRepository.findAllByStatus(eq(Status.ACTIVE), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(item)));
        when(productMapper.toListItemDtoList(List.of(item)))
                .thenReturn(List.of(com.nexus.nexus.Dto.ProductListItemDto.builder().id(10L).build()));

        com.nexus.nexus.Service.ProductPage<com.nexus.nexus.Dto.ProductListItemDto> result =
                service.findAllProducts(0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getId()).isEqualTo(10L);
        verify(reportRepository).findAllByStatus(eq(Status.ACTIVE), any(org.springframework.data.domain.Pageable.class));
        verify(productMapper).toListItemDtoList(List.of(item));
    }

    @Test
    void findAllProducts_usesSafePaging() {
        when(reportRepository.findAllByStatus(eq(Status.ACTIVE), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        service.findAllProducts(-1, 0);

        verify(reportRepository).findAllByStatus(eq(Status.ACTIVE), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void addProduct_persistsAndMaps() {
        ProductRequestDto request = ProductRequestDto.builder()
                .name("test")
                .description("d")
                .type(TypeOfReport.FOUND)
                .status(Status.ACTIVE)
                .latitude(new BigDecimal("35.7"))
                .longitude(new BigDecimal("51.3"))
                .categoryName("phones")
                .build();

        when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(reporter));
        when(categoryRepository.findByNameIgnoreCase(eq("phones"))).thenReturn(Optional.of(category));
        when(reportRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toDto(any(Item.class))).thenReturn(ProductResponseDto.builder().id(1L).build());

        ProductResponseDto result = service.addProduct(request, principal);

        assertThat(result.getId()).isEqualTo(1L);
        verify(reportRepository).save(itemCaptor.capture());
        Item saved = itemCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("test");
        assertThat(saved.getCategory().getId()).isEqualTo(2L);
        assertThat(saved.getReporter().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void updateProduct_blocksNonOwner() {
        Item item = Item.builder()
                .id(5L)
                .reporter(User.builder().email("other@example.com").build())
                .build();

        when(reportRepository.findById(5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.updateProduct(5L, ProductRequestDto.builder().build(), principal))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void updateProduct_updatesFieldsForOwner() {
        Item item = Item.builder()
                .id(5L)
                .name("old")
                .description("old desc")
                .reporter(User.builder().email("user@example.com").build())
                .status(Status.ACTIVE)
                .build();

        ProductRequestDto request = ProductRequestDto.builder()
                .name("new")
                .description("new desc")
                .status(Status.DELIVERED)
                .build();

        when(reportRepository.findById(5L)).thenReturn(Optional.of(item));
        when(reportRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toDto(any(Item.class))).thenReturn(ProductResponseDto.builder().id(5L).build());

        ProductResponseDto result = service.updateProduct(5L, request, principal);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(item.getName()).isEqualTo("new");
        assertThat(item.getDescription()).isEqualTo("new desc");
        assertThat(item.getStatus()).isEqualTo(Status.DELIVERED);
    }

    @Test
    void reportItem_rejectsDuplicateReport() {
        Item item = Item.builder()
                .id(7L)
                .reportedCounts(1)
                .build();

        when(reportRepository.findById(7L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(reporter));
        when(itemReportRepository.existsByItemIdAndReporterId(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.reportItem(7L, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reported");
    }

    @Test
    void reportItem_updatesReportedCountsAndStatus() {
        Item item = Item.builder()
                .id(7L)
                .reportedCounts(1)
                .build();

        when(reportRepository.findById(7L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(reporter));
        when(itemReportRepository.existsByItemIdAndReporterId(7L, 1L)).thenReturn(false);
        when(itemReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemReportRepository.countByItemId(7L)).thenReturn(3L);

        service.reportItem(7L, principal);

        assertThat(item.getReportedCounts()).isEqualTo(3);
        assertThat(item.getStatus()).isEqualTo(Status.REPORTED);
        verify(reportRepository).save(item);
    }

    @Test
    void reportItem_updatesReportedCountsWithoutChangingStatus() {
        Item item = Item.builder()
                .id(7L)
                .reportedCounts(1)
                .status(Status.ACTIVE)
                .build();

        when(reportRepository.findById(7L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(reporter));
        when(itemReportRepository.existsByItemIdAndReporterId(7L, 1L)).thenReturn(false);
        when(itemReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemReportRepository.countByItemId(7L)).thenReturn(2L);

        service.reportItem(7L, principal);

        assertThat(item.getReportedCounts()).isEqualTo(2);
        assertThat(item.getStatus()).isEqualTo(Status.ACTIVE);
        verify(reportRepository).save(item);
    }

    @Test
    void getItemCounts_returnsAggregates() {
        when(reportRepository.countByCreatedAtBetween(any(), any())).thenReturn(2L);
        when(reportRepository.count()).thenReturn(10L);
        when(reportRepository.countByStatus(Status.DELIVERED)).thenReturn(4L);

        com.nexus.nexus.Dto.ItemCountsDto counts = service.getItemCounts(java.time.ZoneId.of("UTC"));

        assertThat(counts.getTodayReported()).isEqualTo(2L);
        assertThat(counts.getAllReported()).isEqualTo(10L);
        assertThat(counts.getReturned()).isEqualTo(4L);
    }

    @Test
    void getUserItemCounts_usesReporterId() {
        when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(reporter));
        when(reportRepository.countByReporter_IdAndType(1L, TypeOfReport.FOUND)).thenReturn(5L);
        when(reportRepository.countByReporter_IdAndType(1L, TypeOfReport.LOST)).thenReturn(2L);

        com.nexus.nexus.Dto.UserItemCountsDto counts = service.getUserItemCounts(principal);

        assertThat(counts.getFoundReported()).isEqualTo(5L);
        assertThat(counts.getLostReported()).isEqualTo(2L);
    }

    @Test
    void searchByLocation_rejectsPartialCoordinates() {
        assertThatThrownBy(() -> service.searchByLocation(1.0, null, 2.0, null, null, null, null, null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lat, lon, and radiusKm");
    }

    @Test
    void addProduct_rejectsMissingName() {
        ProductRequestDto request = ProductRequestDto.builder()
                .type(TypeOfReport.FOUND)
                .status(Status.ACTIVE)
                .latitude(new BigDecimal("35.7"))
                .longitude(new BigDecimal("51.3"))
                .categoryName("phones")
                .build();

        assertThatThrownBy(() -> service.addProduct(request, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name is required");
    }
}
