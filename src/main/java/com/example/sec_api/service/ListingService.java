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
    
    public Listing createListing(String ticker, String title, Long cik) {
        Asset asset = assetRepository.findByCik(cik)
            .orElseThrow(() -> new RuntimeException("Asset not found with CIK: " + cik));
        Listing listing = new Listing();
        listing.setTicker(ticker);
        listing.setTitle(title);
        listing.setAsset(asset);
        return listingRepository.save(listing);
    }
}