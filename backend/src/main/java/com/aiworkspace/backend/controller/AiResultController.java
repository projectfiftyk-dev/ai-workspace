package com.aiworkspace.backend.controller;

import com.aiworkspace.backend.model.AiResult;
import com.aiworkspace.backend.model.Task;
import com.aiworkspace.backend.model.User;
import com.aiworkspace.backend.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai-results")
public class AiResultController {

    private final AnalysisService analysisService;

    public AiResultController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiResult> get(@PathVariable String id, Authentication authentication) {
        User user = requireUser(authentication);
        return ResponseEntity.ok(analysisService.getAiResult(user.getOrgId(), id));
    }

    @PostMapping("/{id}/action-items/{index}/confirm")
    public ResponseEntity<Task> confirmActionItem(@PathVariable String id, @PathVariable int index,
                                                   Authentication authentication) {
        User user = requireUser(authentication);
        Task task = analysisService.confirmActionItem(user.getOrgId(), id, index);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
        }
        return user;
    }
}
