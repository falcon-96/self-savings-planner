package com.blackrock_hackathon.self_savings_planner.service;

import com.blackrock_hackathon.self_savings_planner.dto.common.TemporalData;
import com.blackrock_hackathon.self_savings_planner.dto.period.K;
import com.blackrock_hackathon.self_savings_planner.dto.period.P;
import com.blackrock_hackathon.self_savings_planner.dto.period.Q;
import com.blackrock_hackathon.self_savings_planner.dto.request.ReturnsRequest;
import com.blackrock_hackathon.self_savings_planner.dto.request.TransactionInput;
import com.blackrock_hackathon.self_savings_planner.dto.response.ReturnsResponse;
import com.blackrock_hackathon.self_savings_planner.dto.response.Saving;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates projected investment returns for NPS and Index fund strategies.
 *
 * <p>Processing order per the challenge spec:
 * <ol>
 *   <li>Compute ceiling and base remnant for each transaction</li>
 *   <li>Apply Q-period rules (fixed-amount override)</li>
 *   <li>Apply P-period rules (extra-amount addition)</li>
 *   <li>Group by K-periods and sum remnants</li>
 *   <li>Compound interest → inflation adjustment → tax benefit (NPS only)</li>
 * </ol>
 */
@Service
public class ReturnsService {

    private static final BigDecimal NPS_RATE = new BigDecimal("0.0711");
    private static final BigDecimal INDEX_RATE = new BigDecimal("0.1449");
    private static final int RETIREMENT_AGE = 60;
    private static final int MIN_INVESTMENT_YEARS = 5;
    private static final BigDecimal MAX_NPS_DEDUCTION = new BigDecimal("200000");
    private static final BigDecimal NPS_INCOME_PERCENT = new BigDecimal("0.10");
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public ReturnsResponse calculateNpsReturns(ReturnsRequest request) {
        return calculateReturns(request, NPS_RATE, true);
    }

    public ReturnsResponse calculateIndexReturns(ReturnsRequest request) {
        return calculateReturns(request, INDEX_RATE, false);
    }

    private ReturnsResponse calculateReturns(ReturnsRequest request, BigDecimal rate, boolean isNps) {
        int years = request.age() < RETIREMENT_AGE
                ? RETIREMENT_AGE - request.age()
                : MIN_INVESTMENT_YEARS;

        BigDecimal inflation = BigDecimal.valueOf(request.inflation()).divide(HUNDRED, MC);
        BigDecimal annualIncome = BigDecimal.valueOf(request.wage()).multiply(BigDecimal.valueOf(12));

        List<TxRemnant> enriched = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCeiling = BigDecimal.ZERO;

        for (TransactionInput tx : request.transactions()) {
            if (tx.amount() == null || tx.amount() < 0) continue;

            BigDecimal amount = BigDecimal.valueOf(tx.amount());
            BigDecimal ceiling = amount.divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED);
            BigDecimal remnant = ceiling.subtract(amount);

            totalAmount = totalAmount.add(amount);
            totalCeiling = totalCeiling.add(ceiling);

            remnant = applyQ(tx.date(), remnant, request.q());
            remnant = applyP(tx.date(), remnant, request.p());

            enriched.add(new TxRemnant(tx.date(), remnant));
        }

        List<Saving> savings = new ArrayList<>();
        for (K k : request.k()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (TxRemnant tr : enriched) {
                if (within(tr.date, k.temporalData())) {
                    sum = sum.add(tr.remnant);
                }
            }

            BigDecimal futureValue = sum.multiply(BigDecimal.ONE.add(rate).pow(years, MC), MC);
            BigDecimal realValue = futureValue.divide(BigDecimal.ONE.add(inflation).pow(years, MC), MC);
            BigDecimal profit = realValue.subtract(sum);
            BigDecimal taxBenefit = isNps ? taxBenefit(sum, annualIncome) : BigDecimal.ZERO;

            savings.add(new Saving(k.temporalData(), round2(sum), round2(profit), round2(taxBenefit)));
        }

        return new ReturnsResponse(round2(totalAmount), round2(totalCeiling), savings);
    }

    /** If multiple Q periods match, the one with the latest start wins. Ties: first in list. */
    private BigDecimal applyQ(LocalDateTime date, BigDecimal remnant, List<Q> qs) {
        if (qs == null) return remnant;
        Q best = null;
        for (Q q : qs) {
            if (within(date, q.temporalData())) {
                if (best == null || q.temporalData().start().isAfter(best.temporalData().start())) {
                    best = q;
                }
            }
        }
        return best != null ? BigDecimal.valueOf(best.fixed()) : remnant;
    }

    /** All matching P extras are summed and added to the remnant. */
    private BigDecimal applyP(LocalDateTime date, BigDecimal remnant, List<P> ps) {
        if (ps == null) return remnant;
        for (P p : ps) {
            if (within(date, p.temporalData())) {
                remnant = remnant.add(BigDecimal.valueOf(p.extra()));
            }
        }
        return remnant;
    }

    private boolean within(LocalDateTime date, TemporalData period) {
        return !date.isBefore(period.start()) && !date.isAfter(period.end());
    }

    /**
     * Simplified Indian tax slabs:
     * 0–7L → 0%, 7–10L → 10%, 10–12L → 15%, 12–15L → 20%, 15L+ → 30%.
     */
    private BigDecimal tax(BigDecimal income) {
        BigDecimal[] limits = {bd("700000"), bd("1000000"), bd("1200000"), bd("1500000")};
        BigDecimal[] rates = {bd("0.10"), bd("0.15"), bd("0.20"), bd("0.30")};

        if (income.compareTo(limits[0]) <= 0) return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < limits.length; i++) {
            BigDecimal lower = limits[i];
            BigDecimal upper = i + 1 < limits.length ? limits[i + 1] : income;
            if (income.compareTo(lower) <= 0) break;
            BigDecimal taxable = income.min(upper).subtract(lower).max(BigDecimal.ZERO);
            total = total.add(taxable.multiply(rates[i]));
        }
        return total;
    }

    /** NPS_Deduction = min(invested, 10% of annual_income, ₹2L). Benefit = Tax(income) − Tax(income − deduction). */
    private BigDecimal taxBenefit(BigDecimal invested, BigDecimal annualIncome) {
        BigDecimal deduction = invested
                .min(annualIncome.multiply(NPS_INCOME_PERCENT))
                .min(MAX_NPS_DEDUCTION);
        return tax(annualIncome).subtract(tax(annualIncome.subtract(deduction))).max(BigDecimal.ZERO);
    }

    private double round2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private record TxRemnant(LocalDateTime date, BigDecimal remnant) {}
}
