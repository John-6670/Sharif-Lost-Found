package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.Category;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.ItemReport;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.ProductMapper;
import com.nexus.nexus.Repository.CategoryRepository;
import com.nexus.nexus.Repository.ItemReportRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ItemReportRepository itemReportRepository;
    private final ProductMapper productMapper;

    @Override
    public com.nexus.nexus.Service.ProductPage<com.nexus.nexus.Dto.ProductListItemDto> findAllProducts(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        org.springframework.data.domain.Page<Item> pageResult =
                reportRepository.findAllByStatusNot(
                        com.nexus.nexus.Enumaration.Status.REPORTED,
                        org.springframework.data.domain.PageRequest.of(safePage, safeSize)
                );

        List<com.nexus.nexus.Dto.ProductListItemDto> items = productMapper.toListItemDtoList(pageResult.getContent());
        return new com.nexus.nexus.Service.ProductPage<>(
                items,
                safePage,
                safeSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext()
        );
    }

    @Override
    public ProductResponseDto getProductById(Long productId) {
        Item item = reportRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (item.getStatus() == com.nexus.nexus.Enumaration.Status.REPORTED) {
            throw new IllegalArgumentException("Product not found");
        }
        return productMapper.toDto(item);
    }

    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productMapper.toDtoList(
                    reportRepository.findAllByStatusNot(com.nexus.nexus.Enumaration.Status.REPORTED)
            );
        }

        try {
            Long categoryId = Long.parseLong(keyword.trim());
            return productMapper.toDtoList(
                    reportRepository.findAllByCategory_IdAndStatusNot(
                            categoryId,
                            com.nexus.nexus.Enumaration.Status.REPORTED
                    )
            );
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    @Override
    public com.nexus.nexus.Service.ProductPage<ProductResponseDto> searchByLocation(Double centerLat, Double centerLon, Double radiusKm,
                                                                String name, com.nexus.nexus.Enumaration.TypeOfReport type,
                                                                java.util.List<Long> categoryIds,
                                                                java.time.OffsetDateTime from, java.time.OffsetDateTime to,
                                                                int page, int size) {
        boolean anyLocationProvided = centerLat != null || centerLon != null || radiusKm != null;
        boolean allLocationProvided = centerLat != null && centerLon != null && radiusKm != null;
        if (anyLocationProvided && !allLocationProvided) {
            throw new IllegalArgumentException("lat, lon, and radiusKm must be provided together");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        java.math.BigDecimal minLat = null;
        java.math.BigDecimal maxLat = null;
        java.math.BigDecimal minLon = null;
        java.math.BigDecimal maxLon = null;

        if (allLocationProvided) {
            if (radiusKm <= 0) {
                throw new IllegalArgumentException("Radius must be greater than 0");
            }
            double latDelta = radiusKm / 111.0; // ~111 km per degree latitude
            double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));

            minLat = java.math.BigDecimal.valueOf(centerLat - latDelta);
            maxLat = java.math.BigDecimal.valueOf(centerLat + latDelta);
            minLon = java.math.BigDecimal.valueOf(centerLon - lonDelta);
            maxLon = java.math.BigDecimal.valueOf(centerLon + lonDelta);
        }

        String safeName = (name == null || name.isBlank())
                ? null
                : "%" + name.trim().toLowerCase() + "%";

        java.util.List<Long> safeCategoryIds = null;
        if (categoryIds != null) {
            safeCategoryIds = categoryIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            if (safeCategoryIds.isEmpty()) {
                safeCategoryIds = null;
            }
        }

        org.springframework.data.domain.Page<Item> pageResult = reportRepository.searchByLocationAndFilters(
                minLat, maxLat, minLon, maxLon, safeName, type,
                com.nexus.nexus.Enumaration.Status.REPORTED,
                safeCategoryIds, from, to,
                org.springframework.data.domain.PageRequest.of(safePage, safeSize)
        );
        List<ProductResponseDto> items = productMapper.toDtoList(pageResult.getContent());
        return new com.nexus.nexus.Service.ProductPage<>(
                items,
                safePage,
                safeSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext()
        );
    }

    @Override
    public com.nexus.nexus.Dto.ItemCountsDto getItemCounts(java.time.ZoneId zoneId) {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
        java.time.ZonedDateTime startOfDay = now.toLocalDate().atStartOfDay(zoneId);
        java.time.ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        java.time.OffsetDateTime start = startOfDay.toOffsetDateTime();
        java.time.OffsetDateTime end = endOfDay.toOffsetDateTime();

        long todayReported = reportRepository.countByCreatedAtBetween(start, end);
        long allReported = reportRepository.count();
        long returned = reportRepository.countByStatus(com.nexus.nexus.Enumaration.Status.DELIVERED);

        return com.nexus.nexus.Dto.ItemCountsDto.builder()
                .todayReported(todayReported)
                .allReported(allReported)
                .returned(returned)
                .build();
    }

    @Override
    public java.util.List<com.nexus.nexus.Dto.CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Category::getId, java.util.Comparator.nullsLast(Long::compareTo)))
                .map(category -> com.nexus.nexus.Dto.CategoryDto.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .color(category.getColor())
                        .build())
                .toList();
    }

    @Override
    public ProductResponseDto addProduct(ProductRequestDto request, JwtPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        validatePrincipal(principal);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (request.getCategoryId() == null
                && (request.getCategoryName() == null || request.getCategoryName().isBlank())) {
            throw new IllegalArgumentException("Category is required");
        }

        User reporter = resolveReporterFromPrincipal(principal);

        Category category = resolveCategory(request);

        Item item = Item.builder()
                .name(request.getName())
                .description(request.getDescription() != null ? request.getDescription() : request.getNotes())
                .type(request.getType())
                .status(request.getStatus())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .image(parseImageBase64(request.getImage()))
                .category(category)
                .reporter(reporter)
                .build();
        item = reportRepository.save(item);

        return productMapper.toDto(item);
    }

    @Override
    public ProductResponseDto deleteProduct(Long productId, JwtPrincipal principal) {
        validatePrincipal(principal);

        Optional<Item> report = reportRepository.findById(productId);
        if (report.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        Item foundItem = report.get();
        if (!foundItem.getReporter().getEmail().equals(principal.email())) {
            throw new SecurityException("You are not authorized to delete this product");
        }

        reportRepository.delete(foundItem);
        return productMapper.toDto(foundItem);
    }

    @Override
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto request, JwtPrincipal principal) {
        validatePrincipal(principal);

        Optional<Item> report = reportRepository.findById(productId);

        if (report.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }
        Item foundItem = report.get();

        if (!foundItem.getReporter().getEmail().equals(principal.email())) {
            throw new SecurityException("You are not authorized to update this product");
        }

        if (request != null) {
            if (request.getName() != null) {
                foundItem.setName(request.getName());
            }
            if (request.getDescription() != null) {
                foundItem.setDescription(request.getDescription());
            } else if (request.getNotes() != null) {
                foundItem.setDescription(request.getNotes());
            }
            if (request.getType() != null) {
                foundItem.setType(request.getType());
            }
            if (request.getStatus() != null) {
                foundItem.setStatus(request.getStatus());
            }
            if (request.getLatitude() != null) {
                foundItem.setLatitude(request.getLatitude());
            }
            if (request.getLongitude() != null) {
                foundItem.setLongitude(request.getLongitude());
            }
            if (request.getImage() != null) {
                foundItem.setImage(parseImageBase64(request.getImage()));
            }
            if (request.getCategoryId() != null
                    || (request.getCategoryName() != null && !request.getCategoryName().isBlank())) {
                Category category = resolveCategory(request);
                foundItem.setCategory(category);
            }
        }

        foundItem = reportRepository.save(foundItem);
        return productMapper.toDto(foundItem);
    }

    @Override
    @Transactional
    public void reportItem(Long itemId, JwtPrincipal principal) {
        validatePrincipal(principal);
        Item item = reportRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        User reporter = resolveReporterFromPrincipal(principal);
        if (itemReportRepository.existsByItemIdAndReporterId(itemId, reporter.getId())) {
            throw new IllegalArgumentException("You have already reported this item");
        }

        ItemReport report = ItemReport.builder()
                .item(item)
                .reporter(reporter)
                .cause("reported") // placeholder until a request body is added
                .createdAt(OffsetDateTime.now())
                .build();
        itemReportRepository.save(report);

        long reportCount = itemReportRepository.countByItemId(itemId);
        item.setReportedCounts((int) reportCount);
        if (reportCount >= 3) {
            item.setStatus(com.nexus.nexus.Enumaration.Status.REPORTED);
        }
        item.setUpdatedAt(OffsetDateTime.now());
        reportRepository.save(item);
    }

    private void validatePrincipal(JwtPrincipal principal) {
        if (principal == null || principal.email() == null || principal.email().isBlank()) {
            throw new SecurityException("Missing required JWT claims");
        }
        if (!principal.verified()) {
            throw new SecurityException("User is not verified");
        }
    }

    private User resolveReporterFromPrincipal(JwtPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseGet(() -> {
                    String fullName = principal.name() != null && !principal.name().isBlank()
                            ? principal.name()
                            : principal.email().split("@")[0];

                    User newUser = User.builder()
                            .fullName(fullName)
                            .email(principal.email())
                            .password(UUID.randomUUID().toString())
                            .registrationDate(OffsetDateTime.now())
                            .lastSeen(OffsetDateTime.now())
                            .isVerified(true)
                            .isSuperuser(false)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private Category resolveCategory(ProductRequestDto request) {
        if (request.getCategoryId() != null) {
            return categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        }
        return categoryRepository.findByNameIgnoreCase(request.getCategoryName().trim())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    private byte[] parseImageBase64(String image) {
        if (image == null || image.isBlank()) {
            return null;
        }
        String trimmed = image.trim();
        int commaIndex = trimmed.indexOf(',');
        if (commaIndex >= 0) {
            trimmed = trimmed.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(trimmed);
    }
}
