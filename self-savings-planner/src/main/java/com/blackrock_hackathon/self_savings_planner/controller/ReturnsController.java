package com.blackrock_hackathon.self_savings_planner.controller;

import com.blackrock_hackathon.self_savings_planner.dto.request.ReturnsRequest;
import com.blackrock_hackathon.self_savings_planner.dto.response.ReturnsResponse;
import com.blackrock_hackathon.self_savings_planner.service.ReturnsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blackrock/challenge/v1/returns")
@Tag(name = "Returns", description = "Calculate investment returns via NPS or Index fund")
public class ReturnsController {

    private final ReturnsService returnsService;

    public ReturnsController(ReturnsService returnsService) {
        this.returnsService = returnsService;
    }

    @PostMapping("/nps")
    @Operation(summary = "NPS returns",
            description = "Compounds savings at 7.11% annually with inflation adjustment and tax benefit under Section 80CCD.")
    public ResponseEntity<ReturnsResponse> calculateNpsReturns(@RequestBody ReturnsRequest request) {
        return ResponseEntity.ok(returnsService.calculateNpsReturns(request));
    }

    @PostMapping("/index")
    @Operation(summary = "Index fund returns",
            description = "Compounds savings at 14.49% (NIFTY 50) annually with inflation adjustment. No tax benefit.")
    public ResponseEntity<ReturnsResponse> calculateIndexReturns(@RequestBody ReturnsRequest request) {
        return ResponseEntity.ok(returnsService.calculateIndexReturns(request));
    }
}
