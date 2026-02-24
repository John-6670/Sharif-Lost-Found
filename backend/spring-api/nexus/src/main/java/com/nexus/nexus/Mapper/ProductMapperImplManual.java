package com.nexus.nexus.Mapper;

import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.ReporterDto;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Primary
public class ProductMapperImplManual implements ProductMapper {

    @Override
    public ProductResponseDto toDto(Item item) {
        if (item == null) {
            return null;
        }

        return ProductResponseDto.builder()
                .id(item.getId())
                .type(item.getType() != null ? item.getType().name().toLowerCase() : null)
                .name(item.getName())
                .description(item.getDescription())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .latitude(item.getLatitude() != null ? item.getLatitude().toPlainString() : null)
                .longitude(item.getLongitude() != null ? item.getLongitude().toPlainString() : null)
                .status(item.getStatus() != null ? item.getStatus().name().toLowerCase() : null)
                .image(withImageDataUrl(item.getImage()))
                .reporter(toReporterDto(item.getReporter()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    @Override
    public List<ProductResponseDto> toDtoList(List<Item> items) {
        if (items == null) {
            return List.of();
        }
        List<ProductResponseDto> result = new ArrayList<>(items.size());
        for (Item item : items) {
            result.add(toDto(item));
        }
        return result;
    }

    @Override
    public ReporterDto toReporterDto(User user) {
        if (user == null) {
            return null;
        }
        return ReporterDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getFullName())
                .createdAt(user.getRegistrationDate())
                .isVerified(user.getIsVerified())
                .build();
    }
}
