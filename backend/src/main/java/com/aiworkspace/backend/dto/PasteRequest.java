package com.aiworkspace.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record PasteRequest(@NotBlank String text) {
}
