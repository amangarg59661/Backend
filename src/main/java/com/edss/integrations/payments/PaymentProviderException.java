package com.edss.integrations.payments;

/** Wraps SDK-specific exceptions so callers do not depend on Stripe / Razorpay types. */
public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
