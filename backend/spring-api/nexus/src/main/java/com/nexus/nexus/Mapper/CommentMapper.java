package com.nexus.nexus.Mapper;

import com.nexus.nexus.Dto.ApplicantDto;
import com.nexus.nexus.Dto.CommentResponseDto;
import com.nexus.nexus.Entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "author.email", target = "author.email")
    @Mapping(source = "author.fullName", target = "author.fullName")
    @Mapping(source = "parent.id", target = "parentCommentId")
    @Mapping(target = "replies", ignore = true)
    CommentResponseDto toDto(Comment comment);

    List<CommentResponseDto> toDtoList(List<Comment> comments);

    @Mapping(source = "email", target = "email")
    @Mapping(source = "fullName", target = "fullName")
    ApplicantDto toApplicantDto(com.nexus.nexus.Entity.User user);
}
