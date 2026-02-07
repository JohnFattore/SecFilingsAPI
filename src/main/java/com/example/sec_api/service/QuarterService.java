package com.example.sec_api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.example.sec_api.model.Asset;
import com.example.sec_api.model.Quarter;
import com.example.sec_api.repository.QuarterRepository;

@Service
public class QuarterService {

    private final QuarterRepository quarterRepository;

    // constructor
    public QuarterService(QuarterRepository quarterRepository) {
        this.quarterRepository = quarterRepository;
    }

    public Quarter saveQuarter(Quarter quarter) {
        return quarterRepository.save(quarter);
    }

    public Quarter createOrUpdateQuarter(Quarter quarter) {
        if (quarter.getPeriodStart() != null && quarter.getPeriodEnd() != null) {
            return quarterRepository
                    .findByAssetAndPeriodStartAndPeriodEnd(quarter.getAsset(), quarter.getPeriodStart(),
                            quarter.getPeriodEnd())
                    .map(existing -> {
                        updateQuarterFields(existing, quarter);
                        return quarterRepository.save(existing);
                    })
                    .orElseGet(() -> quarterRepository.save(quarter));
        }
        return quarterRepository.save(quarter);
    }

    public void updateAssetQuarters(Asset asset, List<Quarter> newQuarters) {
        // Safeguard: Merge duplicates within the incoming list first
        Map<String, Quarter> consolidatedNew = new HashMap<>();
        for (Quarter q : newQuarters) {
            if (q.getPeriodStart() == null || q.getPeriodEnd() == null)
                continue;
            String key = q.getPeriodStart() + "|" + q.getPeriodEnd();
            if (consolidatedNew.containsKey(key)) {
                updateQuarterFields(consolidatedNew.get(key), q);
            } else {
                consolidatedNew.put(key, q);
            }
        }

        List<Quarter> existingQuarters = quarterRepository.findByAsset(asset);
        Map<String, Quarter> existingMap = existingQuarters.stream()
                .filter(q -> q.getPeriodStart() != null && q.getPeriodEnd() != null)
                .collect(Collectors.toMap(q -> q.getPeriodStart() + "|" + q.getPeriodEnd(), q -> q, (a, b) -> a));

        List<Quarter> toSave = new ArrayList<>();
        for (Quarter newQ : consolidatedNew.values()) {
            Quarter existing = existingMap.get(newQ.getPeriodStart() + "|" + newQ.getPeriodEnd());

            if (existing != null) {
                updateQuarterFields(existing, newQ);
                toSave.add(existing);
            } else {
                toSave.add(newQ);
            }
        }
        quarterRepository.saveAll(toSave);
    }

    private void updateQuarterFields(Quarter existing, Quarter source) {
        if (source.getPeriodStart() != null)
            existing.setPeriodStart(source.getPeriodStart());
        if (source.getPeriodEnd() != null)
            existing.setPeriodEnd(source.getPeriodEnd());

        if (source.getNetIncomeLoss() != null)
            existing.setNetIncomeLoss(source.getNetIncomeLoss());
        if (source.getRevenue() != null)
            existing.setRevenue(source.getRevenue());
        if (source.getCostOfRevenue() != null)
            existing.setCostOfRevenue(source.getCostOfRevenue());
        if (source.getGrossProfit() != null)
            existing.setGrossProfit(source.getGrossProfit());
        if (source.getOperatingIncome() != null)
            existing.setOperatingIncome(source.getOperatingIncome());
        if (source.getAssets() != null)
            existing.setAssets(source.getAssets());
        if (source.getLiabilities() != null)
            existing.setLiabilities(source.getLiabilities());
        if (source.getEquity() != null)
            existing.setEquity(source.getEquity());
        if (source.getLongTermDebt() != null)
            existing.setLongTermDebt(source.getLongTermDebt());
        if (source.getInventory() != null)
            existing.setInventory(source.getInventory());
        if (source.getOperatingCashFlow() != null)
            existing.setOperatingCashFlow(source.getOperatingCashFlow());
        if (source.getCash() != null)
            existing.setCash(source.getCash());
        if (source.getEpsBasic() != null)
            existing.setEpsBasic(source.getEpsBasic());
    }
}