package com.example.techloom.repository;

import com.example.techloom.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Takes a PostgreSQL row-level lock (SELECT ... FOR UPDATE) on the product row.
     * This is the crux of the concurrency guarantee: two concurrent checkouts for
     * the same product will serialize here — the second transaction blocks until
     * the first commits or rolls back, so stock is always read-then-decremented
     * atomically and overselling is impossible.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
