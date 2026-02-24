package com.nexus.nexus.Mapper;

import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Dto.ReporterDto;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "reporter", target = "reporter")
    @Mapping(target = "type", expression = "java(item.getType() != null ? item.getType().name().toLowerCase() : null)")
    @Mapping(target = "status", expression = "java(item.getStatus() != null ? item.getStatus().name().toLowerCase() : null)")
    @Mapping(target = "latitude", expression = "java(item.getLatitude() != null ? item.getLatitude().toPlainString() : null)")
    @Mapping(target = "longitude", expression = "java(item.getLongitude() != null ? item.getLongitude().toPlainString() : null)")
    @Mapping(target = "image", expression = "java(withImageDataUrl(item.getImage()))")
    ProductResponseDto toDto(Item item);

    List<ProductResponseDto> toDtoList(List<Item> items);

    @Mapping(source = "fullName", target = "name")
    @Mapping(source = "registrationDate", target = "createdAt")
    @Mapping(source = "isVerified", target = "isVerified")
    ReporterDto toReporterDto(User user);

    default String withImageDataUrl(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/jpeg;base64," + base64;
    }
}
