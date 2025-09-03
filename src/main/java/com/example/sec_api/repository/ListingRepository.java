package com.example.sec_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.sec_api.model.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    // Optional: custom queries here
}
