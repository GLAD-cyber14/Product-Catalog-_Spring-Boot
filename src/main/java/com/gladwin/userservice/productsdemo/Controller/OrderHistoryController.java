package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
    public String orderHistory(
            Authentication authentication,
            Model model) {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

        List<Order> orders =
                orderRepository.findByCustomerIdOrderByOrderDateDesc(
                        user.getId());

        model.addAttribute("orders", orders);

        return "order-history";
    }

}
