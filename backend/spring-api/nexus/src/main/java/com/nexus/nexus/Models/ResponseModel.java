package com.nexus.nexus.Models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResponseModel<T> {

    private Boolean success;
    private String message;
    private T data;
}
