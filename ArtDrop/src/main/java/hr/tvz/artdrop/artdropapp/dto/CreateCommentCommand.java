package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentCommand(
        @NotBlank
        @Size(min = 1, max = 2000)
        String text,
        Long parentCommentId
) {}
