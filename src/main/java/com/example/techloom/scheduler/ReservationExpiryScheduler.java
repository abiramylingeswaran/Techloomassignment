package com.example.techloom.scheduler;


import com.example.techloom.entity.Order;
import com.example.techloom.entity.Product;
import com.example.techloom.entity.StockReservation;
import com.example.techloom.enums.OrderStatus;
import com.example.techloom.enums.ReservationStatus;
import com.example.techloom.repository.OrderRepository;
import com.example.techloom.repository.ProductRepository;
import com.example.techloom.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Background job that reclaims stock from checkouts that were never
 * completed within the 5-minute reservation window.
 *
 * Runs on a short poll interval (app.reservation.expiry-scan-rate-ms) rather
 * than scheduling one timer per reservation — simpler to reason about and
 * scales fine since each pass is a single indexed query.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryScheduler {

    private final StockReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Scheduled(fixedRateString = "${app.reservation.expiry-scan-rate-ms:15000}")
    public void expireStaleReservations() {
        List<StockReservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, LocalDateTime.now());

        // Group by order: an order can hold several line-item reservations,
        // and all of them must be released together in the same transaction
        // as the order's status flip — otherwise the first reservation
        // processed would flip the order to EXPIRED and the sweep would skip
        // its siblings on the (mistaken) belief the order already moved on.
        Set<Long> orderIds = expired.stream()
                .map(r -> r.getOrder().getId())
                .collect(Collectors.toSet());

        for (Long orderId : orderIds) {
            try {
                expireOrder(orderId);
            } catch (Exception ex) {
                // One bad order should never stop the sweep of the rest.
                log.warn("Failed to expire reservations for order {}: {}", orderId, ex.getMessage());
            }
        }
    }

    /**
     * Expires every still-ACTIVE reservation belonging to one order in a
     * single transaction, locking the order row first. This guarantees that
     * if a payment is committing for the same order at the same instant, the
     * two operations serialize instead of racing: whichever commits first
     * decides the order's fate, and the loser sees a non-RESERVED order and
     * does nothing.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.RESERVED) {
            return; // Order already moved on (paid, cancelled, etc) since the sweep started.
        }

        List<StockReservation> active = reservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        if (active.isEmpty()) {
            return;
        }

        for (StockReservation reservation : active) {
            Product locked = productRepository.findByIdForUpdate(reservation.getProduct().getId()).orElse(null);
            if (locked != null) {
                locked.setAvailableStock(locked.getAvailableStock() + reservation.getQuantity());
                productRepository.save(locked);
            }

            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setReleasedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
        }

        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);

        log.info("Expired {} reservation(s) for order {} — stock released", active.size(), orderId);
    }
}
