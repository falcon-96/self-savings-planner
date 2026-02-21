package com.blackrock_hackathon.self_savings_planner.controller;

import com.blackrock_hackathon.self_savings_planner.model.*;
import com.blackrock_hackathon.self_savings_planner.model.validation.TransactionValidationResult;
import com.blackrock_hackathon.self_savings_planner.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("blackrock/challenge/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/parse")
    public ResponseEntity<List<EnrichedTransaction>> parseTransactions(@RequestBody List<TransactionCandidate> transactions) {
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.parseTransactions(transactions));
    }

    @PostMapping("/validator")
    public ResponseEntity<TransactionValidationResult> validateTransactionWithWage(@RequestBody IncomeStatement incomeStatement) {
        return ResponseEntity.ok(transactionService.validateTransactionWithWage(incomeStatement));
    }

    @PostMapping("/filter")
    public ResponseEntity<TransactionValidationResult> filterTransaction(@RequestBody IncomeStatementWithSpecialPeriodData incomeStatementWithSpecialPeriodData) {
        return ResponseEntity.ok(transactionService.validateTransactionWithWageAndPeriods(incomeStatementWithSpecialPeriodData));
    }

}
