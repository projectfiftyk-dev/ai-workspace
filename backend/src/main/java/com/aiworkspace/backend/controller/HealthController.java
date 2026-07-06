package com.aiworkspace.backend.controller;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final MongoTemplate mongoTemplate;

    public HealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of("status", "down", "reason", "mongo unreachable"));
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
