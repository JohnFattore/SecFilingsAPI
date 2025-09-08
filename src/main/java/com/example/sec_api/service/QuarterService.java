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
    
    public Quarter createOrUpdateQuarter(Quarter quarter) {
        return quarterRepository.findByAssetAndPeriodEnd(quarter.getAsset(), quarter.getPeriodEnd())
        .map(existing -> {
            existing.setNetIncomeLoss(quarter.getNetIncomeLoss());
            return quarterRepository.save(existing);
        })
        .orElseGet(() -> {
            return quarterRepository.save(quarter);
        });
    }
}