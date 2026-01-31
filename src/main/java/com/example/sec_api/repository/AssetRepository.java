package com.example.sec_api.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.sec_api.model.Asset;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByCik(Long cik);
    
    Asset findByListings_Ticker(String ticker);

    List<Asset> findByIsFund(Boolean isFund);
}