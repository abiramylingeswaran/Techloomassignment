package com.example.techloom.dto;

import com.pos.system.entity.CartItem;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    public static CartItemResponse from(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}

