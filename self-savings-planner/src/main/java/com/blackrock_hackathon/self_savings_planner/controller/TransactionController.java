package com.blackrock_hackathon.self_savings_planner.controller;

import com.blackrock_hackathon.self_savings_planner.dto.request.FilterRequest;
import com.blackrock_hackathon.self_savings_planner.dto.request.TransactionInput;
import com.blackrock_hackathon.self_savings_planner.dto.request.ValidatorRequest;
import com.blackrock_hackathon.self_savings_planner.dto.response.EnrichedTransaction;
import com.blackrock_hackathon.self_savings_planner.dto.response.ValidationResult;
import com.blackrock_hackathon.self_savings_planner.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("blackrock/challenge/v1/transactions")
@Tag(name = "Transactions", description = "Parse, validate, and filter financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/parse")
    @Operation(summary = "Parse raw transactions",
            description = "Rounds each transaction amount up to the nearest 100 and returns the ceiling and remnant.")
    public ResponseEntity<List<EnrichedTransaction>> parseTransactions(@RequestBody List<TransactionInput> transactions) {
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.parseTransactions(transactions));
    }

    @PostMapping("/validator")
    @Operation(summary = "Validate transactions against wage",
            description = "Checks for duplicates, negative amounts, ceiling/remnant accuracy, and wage cap.")
    public ResponseEntity<ValidationResult> validateTransactionWithWage(@RequestBody ValidatorRequest validatorRequest) {
        return ResponseEntity.ok(transactionService.validateTransactionWithWage(validatorRequest));
    }

    @PostMapping("/filter")
    @Operation(summary = "Validate and filter with period rules",
            description = "Validates transactions and marks whether each falls within a K evaluation period.")
    public ResponseEntity<ValidationResult> filterTransaction(@RequestBody FilterRequest filterRequest) {
        return ResponseEntity.ok(transactionService.validateTransactionWithWageAndPeriods(filterRequest));
    }
}
