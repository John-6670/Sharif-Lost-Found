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
    public ProductResponseDto addProduct(ProductRequestDto request) {

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

        Optional<User> user = userRepository.findByEmail(request.getUserEmail());

        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
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
    public ProductResponseDto deleteProduct(Long productId) {

        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }
        productRepository.deleteById(productId);

        return productMapper.toDto(product.get());
    }

    @Override
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto request) {

        Optional<Product> product = productRepository.findById(productId);

        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }
        Product foundProduct = product.get();

        if (request.getProductName() != null) {
            foundProduct.setProductName(request.getProductName());
        }

        if (request.getUserEmail() != null) {
            Optional<User> user = userRepository.findByEmail(request.getUserEmail());
            if (user.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            }
            foundProduct.setUser(user.get());
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
