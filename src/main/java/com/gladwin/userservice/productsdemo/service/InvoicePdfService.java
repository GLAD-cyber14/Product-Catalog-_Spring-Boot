package com.gladwin.userservice.productsdemo.service;

import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Model.OrderItem;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfService {

    public byte[] generateInvoice(Order order, List<OrderItem> items) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20);

            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12);

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10);

            // Company heading
            Paragraph title = new Paragraph(
                    "GLADWIN PRODUCT SUITE", titleFont);

            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph invoiceTitle = new Paragraph(
                    "INVOICE", headingFont);

            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(invoiceTitle);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Invoice Number: INV-" + order.getId(),
                    normalFont));

            if (order.getOrderDate() != null) {
                String date = order.getOrderDate().format(
                        DateTimeFormatter.ofPattern(
                                "dd-MM-yyyy HH:mm"));

                document.add(new Paragraph(
                        "Order Date: " + date, normalFont));
            }

            document.add(new Paragraph(" "));

            // Customer details
            document.add(new Paragraph(
                    "CUSTOMER DETAILS", headingFont));

            if (order.getCustomer() != null) {
                String fullName =
                        (order.getCustomer().getFirstName() == null
                                ? ""
                                : order.getCustomer().getFirstName())
                                + " "
                                + (order.getCustomer().getLastName() == null
                                ? ""
                                : order.getCustomer().getLastName());

                document.add(new Paragraph(
                        "Name: " + fullName.trim(), normalFont));

                document.add(new Paragraph(
                        "Username: "
                                + safe(order.getCustomer().getUsername()),
                        normalFont));
            }

            // Shipping address stored with this order
            document.add(new Paragraph(
                    "Shipping Address", headingFont));

            document.add(new Paragraph(
                    safe(order.getAddressLine()), normalFont));

            document.add(new Paragraph(
                    safe(order.getCity()) + ", "
                            + safe(order.getState()) + " - "
                            + safe(order.getPostalCode()),
                    normalFont));

            document.add(new Paragraph(
                    "Phone: " + safe(order.getPhoneNumber()),
                    normalFont));

            document.add(new Paragraph(" "));

            // Products table
            PdfPTable table = new PdfPTable(
                    new float[]{4, 1, 2, 2});

            table.setWidthPercentage(100);

            table.addCell(new Phrase("Product", headingFont));
            table.addCell(new Phrase("Qty", headingFont));
            table.addCell(new Phrase("Unit Price", headingFont));
            table.addCell(new Phrase("Subtotal", headingFont));

            for (OrderItem item : items) {

                String productName = "Product";

                if (item.getProduct() != null
                        && item.getProduct().getName() != null) {
                    productName = item.getProduct().getName();
                }

                double subtotal = item.getPrice() * item.getQuantity();

                table.addCell(new Phrase(productName, normalFont));

                table.addCell(new Phrase(
                        String.valueOf(item.getQuantity()), normalFont));

                table.addCell(new Phrase(
                        formatMoney(item.getPrice()), normalFont));

                table.addCell(new Phrase(
                        formatMoney(subtotal), normalFont));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Order total and payment details
            Paragraph total = new Paragraph(
                    "TOTAL: " + formatMoney(order.getTotalAmount()),
                    headingFont);

            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph(
                    "Payment Method: "
                            + safe(order.getPaymentMethod()),
                    normalFont));

            document.add(new Paragraph(
                    "Payment Status: "
                            + safe(order.getPaymentStatus()),
                    normalFont));

            document.add(new Paragraph(
                    "Order Status: "
                            + safe(order.getOrderStatus()),
                    normalFont));

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Thank you for shopping with Gladwin Product Suite!",
                    normalFont));

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            if (document.isOpen()) {
                document.close();
            }

            throw new IllegalStateException(
                    "Could not generate the invoice PDF.", e);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "Not provided"
                : value;
    }

    private String formatMoney(double amount) {
        return String.format(
                Locale.US, "Rs. %.2f", amount);
    }

}
