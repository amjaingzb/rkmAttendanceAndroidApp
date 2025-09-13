package com.rkm.attendance.core;

// NEW: A custom exception for clear error handling.
public class OverlapException extends Exception {
    public OverlapException(String message) {
        super(message);
    }
}
