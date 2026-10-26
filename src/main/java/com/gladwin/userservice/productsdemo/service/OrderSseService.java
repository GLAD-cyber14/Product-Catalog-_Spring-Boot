 package com.gladwin.userservice.productsdemo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderSseService {

    // Store SSE connections separately for each customer.
    private final Map<Long, List<SseEmitter>> connections =
            new ConcurrentHashMap<>();

    // Subscribe a customer to order status updates.
    public SseEmitter subscribe(Long customerId) {

        SseEmitter emitter = new SseEmitter(0L);

        connections.computeIfAbsent(
                customerId,
                id -> new CopyOnWriteArrayList<>()
        ).add(emitter);

        emitter.onCompletion(() -> remove(customerId, emitter));
        emitter.onTimeout(() -> remove(customerId, emitter));
        emitter.onError(error -> remove(customerId, emitter));

        return emitter;
    }

    // Notify a customer when their order status changes.
    public void notifyOrderStatus(
            Long customerId,
            Long orderId,
            String status) {

        List<SseEmitter> emitters = connections.get(customerId);

        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("order-status")
                                .data(Map.of(
                                        "orderId", orderId,
                                        "status", status
                                ))
                );
            } catch (IOException | IllegalStateException ex) {
                remove(customerId, emitter);
            }
        }
    }

    // Remove disconnected browser connections.
    private void remove(Long customerId, SseEmitter emitter) {

        List<SseEmitter> emitters = connections.get(customerId);

        if (emitters != null) {
            emitters.remove(emitter);

            if (emitters.isEmpty()) {
                connections.remove(customerId, emitters);
            }
        }
    }
}

