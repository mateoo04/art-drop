package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ArtworkUpdateCommand(
        String title,
        String medium,
        @Size(max = 2000)
        String description,
        @Valid
        List<ArtworkImageCommand> images,
        BigDecimal width,
        BigDecimal height,
        BigDecimal depth,
        @Pattern(regexp = "CM|MM|IN|PX", message = "dimensionUnit must be CM, MM, IN or PX")
        String dimensionUnit,
        BigDecimal price,
        @Pattern(regexp = "ORIGINAL|EDITION|AVAILABLE|SOLD", message = "saleStatus must be ORIGINAL, EDITION, AVAILABLE or SOLD")
        String saleStatus,
        Boolean unlist
) {
}
