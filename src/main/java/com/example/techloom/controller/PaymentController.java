package com.example.techloom.controller;

import com.pos.system.dto.PaymentRequest;
import com.pos.system.dto.PaymentResponse;
import com.pos.system.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.pay(req));
    }

    @GetMapping("/{id}")
    public PaymentResponse getOne(@PathVariable Long id) {
        return paymentService.findById(id);
    }
}

