package com.gladwin.userservice.productsdemo.service;

import com.gladwin.userservice.productsdemo.Model.Order;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;

import org.springframework.stereotype.Service;

@Service
public class StripeRefundService {

    public Refund refundOrder(Order order) throws StripeException {

        // Reuse an existing refund if its ID is already saved.
        if (order.getStripeRefundId() != null
                && !order.getStripeRefundId().isBlank()) {

            return Refund.retrieve(order.getStripeRefundId());
        }

        if (order.getStripeSessionId() == null
                || order.getStripeSessionId().isBlank()) {

            throw new IllegalStateException(
                    "Stripe Checkout Session ID is missing.");
        }

        // Retrieve the original Checkout Session.
        Session session = Session.retrieve(order.getStripeSessionId());

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException(
                    "Stripe payment has not been completed.");
        }

        String paymentIntentId = session.getPaymentIntent();

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalStateException(
                    "Stripe PaymentIntent was not found.");
        }

        // Use the same idempotency key when retrying this order's refund.
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("order-refund-" + order.getId())
                .build();

        RefundCreateParams refundParams =
                RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .build();

        Refund refund = Refund.create(refundParams, options);

        // Keep the refund ID available for the cancellation service.
        if (refund.getId() != null) {
            order.setStripeRefundId(refund.getId());
        }

        return refund;
    }

}
