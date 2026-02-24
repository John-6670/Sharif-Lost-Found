package com.nexus.nexus.Mapper;

import com.nexus.nexus.Dto.ApplicantDto;
import com.nexus.nexus.Dto.LocationDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.MapTile;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Base64;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "applicant", target = "applicant")
    @Mapping(source = "resolver", target = "resolver")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "image", target = "imageBase64", qualifiedByName = "bytesToBase64")
    ProductResponseDto toDto(Item item);

    List<ProductResponseDto> toDtoList(List<Item> items);

    ApplicantDto toApplicantDto(User user);

    LocationDto toLocationDto(MapTile mapTile);

    @Named("bytesToBase64")
    static String bytesToBase64(byte[] image) {
        return image != null ? Base64.getEncoder().encodeToString(image) : null;
    }

    @Named("base64ToBytes")
    static byte[] base64ToBytes(String base64) {
        return base64 != null && !base64.isBlank() ? Base64.getDecoder().decode(base64) : null;
    }
}
