package com.example.techloom.enums;

public enum CartStatus {
    ACTIVE,     // Cart is still being used / modified
    CONVERTED,  // Cart has been converted into an order (checked out)
    EXPIRED     // Cart is no longer valid
}

