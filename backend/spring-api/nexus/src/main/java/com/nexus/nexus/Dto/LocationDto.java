package com.nexus.nexus.Dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class LocationDto {

    private Double longitude;
    private Double latitude;
}
