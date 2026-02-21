package com.blackrock_hackathon.self_savings_planner.controller;

import com.blackrock_hackathon.self_savings_planner.model.ReturnsRequest;
import com.blackrock_hackathon.self_savings_planner.model.ReturnsResponse;
import com.blackrock_hackathon.self_savings_planner.service.ReturnsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blackrock/challenge/v1/returns")
public class ReturnsController {

    private final ReturnsService returnsService;

    public ReturnsController(ReturnsService returnsService) {
        this.returnsService = returnsService;
    }

    @PostMapping("/nps")
    public ResponseEntity<ReturnsResponse> calculateNpsReturns(
            @RequestBody ReturnsRequest request) {
        ReturnsResponse response = returnsService.calculateNpsReturns(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/index")
    public ResponseEntity<ReturnsResponse> calculateIndexReturns(
            @RequestBody ReturnsRequest request) {
        ReturnsResponse response = returnsService.calculateIndexReturns(request);
        return ResponseEntity.ok(response);
    }
}
