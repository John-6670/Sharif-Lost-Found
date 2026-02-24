package com.nexus.nexus.Controller;

import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Service.CommentPage;
import com.nexus.nexus.Service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController controller;

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
}
