package com.gladwin.userservice.productsdemo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRefundConfirmation(
            String recipientEmail,
            Long orderId,
            double refundAmount,
            boolean refundCompleted) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);

        if (refundCompleted) {
            message.setSubject("Refund Confirmation - Order #" + orderId);

            message.setText(
                    "Hello,\n\n" +
                            "Your refund for Order #" + orderId +
                            " has been successfully processed by Stripe.\n\n" +
                            "Refund amount: INR " +
                            String.format("%.2f", refundAmount) +
                            "\n\nThe time taken for the funds to appear in your account " +
                            "depends on your bank or payment provider.\n\n" +
                            "Thank you for shopping with Gladwin Product Suite."
            );
        } else {
            message.setSubject("Refund Request Submitted - Order #" + orderId);

            message.setText(
                    "Hello,\n\n" +
                            "Your refund request for Order #" + orderId +
                            " has been submitted to Stripe.\n\n" +
                            "Refund amount: INR " +
                            String.format("%.2f", refundAmount) +
                            "\n\nThe refund is still being processed. " +
                            "The time taken for the funds to appear in your account " +
                            "depends on your bank or payment provider.\n\n" +
                            "Thank you for shopping with Gladwin Product Suite."
            );
        }

        mailSender.send(message);
    }


}

