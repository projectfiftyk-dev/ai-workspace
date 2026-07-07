package com.aiworkspace.backend.controller;

import com.aiworkspace.backend.model.AiResult;
import com.aiworkspace.backend.model.User;
import com.aiworkspace.backend.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sources")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/{sourceId}/analyze")
    public ResponseEntity<AiResult> analyze(@PathVariable String sourceId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
        }
        AiResult aiResult = analysisService.analyzeSource(user.getOrgId(), sourceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(aiResult);
    }
}
