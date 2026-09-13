package com.example.techloom.controller;

import com.example.techloom.dto.*;
import com.example.techloom.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse getOne(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
