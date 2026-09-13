package com.example.techloom.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddCartItemRequest {
    @NotNull
    private Long productId;

    @NotNull @Min(1)
    private Integer quantity;
}