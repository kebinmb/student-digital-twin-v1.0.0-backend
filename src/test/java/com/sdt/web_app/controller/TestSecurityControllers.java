package com.sdt.web_app.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@TestConfiguration
public class TestSecurityControllers {

    @RestController
    @RequestMapping("/api")
    static class DummyOrderController {
        @GetMapping("/orders")
        public ResponseEntity<Map<String, String>> getOrders() {
            return ResponseEntity.ok(Map.of("status", "orders retrieved"));
        }
    }

    @RestController
    @RequestMapping("/api/admin")
    static class DummyAdminController {
        @GetMapping("/dashboard")
        public ResponseEntity<Map<String, String>> getDashboard() {
            return ResponseEntity.ok(Map.of("status", "admin dashboard"));
        }
    }
}
