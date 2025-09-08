package com.example.sec_api.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.example.sec_api.model.Listing;
import com.example.sec_api.model.Asset;
import com.example.sec_api.repository.ListingRepository;
import com.example.sec_api.repository.AssetRepository;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final AssetRepository assetRepository;
    // constructor
    public ListingService(ListingRepository listingRepository, AssetRepository assetRepository) {
        this.listingRepository = listingRepository;
        this.assetRepository = assetRepository;
    }

    public Listing saveListing(Listing listing) {
        return listingRepository.save(listing);
    }
    
    public Listing createOrUpdateListing(Listing listing) {
        //Asset asset = assetRepository.findByCik(cik)
        //    .orElseThrow(() -> new RuntimeException("Asset not found with CIK: " + cik));
        return listingRepository.findByTicker(listing.getTicker())
            .map(existing -> {
                // update fields if needed
                return listingRepository.save(existing);
            })
            .orElseGet(() -> {
                return listingRepository.save(listing);
            });
    }
}