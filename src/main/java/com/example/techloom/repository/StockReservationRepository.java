package com.example.techloom.repository;

import com.example.techloom.entity.StockReservation;
import com.example.techloom.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    List<StockReservation> findByOrderId(Long orderId);

    /** Scanned periodically by the expiry job — indexed on (status, expires_at). */
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime cutoff);

    Optional<StockReservation> findFirstByOrderIdAndStatus(Long orderId, ReservationStatus status);
}