package com.uniquindio.ecommerce.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PaginaPrendasResponse(
        List<PrendaResponse> content,
        @Schema(example = "3") long totalElements,
        @Schema(example = "2") int totalPages,
        @Schema(example = "0") int page,
        @Schema(example = "2") int size) {
}
