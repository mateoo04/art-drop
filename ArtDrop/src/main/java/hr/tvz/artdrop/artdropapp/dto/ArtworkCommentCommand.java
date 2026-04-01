package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ArtworkCommentCommand(
        @NotNull
        @NotBlank
        String title,
        @NotBlank
        @Size(max = 80)
        String author,
        @NotBlank
        @Size(max = 1000)
        String content,
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "contactEmail must be valid")
        String contactEmail,
        @PositiveOrZero
        Integer priority
) {
}
