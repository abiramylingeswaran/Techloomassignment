package com.example.techloom.controller;


import com.example.techloom.dto.CartResponse;
import com.example.techloom.service.CartService;
import com.example.techloom.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CartResponse> create() {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.createCart());
    }

    @GetMapping("/{id}")
    public CartResponse get(@PathVariable Long id) {
        return cartService.getCart(id);
    }

    @PostMapping("/{cartId}/items")
    public CartResponse addItem(@PathVariable Long cartId, @Valid @RequestBody AddCartItemRequest req) {
        return cartService.addItem(cartId, req);
    }

    @PutMapping("/{cartId}/items/{itemId}")
    public CartResponse updateItem(@PathVariable Long cartId, @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest req) {
        return cartService.updateItem(cartId, itemId, req);
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long cartId, @PathVariable Long itemId) {
        return cartService.removeItem(cartId, itemId);
    }

    @DeleteMapping("/{cartId}/items")
    public CartResponse clearItems(@PathVariable Long cartId) {
        return cartService.clearItems(cartId);
    }

    /**
     * Converts the cart into a RESERVED order, locking stock for 5 minutes.
     * Concurrency-safe: see OrderService.checkout for the row-locking strategy
     * that prevents overselling when multiple carts race for the same product.
     */
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long cartId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(cartId));
    }
}

