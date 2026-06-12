package com.picknpay.ecommerce.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for product '" + sku + "': requested "
                + requested + ", available " + available);
    }
}
