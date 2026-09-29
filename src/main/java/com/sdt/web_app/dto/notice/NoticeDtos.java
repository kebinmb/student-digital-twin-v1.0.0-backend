package com.sdt.web_app.dto.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class NoticeDtos {

    public record NoticeDto(
            String id,
            String title,
            String category,
            String date,
            boolean unread,
            String content,
            String audience,
            String priority,
            String authorName,
            boolean isPinned,
            Instant publishAt
    ) {}

    public record CreateNoticeRequest(
            @NotBlank(message = "Title is required")
            @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
            String title,

            @NotBlank(message = "Category is required")
            String category,

            @NotBlank(message = "Content is required")
            @Size(min = 10, message = "Content must be at least 10 characters")
            String content,

            String audience,
            String priority
    ) {}

    public record UpdateNoticeRequest(
            @NotBlank(message = "Title is required")
            @Size(min = 5, max = 255, message = "Title must be between 5 and 255 characters")
            String title,

            @NotBlank(message = "Category is required")
            String category,

            @NotBlank(message = "Content is required")
            String content,

            String audience,
            String priority,
            boolean isPinned,
            boolean isPublished
    ) {}
}
