package com.example.techloom.enums;

public enum ReservationStatus {
    ACTIVE,     // Stock is currently locked for this order
    CONFIRMED,  // Payment succeeded; reservation is now permanent (stock sold)
    RELEASED,   // Stock returned (failure or manual cancellation)
    EXPIRED     // 5-minute window elapsed before checkout completed
}