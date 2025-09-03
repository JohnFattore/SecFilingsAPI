package com.example.sec_api.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.example.sec_api.model.Asset;
import com.example.sec_api.repository.AssetRepository;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    // constructor
    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Asset saveAsset(Asset asset) {
        return assetRepository.save(asset);
    }
        
    public Asset createOrUpdateAsset(Long cik) {
        return assetRepository.findByCik(cik)
            .map(asset -> {
                // update fields if needed
                return assetRepository.save(asset);
            })
            .orElseGet(() -> {
                Asset asset = new Asset();
                asset.setCik(cik);
                return assetRepository.save(asset);
            });
    }
}