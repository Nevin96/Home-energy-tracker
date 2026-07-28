package com.nev.insight_service.controller;

import com.nev.insight_service.dto.InsightDto;
import com.nev.insight_service.service.InsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/insight")
public class InsightController {

    private final InsightService insightService;
    public InsightController(InsightService insightService){
        this.insightService = insightService;
    }

    @GetMapping("/saving-tips/{userId}")
    public ResponseEntity<InsightDto> getSavingsTips(@PathVariable Long userId){
        final InsightDto insight = insightService.getSavingsTips(userId);
        return ResponseEntity.ok(insight);
    }

    @GetMapping("/overview/{userId}")
    public ResponseEntity<InsightDto> getOverview(@PathVariable Long userId){
        final InsightDto insight = insightService.getOverview(userId);
        return ResponseEntity.ok(insight);
    }
}
