package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ArtworkCommand(
        @NotNull
        @NotBlank
        String title,
        @NotBlank
        String medium,
        @Size(max = 2000)
        String description,
        @Valid
        @NotNull
        @Size(min = 1, message = "at least one image is required")
        List<ArtworkImageCommand> images,
        BigDecimal width,
        BigDecimal height,
        BigDecimal depth,
        @Pattern(regexp = "CM|MM|IN", message = "dimensionUnit must be CM, MM or IN")
        String dimensionUnit,
        @PositiveOrZero
        Integer catalogSequence
) {
}
