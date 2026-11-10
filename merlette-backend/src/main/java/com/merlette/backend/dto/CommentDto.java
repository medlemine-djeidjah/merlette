package com.merlette.backend.dto;

import com.merlette.backend.entity.Comment;
import java.time.Instant;

public record CommentDto(Integer id, String content, UserSummaryDto author,
                         Instant createdAt, Instant updatedAt, boolean edited) {

    public static CommentDto from(Comment c) {
        return new CommentDto(c.getId(), c.getContent(), UserSummaryDto.from(c.getAuthor()),
                c.getCreatedAt(), c.getUpdatedAt(), c.isEdited());
    }
}
