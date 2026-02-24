package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.Category;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.ProductMapper;
import com.nexus.nexus.Repository.CategoryRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponseDto> findAllProducts() {
        List<Item> items = reportRepository.findAll();
        return productMapper.toDtoList(items);
    }

    @Override
    public ProductResponseDto getProductById(Long productId) {
        Item item = reportRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return productMapper.toDto(item);
    }

    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAllProducts();
        }

        try {
            Long categoryId = Long.parseLong(keyword.trim());
            return productMapper.toDtoList(reportRepository.findAllByCategory_Id(categoryId));
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    @Override
    public List<ProductResponseDto> searchByLocation(Double centerLat, Double centerLon, Double radiusKm,
                                                     String name, com.nexus.nexus.Enumaration.TypeOfReport type,
                                                     java.time.OffsetDateTime from, java.time.OffsetDateTime to) {
        boolean anyLocationProvided = centerLat != null || centerLon != null || radiusKm != null;
        boolean allLocationProvided = centerLat != null && centerLon != null && radiusKm != null;
        if (anyLocationProvided && !allLocationProvided) {
            throw new IllegalArgumentException("lat, lon, and radiusKm must be provided together");
        }

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

        List<Item> items = reportRepository.searchByLocationAndFilters(
                minLat, maxLat, minLon, maxLon, safeName, type, from, to
        );
        return productMapper.toDtoList(items);
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
                .image(request.getImage())
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
                foundItem.setImage(request.getImage());
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
        item.setReportedCounts(item.getReportedCounts() + 1);
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
}
