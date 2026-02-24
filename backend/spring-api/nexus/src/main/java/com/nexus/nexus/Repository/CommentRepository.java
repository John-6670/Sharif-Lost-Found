package com.nexus.nexus.Repository;

import com.nexus.nexus.Entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** All top-level comments for an item, ordered oldest-first. */
    List<Comment> findByItemIdAndParentIsNullOrderByCreatedAtAsc(Long itemId);

    /** All replies for a given parent comment, ordered oldest-first. */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    /** All comments for an item (any depth), ordered oldest-first — used to build the full tree. */
    List<Comment> findByItemIdOrderByCreatedAtAsc(Long itemId);
}
