package com.example.sec_api.model;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "quarters")
public class Quarter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate periodEnd;

    private Long netIncomeLoss;

    private Long revenue;

    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;    

    // --- getters and setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public Long getNetIncomeLoss() { return netIncomeLoss; }
    public void setNetIncomeLoss(Long netIncomeLoss) { this.netIncomeLoss = netIncomeLoss; }

    public Long getRevenue() { return revenue; }
    public void setRevenue(Long revenue) { this.revenue = revenue; }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
}