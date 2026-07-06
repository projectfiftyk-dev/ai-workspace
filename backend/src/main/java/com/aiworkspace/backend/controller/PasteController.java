package com.aiworkspace.backend.controller;

import com.aiworkspace.backend.dto.PasteRequest;
import com.aiworkspace.backend.dto.PasteResponse;
import com.aiworkspace.backend.model.User;
import com.aiworkspace.backend.service.PasteIngestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/paste")
public class PasteController {

    private final PasteIngestService pasteIngestService;

    public PasteController(PasteIngestService pasteIngestService) {
        this.pasteIngestService = pasteIngestService;
    }

    @PostMapping
    public ResponseEntity<PasteResponse> paste(@Valid @RequestBody PasteRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
        }
        var result = pasteIngestService.ingest(user.getOrgId(), request.text());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PasteResponse(result.sourceId(), result.messageId()));
    }
}
