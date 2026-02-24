package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.Category;
import com.nexus.nexus.Entity.MapTile;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.ProductMapper;
import com.nexus.nexus.Repository.CategoryRepository;
import com.nexus.nexus.Repository.MapTileRepository;
import com.nexus.nexus.Repository.ReportRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final MapTileRepository mapTileRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    private static final int ABUSE_REPORT_THRESHOLD = 5;

    @Override
    public List<ProductResponseDto> findAllProducts() {
        List<Item> items = reportRepository.findAllByIsRemovedFalse();
        return productMapper.toDtoList(items);
    }

    @Override
    public List<ProductResponseDto> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAllProducts();
        }
        List<Item> items = reportRepository.searchByKeyword(keyword.trim());
        return productMapper.toDtoList(items);
    }

    @Override
    public ProductResponseDto addProduct(ProductRequestDto request, JwtPrincipal principal) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        validatePrincipal(principal);

        if (request.getItemName() == null || request.getItemName().isEmpty()) {
            throw new IllegalArgumentException("Item name is required");
        }

        if (request.getReportedAt() == null) {
            throw new IllegalArgumentException("Reported at time is required");
        }

        if (request.getLocationLongitude() == null || request.getLocationLatitude() == null) {
            throw new IllegalArgumentException("Location coordinates are required");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Type of report is required");
        }

        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }

        if (request.getCategoryName() == null || request.getCategoryName().isEmpty()) {
            throw new IllegalArgumentException("Category is required");
        }

        User applicant = resolveApplicantFromPrincipal(principal);

        Category category = categoryRepository.findById(request.getCategoryName())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        MapTile location = MapTile.builder()
                .longitude(request.getLocationLongitude())
                .latitude(request.getLocationLatitude())
                .build();
        location = mapTileRepository.save(location);

        Item item = Item.builder()
                .itemName(request.getItemName())
                .description(request.getDescription())
                .type(request.getType())
                .status(request.getStatus())
                .reportedAt(request.getReportedAt())
                .category(category)
                .applicant(applicant)
                .location(location)
                .image(decodeImage(request.getImageBase64()))
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

        // Verify that the product belongs to the authenticated user
        Item foundItem = report.get();
        if (!foundItem.getApplicant().getEmail().equals(principal.email())) {
            throw new SecurityException("You are not authorized to delete this product");
        }

        // Soft-delete for consistency with the abuse-report removal mechanism
        foundItem.setIsRemoved(true);
        reportRepository.save(foundItem);

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

        if (foundItem.getIsRemoved()) {
            throw new IllegalArgumentException("Product has been removed and cannot be updated");
        }

        // Verify that the product belongs to the authenticated user
        if (!foundItem.getApplicant().getEmail().equals(principal.email())) {
            throw new SecurityException("You are not authorized to update this product");
        }

        if (request.getItemName() != null) {
            foundItem.setItemName(request.getItemName());
        }

        if (request.getDescription() != null) {
            foundItem.setDescription(request.getDescription());
        }

        if (request.getType() != null) {
            foundItem.setType(request.getType());
        }

        if (request.getStatus() != null) {
            foundItem.setStatus(request.getStatus());
        }

        if (request.getReportedAt() != null) {
            foundItem.setReportedAt(request.getReportedAt());
        }

        if (request.getCategoryName() != null) {
            Category category = categoryRepository.findById(request.getCategoryName())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            foundItem.setCategory(category);
        }

        if (request.getLocationLongitude() != null && request.getLocationLatitude() != null) {
            MapTile location = foundItem.getLocation();
            location.setLongitude(request.getLocationLongitude());
            location.setLatitude(request.getLocationLatitude());
            mapTileRepository.save(location);
        }

        if (request.getImageBase64() != null) {
            foundItem.setImage(decodeImage(request.getImageBase64()));
        }

        foundItem = reportRepository.save(foundItem);
        return productMapper.toDto(foundItem);
    }

    @Override
    @Transactional
    public void reportItem(Long itemId, JwtPrincipal principal) {
        validatePrincipal(principal);
        
        // Find the item
        Item item = reportRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        
        // Check if item is already removed
        if (item.getIsRemoved()) {
            throw new IllegalArgumentException("Item has already been removed");
        }
        
        // Increment counter
        item.setReportsCounter(item.getReportsCounter() + 1);
        
        // Check if threshold is reached
        if (item.getReportsCounter() >= ABUSE_REPORT_THRESHOLD) {
            item.setIsRemoved(true);
        }
        
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

    /**
     * Decodes a Base64 string to bytes.
     * Returns null if the input is null or blank (image is optional).
     * Throws IllegalArgumentException if the string is not valid Base64.
     */
    private byte[] decodeImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 image data");
        }
    }

    private User resolveApplicantFromPrincipal(JwtPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseGet(() -> {
                    String fullName = principal.name() != null && !principal.name().isBlank()
                            ? principal.name()
                            : principal.email().split("@")[0];

                    User newUser = User.builder()
                            .fullName(fullName)
                            .email(principal.email())
                            .password(UUID.randomUUID().toString())
                            .registrationDate(LocalDateTime.now())
                            .lastSeen(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
