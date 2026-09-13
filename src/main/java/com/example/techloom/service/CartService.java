package com.example.techloom.service;

import com.example.techloom.entity.Cart;
import com.example.techloom.entity.CartItem;
import com.example.techloom.entity.Product;
import com.example.techloom.enums.CartStatus;
import com.example.techloom.exception.ResourceNotFoundException;
import com.example.techloom.repository.CartItemRepository;
import com.example.techloom.repository.CartRepository;
import com.example.techloom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartResponse createCart() {
        Cart cart = Cart.builder().status(CartStatus.ACTIVE).build();
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long cartId) {
        return CartResponse.from(getActiveCartEntity(cartId, false));
    }

    @Transactional
    public CartResponse addItem(Long cartId, AddCartItemRequest req) {
        Cart cart = getActiveCartEntity(cartId, true);

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.getProductId()));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, product.getId())
                .orElse(null);

        if (item == null) {
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(req.getQuantity())
                    .build();
        } else {
            item.setQuantity(item.getQuantity() + req.getQuantity());
        }
        cartItemRepository.save(item);

        Cart refreshed = cartRepository.findById(cartId).orElseThrow();
        return CartResponse.from(refreshed);
    }

    @Transactional
    public CartResponse updateItem(Long cartId, Long itemId, UpdateCartItemRequest req) {
        getActiveCartEntity(cartId, true);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        if (!item.getCart().getId().equals(cartId)) {
            throw new ResourceNotFoundException("Cart item not found in cart " + cartId);
        }
        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);
        return CartResponse.from(cartRepository.findById(cartId).orElseThrow());
    }

    @Transactional
    public CartResponse removeItem(Long cartId, Long itemId) {
        getActiveCartEntity(cartId, true);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        if (!item.getCart().getId().equals(cartId)) {
            throw new ResourceNotFoundException("Cart item not found in cart " + cartId);
        }
        cartItemRepository.delete(item);
        return CartResponse.from(cartRepository.findById(cartId).orElseThrow());
    }

    @Transactional
    public CartResponse clearItems(Long cartId) {
        Cart cart = getActiveCartEntity(cartId, true);
        cart.getItems().clear();
        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    /**
     * Package-private accessor for OrderService's checkout flow — returns the
     * managed entity (not a DTO) so it can be locked/mutated within the same
     * checkout transaction.
     */
    @Transactional
    public Cart getActiveCartEntity(Long cartId, boolean requireActive) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
        if (requireActive && cart.getStatus() != CartStatus.ACTIVE) {
            throw new InvalidStateException("Cart " + cartId + " is not ACTIVE (status=" + cart.getStatus() + ")");
        }
        return cart;
    }
}

