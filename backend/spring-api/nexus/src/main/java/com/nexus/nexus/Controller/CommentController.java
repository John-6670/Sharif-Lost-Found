package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Models.ResponseModel;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.CommentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product/{itemId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * GET /api/product/{itemId}/comments
     * Public — no JWT required (criterion 1: view comments).
     * Returns all top-level comments with nested replies (criterion 4).
     * Empty list is returned with 200 OK to handle the no-result state (criterion 4).
     */
    @GetMapping
    public ResponseEntity<ResponseModel<List<CommentResponseDto>>> getComments(
            @PathVariable Long itemId) {

        List<CommentResponseDto> comments = commentService.getCommentsForItem(itemId);
        String message = comments.isEmpty() ? "No comments yet" : "Comments fetched successfully";
        return ResponseEntity.ok(ResponseModel.<List<CommentResponseDto>>builder()
                .success(true)
                .message(message)
                .data(comments)
                .build());
    }

    /**
     * POST /api/product/{itemId}/comments
     * Requires JWT (criterion 2: add comment, criterion 3: reply).
     * Body: { "text": "...", "parentCommentId": null|<id> }
     */
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseModel<CommentResponseDto>> addComment(
            @PathVariable Long itemId,
            @RequestBody CommentRequestDto request) {

        JwtPrincipal principal = getJwtPrincipal();
        CommentResponseDto created = commentService.addComment(itemId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseModel.<CommentResponseDto>builder()
                        .success(true)
                        .message("Comment added successfully")
                        .data(created)
                        .build());
    }

    private JwtPrincipal getJwtPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new SecurityException("Authentication required");
        }
        return principal;
    }
}
