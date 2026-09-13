package com.example.techloom.controller;

import com.pos.system.dto.ReservationResponse;
import com.pos.system.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Reservation creation/release/expiry are internal side effects of
     * checkout and payment — this read-only endpoint is exposed mainly for
     * inspection/debugging.
     */
    @GetMapping("/{id}")
    public ReservationResponse getOne(@PathVariable Long id) {
        return reservationService.findById(id);
    }
}
