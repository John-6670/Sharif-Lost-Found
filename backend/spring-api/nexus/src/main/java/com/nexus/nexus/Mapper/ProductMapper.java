package com.nexus.nexus.Mapper;

import com.nexus.nexus.Dto.ApplicantDto;
import com.nexus.nexus.Dto.LocationDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.MapTile;
import com.nexus.nexus.Entity.Item;
import com.nexus.nexus.Entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "applicant", target = "applicant")
    @Mapping(source = "resolver", target = "resolver")
    @Mapping(source = "location", target = "location")
    ProductResponseDto toDto(Item item);

    List<ProductResponseDto> toDtoList(List<Item> items);

    ApplicantDto toApplicantDto(User user);

    LocationDto toLocationDto(MapTile mapTile);
}
