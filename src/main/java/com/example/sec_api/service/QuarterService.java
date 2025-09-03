package com.example.sec_api.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.example.sec_api.model.Quarter;
import com.example.sec_api.model.Asset;
import com.example.sec_api.repository.QuarterRepository;
import com.example.sec_api.repository.AssetRepository;
import java.time.LocalDate;

@Service
public class QuarterService {

    private final QuarterRepository quarterRepository;
    private final AssetRepository assetRepository;
    // constructor
    public QuarterService(QuarterRepository quarterRepository, AssetRepository assetRepository) {
        this.quarterRepository = quarterRepository;
        this.assetRepository = assetRepository;
    }

    public Quarter saveQuarter(Quarter quarter) {
        return quarterRepository.save(quarter);
    }
    
    public Quarter createQuarter(long cik, LocalDate periodEnd, Long netIncomeLoss) {
        Asset asset = assetRepository.findByCik(cik).orElseThrow(() -> new RuntimeException("Asset not found"));
        return quarterRepository.findByAssetAndPeriodEnd(asset, periodEnd)
        .map(quarter -> {
            // update fields if needed
            return quarterRepository.save(quarter);
        })
        .orElseGet(() -> {
            Quarter quarter = new Quarter();
            quarter.setAsset(asset);
            quarter.setPeriodEnd(periodEnd);
            quarter.setNetIncomeLoss(netIncomeLoss);
            return quarterRepository.save(quarter);
        });
    }
}