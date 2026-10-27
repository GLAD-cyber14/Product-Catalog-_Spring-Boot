# 🛒 Spring Boot Product & Order Management Suite

A robust, full-stack E-Commerce and Order Lifecycle Management application built with **Spring Boot**, **Thymeleaf**, **Spring Data JPA**, **Stripe API**, and **Server-Sent Events (SSE)** for real-time order tracking.

---

## ✨ Features

- **User Authentication & Profiles:** Secure authentication with Spring Security. Profile management including customizable avatars via Cloudinary integration and mandatory email for transactional billing.
- **Product & Cart System:** Dynamic product listing, interactive shopping cart, and seamless checkout pipeline.
- **Flexible Payments:** Integration with **Stripe** for digital payments and standard Cash-on-Delivery (COD) flows.
- **Real-Time Order Tracking (SSE):** Dynamic progress bar and order status timeline updated via Server-Sent Events without requiring page refreshes.
- **Automated Billing & Refunds:** Automatic generation of downloadable PDF invoices and instant email notifications for order cancellations and refunds via `JavaMailSender`.
- **Admin Dashboard:** Centralized order and user management for status updates (`PLACED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`).

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, Spring Security
- **Frontend:** HTML5, CSS3, JavaScript (ES6+), Thymeleaf
- **Database:** MySQL 
- **Third-Party Services:** Stripe API (Payment Processing), Cloudinary (Image Hosting)

---

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Apache Maven 3.8+
- MySQL 

### Configuration

Add your environment variables or update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/products_db
spring.datasource.username=root
spring.datasource.password=yourpassword

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Mail Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Third-Party API Keys
stripe.secret.key=your_stripe_secret_key
cloudinary.cloud_name=your_cloud_name
cloudinary.api_key=your_api_key
cloudinary.api_secret=your_api_secret
```

### Installation & Execution

1. Clone the repository:
   ```bash
   git clone https://github.com/GLAD-cyber14/Product-Catalog-_Spring-Boot.git
   cd Product-Catalog-_Spring-Boot
   ```
2. Build the application:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
4. Open your browser and navigate to `http://localhost:8080`.

---

## 📸 Screenshots

<table>
  <tr>
    <td align="center"><b>Order Details & Real-Time SSE</b></td>
    <td align="center"><b>Admin User Management</b></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/c53c2ee9-63ac-4dfd-af3e-9004be62ba58" width="100%" alt="Order Details"></td>
    <td><img src="https://github.com/user-attachments/assets/ccff979a-b390-4c36-a181-45ebd93cfe3a" width="100%" alt="Admin User Management"></td>
  </tr>
  <tr>
    <td align="center"><em>Track order status live with interactive step progress</em></td>
    <td align="center"><em>Manage user accounts, roles (ADMIN/USER), and permissions</em></td>
  </tr>
</table>
---

## 📝 License

This project is open-source and available under the [MIT License](LICENSE).
