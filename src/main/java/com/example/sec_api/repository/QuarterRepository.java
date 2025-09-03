package com.example.sec_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.sec_api.model.Quarter;
import com.example.sec_api.model.Asset;
import java.time.LocalDate;
import java.util.Optional;

public interface QuarterRepository extends JpaRepository<Quarter, Long> {
    Optional<Quarter> findByAssetAndPeriodEnd(Asset asset, LocalDate periodEnd);
}
