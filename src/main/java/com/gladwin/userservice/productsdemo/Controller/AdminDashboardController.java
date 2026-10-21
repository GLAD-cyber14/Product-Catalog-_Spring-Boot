package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;

import com.gladwin.userservice.productsdemo.Model.Product;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Comparator;


@Controller
public class AdminDashboardController {


    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;

    public AdminDashboardController(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            AppUserRepository appUserRepository) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(
            Authentication authentication,
            Model model) {

        // Allow only administrators
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a ->
                                a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Retrieve orders
        List<Order> allOrders = orderRepository.findAll();

        // Total orders
        model.addAttribute("totalOrders", allOrders.size());

        // Total products and users
        model.addAttribute(
                "totalProducts", productRepository.count());

        model.addAttribute(
                "totalUsers", appUserRepository.count());

        // Calculate revenue from paid orders only.
        // COD orders with pending payment are not counted.
        double totalRevenue = allOrders.stream()
                .filter(order -> order.getPaymentStatus() != null &&
                        order.getPaymentStatus().equalsIgnoreCase("PAID"))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        model.addAttribute("totalRevenue", totalRevenue);

        // Get the 5 most recent orders
        List<Order> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(
                        Order::getOrderDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();

        model.addAttribute("recentOrders", recentOrders);

        // Sales trajectory for the last 6 months
        YearMonth currentMonth = YearMonth.now();

        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();
        Map<YearMonth, Double> revenueByMonth = new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);

            String monthName = month.getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + month.getYear();

            monthlyRevenue.put(monthName, 0.0);
            revenueByMonth.put(month, 0.0);
        }

// Add revenue from paid orders
        for (Order order : allOrders) {

            if (order.getOrderDate() == null ||
                    order.getPaymentStatus() == null ||
                    !order.getPaymentStatus().equalsIgnoreCase("PAID")) {
                continue;
            }

            // Assumes orderDate is LocalDateTime
            LocalDateTime orderDate = order.getOrderDate();

            YearMonth orderMonth = YearMonth.from(orderDate);

            if (revenueByMonth.containsKey(orderMonth)) {
                revenueByMonth.put(
                        orderMonth,
                        revenueByMonth.get(orderMonth)
                                + order.getTotalAmount()
                );
            }
        }

        // Prepare graph labels and values
        List<String> salesLabels = new ArrayList<>();
        List<Double> salesData = new ArrayList<>();

        for (Map.Entry<YearMonth, Double> entry : revenueByMonth.entrySet()) {

            YearMonth month = entry.getKey();

            String monthName = month.getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + month.getYear();

            salesLabels.add(monthName);
            salesData.add(entry.getValue());
        }

        model.addAttribute("salesLabels", salesLabels);
        model.addAttribute("salesData", salesData);

        // Low-stock products (5 or fewer units)
        List<Product> lowStockProducts = productRepository.findAll()
                .stream()
                .filter(product -> product.getStock() <= 5)
                .sorted(Comparator.comparingInt(Product::getStock))
                .toList();

// Count out-of-stock products
        long outOfStockCount = lowStockProducts.stream()
                .filter(product -> product.getStock() == 0)
                .count();

// Add stock information to dashboard
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("lowStockCount",
                lowStockProducts.size() - outOfStockCount);
        return "admin-dashboard";
    }



}
