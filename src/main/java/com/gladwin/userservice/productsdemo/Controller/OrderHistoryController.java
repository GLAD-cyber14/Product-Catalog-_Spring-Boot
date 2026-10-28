package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;

import com.gladwin.userservice.productsdemo.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderHistoryController {



    private final OrderRepository orderRepository;
    private final AppUserRepository appUserRepository;

    public OrderHistoryController(
            OrderRepository orderRepository,
            AppUserRepository appUserRepository) {

        this.orderRepository = orderRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public String viewOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recent10") String filter,
            Model model,
            Principal principal) {

        // Retrieve logged-in user
        AppUser user = appUserRepository.findByUsername(principal.getName());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = null;

        // Determine date filter
        if ("week".equals(filter)) {
            startDate = now.minusWeeks(1);
        } else if ("month".equals(filter)) {
            startDate = now.minusMonths(1);
        } else if ("year".equals(filter)) {
            startDate = now.minusYears(1);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage;

        if (startDate != null) {
            orderPage = orderRepository.findByCustomerIdAndOrderDateAfterOrderByOrderDateDesc(user.getId(), startDate, pageable);
        } else {
            orderPage = orderRepository.findByCustomerIdOrderByOrderDateDesc(user.getId(), pageable);
        }

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("currentFilter", filter);

        return "order-history";
    }

}
