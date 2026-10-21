package com.gladwin.userservice.productsdemo.service;

import com.gladwin.userservice.productsdemo.Model.CartItem;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;
import com.gladwin.userservice.productsdemo.Model.AppUser;

import java.util.List;

@Service
public class StripeCheckoutService {

    public String createCheckoutSession(
            List<CartItem> cartItems,
            AppUser user,
            String successUrl,
            String cancelUrl) throws StripeException {

        SessionCreateParams.Builder sessionBuilder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setClientReferenceId(user.getId().toString())
                        // Stripe replaces this placeholder after checkout.
                        .setSuccessUrl(
                                successUrl + "?session_id={CHECKOUT_SESSION_ID}"
                        )
                        .setCancelUrl(cancelUrl);

        for (CartItem item : cartItems) {

            long amountInPaise = Math.round(
                    item.getProduct().getPrice() * 100
            );

            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData
                            .builder()
                            .setName(item.getProduct().getName())
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("inr")
                            .setUnitAmount(amountInPaise)
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) item.getQuantity())
                            .setPriceData(priceData)
                            .build();

            sessionBuilder.addLineItem(lineItem);
        }

        Session session = Session.create(sessionBuilder.build());

        return session.getUrl();
    }

    public Session verifyCheckoutSession(String sessionId)
            throws StripeException {

        return Session.retrieve(sessionId);
    }
}