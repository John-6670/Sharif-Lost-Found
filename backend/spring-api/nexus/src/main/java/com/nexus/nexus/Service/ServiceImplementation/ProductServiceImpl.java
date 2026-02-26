package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.CategoryDto;
import com.nexus.nexus.Dto.ItemCountsDto;
import com.nexus.nexus.Dto.ProductListItemDto;
import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.UserItemCountsDto;
import com.nexus.nexus.Entity.Category;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.ItemReport;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Enumaration.Status;
import com.nexus.nexus.Enumaration.TypeOfReport;
import com.nexus.nexus.Mapper.ProductMapper;
import com.nexus.nexus.Repository.CategoryRepository;
import com.nexus.nexus.Repository.ItemReportRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductPage;
import com.nexus.nexus.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    public ProductPage<ProductListItemDto> findAllProducts(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        Page<Item> pageResult =
                reportRepository.findAllByStatus(
                        Status.ACTIVE,
                        PageRequest.of(safePage, safeSize)
                );

        List<ProductListItemDto> items = productMapper.toListItemDtoList(pageResult.getContent());
        return new ProductPage<>(
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
        if (item.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Product not found");
        }
        return productMapper.toDto(item);
    }

    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return productMapper.toDtoList(
                    reportRepository.findAllByStatus(Status.ACTIVE)
            );
        }

        try {
            Long categoryId = Long.parseLong(keyword.trim());
            return productMapper.toDtoList(
                    reportRepository.findAllByCategory_IdAndStatus(
                            categoryId,
                            Status.ACTIVE
                    )
            );
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    @Override
    public ProductPage<ProductResponseDto> searchByLocation(Double centerLat, Double centerLon, Double radiusKm,
                                                           String name, TypeOfReport type,
                                                           List<Long> categoryIds,
                                                           OffsetDateTime from, OffsetDateTime to,
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

        List<Long> safeCategoryIds = null;
        if (categoryIds != null) {
            safeCategoryIds = categoryIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (safeCategoryIds.isEmpty()) {
                safeCategoryIds = null;
            }
        }

        Page<Item> pageResult = reportRepository.searchByLocationAndFilters(
                minLat, maxLat, minLon, maxLon, safeName, type,
                Status.ACTIVE,
                safeCategoryIds, from, to,
                PageRequest.of(safePage, safeSize)
        );
        List<ProductResponseDto> items = productMapper.toDtoList(pageResult.getContent());
        return new ProductPage<>(
                items,
                safePage,
                safeSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext()
        );
    }

    @Override
    public ItemCountsDto getItemCounts(ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime startOfDay = now.toLocalDate().atStartOfDay(zoneId);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        OffsetDateTime start = startOfDay.toOffsetDateTime();
        OffsetDateTime end = endOfDay.toOffsetDateTime();

        long todayReported = reportRepository.countByCreatedAtBetween(start, end);
        long allReported = reportRepository.count();
        long returned = reportRepository.countByStatus(Status.DELIVERED);

        return ItemCountsDto.builder()
                .todayReported(todayReported)
                .allReported(allReported)
                .returned(returned)
                .build();
    }

    @Override
    public UserItemCountsDto getUserItemCounts(JwtPrincipal principal) {
        validatePrincipal(principal);

        User reporter = resolveReporterFromPrincipal(principal);
        Long reporterId = reporter.getId();
        if (reporterId == null) {
            throw new IllegalStateException("Reporter id is missing");
        }

        long foundReported = reportRepository.countByReporter_IdAndType(
                reporterId,
                TypeOfReport.FOUND
        );
        long lostReported = reportRepository.countByReporter_IdAndType(
                reporterId,
                TypeOfReport.LOST
        );

        return UserItemCountsDto.builder()
                .foundReported(foundReported)
                .lostReported(lostReported)
                .build();
    }

    @Override
    public UserItemCountsDto getUserItemCounts(Long userId) {
        if (userId == null) {
            throw new IllegalStateException("Reporter id is missing");
        }

        long foundReported = reportRepository.countByReporter_IdAndType(
                userId,
                TypeOfReport.FOUND
        );
        long lostReported = reportRepository.countByReporter_IdAndType(
                userId,
                TypeOfReport.LOST
        );

        return UserItemCountsDto.builder()
                .foundReported(foundReported)
                .lostReported(lostReported)
                .build();
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getId, Comparator.nullsLast(Long::compareTo)))
                .map(category -> CategoryDto.builder()
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
            item.setStatus(Status.REPORTED);
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
        String raw = request.getCategoryName();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }

        String trimmed = raw.trim();
        try {
            Long categoryId = Long.parseLong(trimmed);
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        } catch (NumberFormatException ignore) {
            return categoryRepository.findByNameIgnoreCase(trimmed)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        }
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
