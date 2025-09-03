package com.example.sec_api.model;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "quarters")
public class Quarter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column()
    private LocalDate periodEnd;

    @Column()
    private Long netIncomeLoss;

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

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
}
