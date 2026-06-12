package com.picknpay.ecommerce.exception;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(Long orderId, String currentStatus, String attemptedAction) {
        super("Cannot " + attemptedAction + " order " + orderId
                + " in state " + currentStatus);
    }
}
