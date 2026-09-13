package com.example.techloom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.techloom.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
