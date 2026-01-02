package com.nexus.nexus.Service.ServiceImplementation;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.Location;
import com.nexus.nexus.Entity.Product;
import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Mapper.ProductMapper;
import com.nexus.nexus.Repository.LocationRepository;
import com.nexus.nexus.Repository.ProductRepository;
import com.nexus.nexus.Repository.UserRepository;
import com.nexus.nexus.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponseDto addProduct(ProductRequestDto request, String authenticatedUserEmail) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getProductName() == null || request.getProductName().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (request.getLostOrFoundTime() == null) {
            throw new IllegalArgumentException("Lost or found time is required");
        }

        if (request.getLocationX() == null || request.getLocationY() == null) {
            throw new IllegalArgumentException("Location coordinates are required");
        }

        if (request.getTypeOfReport() == null) {
            throw new IllegalArgumentException("Type of report is required");
        }

        // Use authenticated user email instead of request email
        Optional<User> user = userRepository.findByEmail(authenticatedUserEmail);

        if (user.isEmpty()) {
            throw new IllegalArgumentException("Authenticated user not found");
        }

        Location location = Location.builder()
                .x(request.getLocationX())
                .y(request.getLocationY())
                .build();
        location = locationRepository.save(location);

        Product product = productMapper.toEntity(request, location, user.get());
        product = productRepository.save(product);

        return productMapper.toDto(product);
    }

    @Override
    public ProductResponseDto deleteProduct(Long productId, String authenticatedUserEmail) {

        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        // Verify that the product belongs to the authenticated user
        Product foundProduct = product.get();
        if (!foundProduct.getUser().getEmail().equals(authenticatedUserEmail)) {
            throw new SecurityException("You are not authorized to delete this product");
        }

        productRepository.deleteById(productId);

        return productMapper.toDto(foundProduct);
    }

    @Override
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto request, String authenticatedUserEmail) {

        Optional<Product> product = productRepository.findById(productId);

        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }
        Product foundProduct = product.get();

        // Verify that the product belongs to the authenticated user
        if (!foundProduct.getUser().getEmail().equals(authenticatedUserEmail)) {
            throw new SecurityException("You are not authorized to update this product");
        }

        if (request.getProductName() != null) {
            foundProduct.setProductName(request.getProductName());
        }

        if (request.getDescription() != null) {
            foundProduct.setDescription(request.getDescription());
        }

        if (request.getTypeOfReport() != null) {
            foundProduct.setTypeOfReport(request.getTypeOfReport());
        }

        if (request.getStatus() != null) {
            foundProduct.setStatus(request.getStatus());
        }

        if (request.getLostOrFoundTime() != null) {
            foundProduct.setLostOrFoundTime(request.getLostOrFoundTime());
        }

        if (request.getDeliveredTo() != null) {
            foundProduct.setDeliveredTo(request.getDeliveredTo());
        }

        if (request.getLocationX() != null && request.getLocationY() != null) {
            Location location = Location.builder()
                    .x(request.getLocationX())
                    .y(request.getLocationY())
                    .build();
            location = locationRepository.save(location);
            foundProduct.setLocation(location);
        }

        foundProduct = productRepository.save(foundProduct);
        return productMapper.toDto(foundProduct);
    }
}
