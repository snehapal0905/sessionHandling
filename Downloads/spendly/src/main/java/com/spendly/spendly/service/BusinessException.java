package com.spendly.spendly.service;

/** A rule violation the user can fix, e.g. deleting an account that still has transactions. Shown as a flash message. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
