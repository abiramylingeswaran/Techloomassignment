package com.example.techloom.exception;

public class InsufficentStockException extends RuntimeException {
    public InsufficentStockException(String message) {
        super(message);
    }
}
