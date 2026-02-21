package com.blackrock_hackathon.self_savings_planner.controller;

import com.blackrock_hackathon.self_savings_planner.dto.response.PerformanceReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/blackrock/challenge/v1/performance")
@Tag(name = "Performance", description = "System execution metrics")
public class PerformanceController {

    @GetMapping
    @Operation(summary = "Performance report",
            description = "Returns JVM uptime, memory usage (%), and active thread count.")
    public ResponseEntity<PerformanceReport> getPerformanceReport() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        LocalDateTime time = LocalDateTime.ofEpochSecond(
                uptimeMs / 1000, (int) ((uptimeMs % 1000) * 1_000_000), ZoneOffset.UTC);

        Runtime runtime = Runtime.getRuntime();
        double usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
        double totalMb = runtime.totalMemory() / (1024.0 * 1024.0);
        String memoryPercent = String.format("%.2f", (usedMb / totalMb) * 100);

        int threads = Thread.activeCount();

        return ResponseEntity.ok(new PerformanceReport(time, memoryPercent, threads));
    }
}
