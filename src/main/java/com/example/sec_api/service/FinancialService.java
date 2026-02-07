package com.example.sec_api.service;

import com.example.sec_api.model.Quarter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FinancialService {

    public Map<String, Object> calculateMetrics(List<Quarter> quarters) {
        if (quarters == null || quarters.isEmpty()) {
            return Collections.emptyMap();
        }

        // Sort by period end descending
        quarters.sort(Comparator.comparing(Quarter::getPeriodEnd).reversed());

        Map<String, Object> metrics = new HashMap<>();

        // Latest TTM
        double currentTTMNetIncome = calculateTTMNetIncome(quarters, 0);
        double currentTTMRevenue = calculateTTMRevenue(quarters, 0);
        double currentTTMOCF = calculateTTMOperatingCashFlow(quarters, 0);
        double currentTTMCostOfRevenue = calculateTTMCostOfRevenue(quarters, 0);
        double currentTTMGrossProfit = calculateTTMGrossProfit(quarters, 0);
        double currentTTMOperatingIncome = calculateTTMOperatingIncome(quarters, 0);

        // Previous TTM (shifted by 4 quarters)
        double previousTTMNetIncome = calculateTTMNetIncome(quarters, 4);
        double previousTTMRevenue = calculateTTMRevenue(quarters, 4);

        metrics.put("ttmNetIncome", currentTTMNetIncome);
        metrics.put("ttmRevenue", currentTTMRevenue);
        metrics.put("ttmOperatingCashFlow", currentTTMOCF);
        metrics.put("ttmCostOfRevenue", currentTTMCostOfRevenue);
        metrics.put("ttmGrossProfit", currentTTMGrossProfit);
        metrics.put("ttmOperatingIncome", currentTTMOperatingIncome);

        metrics.put("ttmNetIncomeYoY", calculateYoY(currentTTMNetIncome, previousTTMNetIncome));
        metrics.put("ttmRevenueYoY", calculateYoY(currentTTMRevenue, previousTTMRevenue));

        // Latest point-in-time metrics
        Quarter latest = quarters.get(0);
        metrics.put("latestAssets", latest.getAssets());
        metrics.put("latestLiabilities", latest.getLiabilities());
        metrics.put("latestEquity", latest.getEquity());
        metrics.put("latestLongTermDebt", latest.getLongTermDebt());
        metrics.put("latestInventory", latest.getInventory());
        metrics.put("latestCash", latest.getCash());
        metrics.put("latestEps", latest.getEpsBasic());

        // Ratios
        if (currentTTMRevenue != 0) {
            metrics.put("netMargin", currentTTMNetIncome / currentTTMRevenue);
            metrics.put("grossMargin", currentTTMGrossProfit / currentTTMRevenue);
        }

        if (latest.getAssets() != null && latest.getAssets() != 0) {
            metrics.put("debtToAssets", (latest.getLiabilities() != null ? latest.getLiabilities().doubleValue() : 0.0)
                    / latest.getAssets());
            metrics.put("roA", currentTTMNetIncome / latest.getAssets());
        }

        if (latest.getLiabilities() != null && latest.getLiabilities() != 0) {
            metrics.put("cashToLiabilities",
                    (latest.getCash() != null ? latest.getCash().doubleValue() : 0.0) / latest.getLiabilities());
        }

        if (currentTTMNetIncome != 0) {
            metrics.put("ocfToNetIncome", currentTTMOCF / currentTTMNetIncome);
        }

        return metrics;
    }

    // Refined TTM calculation
    public double calculateTTMNetIncome(List<Quarter> quarters, int offset) {
        if (quarters.size() < offset + 4)
            return 0.0;
        return quarters.subList(offset, offset + 4).stream()
                .mapToLong(q -> q.getNetIncomeLoss() != null ? q.getNetIncomeLoss() : 0L)
                .sum();
    }

    public double calculateTTMRevenue(List<Quarter> quarters, int offset) {
        if (quarters.size() < offset + 4)
            return 0.0;
        return quarters.subList(offset, offset + 4).stream()
                .mapToLong(q -> q.getRevenue() != null ? q.getRevenue() : 0L)
                .sum();
    }

    public double calculateTTMOperatingCashFlow(List<Quarter> quarters, int offset) {
        if (quarters.size() < offset + 4)
            return 0.0;
        return quarters.subList(offset, offset + 4).stream()
                .mapToLong(q -> q.getOperatingCashFlow() != null ? q.getOperatingCashFlow() : 0L)
                .sum();
    }

    public double calculateTTMCostOfRevenue(List<Quarter> quarters, int offset) {
        if (quarters.size() < offset + 4)
            return 0.0;
        return quarters.subList(offset, offset + 4).stream()
                .mapToLong(q -> q.getCostOfRevenue() != null ? q.getCostOfRevenue() : 0L)
                .sum();
    }

    public double calculateTTMGrossProfit(List<Quarter> quarters, int offset) {
        if (quarters.size() < offset + 4)
            return 0.0;
        return quarters.subList(offset, offset + 4).stream()
                .mapToLong(q -> q.getGrossProfit() != null ? q.getGrossProfit() : 0L)
                .sum();
    }

    public double calculateTTMOperatingIncome(List<Quarter> quarters, int offset) {
        if (quarters.size() < offset + 4)
            return 0.0;
        return quarters.subList(offset, offset + 4).stream()
                .mapToLong(q -> q.getOperatingIncome() != null ? q.getOperatingIncome() : 0L)
                .sum();
    }

    private Double calculateYoY(double current, double previous) {
        if (previous == 0)
            return null;
        return (current - previous) / Math.abs(previous);
    }
}
