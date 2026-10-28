package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.service.OrderSseService;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/orders")
public class OrderSseController {

    private final OrderSseService orderSseService;
    private final AppUserRepository appUserRepository;

    public OrderSseController(
            OrderSseService orderSseService,
            AppUserRepository appUserRepository) {
        this.orderSseService = orderSseService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        Long userId = null;

        // Safely check if an authenticated user is present
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            AppUser user = appUserRepository.findByUsername(authentication.getName());
            if (user != null) {
                userId = user.getId();
            }
        }

        // Pass userId (or handle general broadcasts if userId is null) to your SSE service
        return orderSseService.subscribe(userId);
    }
}