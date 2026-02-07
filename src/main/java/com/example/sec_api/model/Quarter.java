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

    private LocalDate periodStart;

    private Long netIncomeLoss;

    private Long revenue;

    private Long assets;

    private Long liabilities;

    private Long operatingCashFlow;

    private Long cash;

    private Double epsBasic;

    private Long costOfRevenue;

    private Long grossProfit;

    private Long operatingIncome;

    private Long equity;

    private Long longTermDebt;

    private Long inventory;

    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;

    // --- getters and setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public Long getNetIncomeLoss() {
        return netIncomeLoss;
    }

    public void setNetIncomeLoss(Long netIncomeLoss) {
        this.netIncomeLoss = netIncomeLoss;
    }

    public Long getRevenue() {
        return revenue;
    }

    public void setRevenue(Long revenue) {
        this.revenue = revenue;
    }

    public Long getAssets() {
        return assets;
    }

    public void setAssets(Long assets) {
        this.assets = assets;
    }

    public Long getLiabilities() {
        return liabilities;
    }

    public void setLiabilities(Long liabilities) {
        this.liabilities = liabilities;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Long getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public void setOperatingCashFlow(Long operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }

    public Long getCash() {
        return cash;
    }

    public void setCash(Long cash) {
        this.cash = cash;
    }

    public Double getEpsBasic() {
        return epsBasic;
    }

    public void setEpsBasic(Double epsBasic) {
        this.epsBasic = epsBasic;
    }

    public Long getCostOfRevenue() {
        return costOfRevenue;
    }

    public void setCostOfRevenue(Long costOfRevenue) {
        this.costOfRevenue = costOfRevenue;
    }

    public Long getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(Long grossProfit) {
        this.grossProfit = grossProfit;
    }

    public Long getOperatingIncome() {
        return operatingIncome;
    }

    public void setOperatingIncome(Long operatingIncome) {
        this.operatingIncome = operatingIncome;
    }

    public Long getEquity() {
        return equity;
    }

    public void setEquity(Long equity) {
        this.equity = equity;
    }

    public Long getLongTermDebt() {
        return longTermDebt;
    }

    public void setLongTermDebt(Long longTermDebt) {
        this.longTermDebt = longTermDebt;
    }

    public Long getInventory() {
        return inventory;
    }

    public void setInventory(Long inventory) {
        this.inventory = inventory;
    }
}