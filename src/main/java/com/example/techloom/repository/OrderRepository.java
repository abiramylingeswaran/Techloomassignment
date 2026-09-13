package com.example.techloom.repository;

import com.example.techloom.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByCartId(Long cartId);

    /**
     * Row-lock the order. Used before any state transition (payment, cancel,
     * expiry) so that a payment success and a scheduler-driven expiry racing
     * on the same order serialize instead of both "winning".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}

