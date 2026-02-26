package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Dto.CommentRequestDto;
import com.nexus.nexus.Dto.ReportCommentRequestDto;
import com.nexus.nexus.Models.ResponseModel;
import com.nexus.nexus.Security.JwtPrincipal;
import com.nexus.nexus.Service.CommentPage;
import com.nexus.nexus.Service.CommentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getComments_returnsPagedResponse() {
        CommentPage page = new CommentPage(
                List.of(CommentResponseDto.builder().id(1L).build()),
                0,
                20,
                1,
                1,
                false
        );
        when(commentService.getCommentsForItem(1L, 0, 20)).thenReturn(page);

        ResponseEntity<com.nexus.nexus.Models.ResponseModel<CommentPage>> response =
                controller.getComments(1L, 0, 20);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().items()).hasSize(1);
        assertThat(response.getBody().getData().items().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void addComment_returnsCreatedResponse() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        when(commentService.addComment(eq(1L), any(), eq(principal)))
                .thenReturn(CommentResponseDto.builder().id(1L).build());

        ResponseEntity<ResponseModel<CommentResponseDto>> response =
                controller.addComment(1L, new CommentRequestDto("hi", null));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData().getId()).isEqualTo(1L);
    }

    @Test
    void reportComment_returnsOkResponse() {
        JwtPrincipal principal = new JwtPrincipal(1L, "user@example.com", "User", true, "jti");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        ResponseEntity<ResponseModel<Void>> response =
                controller.reportComment(1L, 2L, new ReportCommentRequestDto("spam"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
