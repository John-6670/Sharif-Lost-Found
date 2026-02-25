package com.nexus.nexus.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ItemCountsDto {

    @JsonProperty("today_reported")
    private long todayReported;

    @JsonProperty("all_reported")
    private long allReported;

    @JsonProperty("returned")
    private long returned;
}
