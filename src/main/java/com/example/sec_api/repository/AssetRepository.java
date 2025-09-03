package com.example.sec_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.sec_api.model.Asset;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByCik(Long cik);
}
