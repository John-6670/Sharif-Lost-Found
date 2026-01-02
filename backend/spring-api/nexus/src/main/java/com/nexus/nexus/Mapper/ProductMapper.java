package com.nexus.nexus.Mapper;

import com.nexus.nexus.Dto.ProductRequestDto;
import com.nexus.nexus.Dto.ProductResponseDto;
import com.nexus.nexus.Entity.Location;
import com.nexus.nexus.Entity.Product;
import com.nexus.nexus.Entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "location.x", target = "locationX")
    @Mapping(source = "location.y", target = "locationY")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "user.name", target = "userName")
    ProductResponseDto toDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "user", target = "user")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "requestDto.productName", target = "productName")
    @Mapping(source = "requestDto.description", target = "description")
    @Mapping(source = "requestDto.typeOfReport", target = "typeOfReport")
    @Mapping(source = "requestDto.status", target = "status")
    @Mapping(source = "requestDto.lostOrFoundTime", target = "lostOrFoundTime")
    @Mapping(source = "requestDto.deliveredTo", target = "deliveredTo")
    Product toEntity(ProductRequestDto requestDto, Location location, User user);
}
