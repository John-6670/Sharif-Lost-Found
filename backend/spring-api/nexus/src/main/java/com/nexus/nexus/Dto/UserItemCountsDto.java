package com.nexus.nexus.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class UserItemCountsDto {

    @JsonProperty("found_reported")
    private long foundReported;

    @JsonProperty("lost_reported")
    private long lostReported;
}
