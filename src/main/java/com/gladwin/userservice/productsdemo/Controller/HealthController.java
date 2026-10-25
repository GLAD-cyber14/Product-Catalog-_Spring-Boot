package com.gladwin.userservice.productsdemo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // 1. Tells Spring Boot this class handles web URLs

        @GetMapping("/health") // 2. Listens specifically for requests sent to "/health"
        public ResponseEntity<String> health() {

            // 3. Instantly returns a "200 OK" status along with the text "OK"
            return ResponseEntity.ok("OK");
        }
}


