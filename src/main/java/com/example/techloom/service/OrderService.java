package com.example.techloom.service;

import com.example.techloom.repository.CartRepository;
import com.example.techloom.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final StockReservationRepository reservationRepository;

    @Value("${app.reservation.ttl-minutes:5}")
    private int reservationTtlMinutes;

    /**
     * Checkout: converts an ACTIVE cart into a RESERVED order, atomically
     * locking and decrementing stock for every line item.
     *
     * Concurrency strategy: for every distinct product involved we take a
     * PESSIMISTIC_WRITE row lock (SELECT ... FOR UPDATE), always acquired in
     * ascending product-id order to prevent deadlocks between two carts that
     * share products. Whichever transaction gets the lock first reads the
     * true available stock, decrements it, and commits; a second concurrent
     * checkout for the same product then sees the updated (possibly
     * insufficient) stock and fails cleanly instead of overselling.
     *
     * The whole operation is one transaction: if any single item has
     * insufficient stock, everything rolls back — no partial reservations.
     */
    @Transactional
    public OrderResponse checkout(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));

        if (cart.getStatus() != CartStatus.ACTIVE) {
            // Cart already converted (or expired) — this is the duplicate-checkout guard.
            throw new DuplicateSubmissionException(
                    "Cart " + cartId + " has already been checked out or is no longer active (status=" + cart.getStatus() + ")");
        }

        if (cart.getItems().isEmpty()) {
            throw new InvalidStateException("Cannot checkout an empty cart");
        }

        // Lock products in a fixed order (ascending id) to avoid deadlocks
        // when two carts overlap on the same set of products.
        List<CartItem> orderedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(ci -> ci.getProduct().getId()))
                .collect(Collectors.toList());

        Order order = Order.builder()
                .cart(cart)
                .status(OrderStatus.PENDING)
                .build();
        order = orderRepository.save(order);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(reservationTtlMinutes);
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : orderedItems) {
            Product locked = productRepository.findByIdForUpdate(ci.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + ci.getProduct().getId()));

            int requestedQty = ci.getQuantity();
            if (locked.getAvailableStock() < requestedQty) {
                // Throwing here rolls back the whole transaction, releasing
                // every lock taken so far and undoing any prior decrements
                // in this checkout.
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + locked.getName() + "': requested " + requestedQty
                                + ", available " + locked.getAvailableStock());
            }

            locked.setAvailableStock(locked.getAvailableStock() - requestedQty);
            productRepository.save(locked);

            BigDecimal unitPrice = locked.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            total = total.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(locked)
                    .quantity(requestedQty)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            order.getItems().add(orderItem);

            StockReservation reservation = StockReservation.builder()
                    .order(order)
                    .product(locked)
                    .quantity(requestedQty)
                    .status(ReservationStatus.ACTIVE)
                    .reservedAt(now)
                    .expiresAt(expiresAt)
                    .build();
            reservationRepository.save(reservation);
        }

        order.setStatus(OrderStatus.RESERVED);
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        cart.setStatus(CartStatus.CONVERTED);
        cartRepository.save(cart);

        OrderResponse response = OrderResponse.from(order);
        response.setReservationExpiresAt(expiresAt);
        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(OrderResponse::from).collect(Collectors.toList());
    }

    /**
     * Cancels an order, restoring any stock still attributable to it.
     * Allowed from RESERVED (checkout in progress) or PAID (post-purchase
     * cancellation). Anything already FAILED/EXPIRED/CANCELLED has no stock
     * left to restore and is rejected.
     */
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED && order.getStatus() != OrderStatus.PAID) {
            throw new InvalidStateException(
                    "Order " + orderId + " cannot be cancelled from status " + order.getStatus());
        }

        List<StockReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (StockReservation r : reservations) {
            if (r.getStatus() == ReservationStatus.ACTIVE || r.getStatus() == ReservationStatus.CONFIRMED) {
                Product locked = productRepository.findByIdForUpdate(r.getProduct().getId()).orElseThrow();
                locked.setAvailableStock(locked.getAvailableStock() + r.getQuantity());
                productRepository.save(locked);

                r.setStatus(ReservationStatus.RELEASED);
                r.setReleasedAt(LocalDateTime.now());
                reservationRepository.save(r);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return OrderResponse.from(order);
    }

    private Order getOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}

