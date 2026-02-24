package com.nexus.nexus.Service;

import com.nexus.nexus.Dto.CommentResponseDto;

import java.util.List;

public record CommentPage(
        List<CommentResponseDto> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext
) {
}
