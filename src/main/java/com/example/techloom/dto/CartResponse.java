package com.example.techloom.dto;

import com.example.techloom.entity.Cart;
import com.example.techloom.enums.CartStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    private Long id;
    private CartStatus status;
    private List<CartItemResponse> items;
    private BigDecimal estimatedTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .items(items)
                .estimatedTotal(total)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}

