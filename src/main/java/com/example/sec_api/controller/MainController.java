package com.example.sec_api.controller;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sec_api.service.WebService;
import com.example.sec_api.model.Asset;
import com.example.sec_api.repository.AssetRepository;
import com.example.sec_api.service.AssetService;
import com.example.sec_api.model.Listing;
import com.example.sec_api.service.ListingService;
import com.example.sec_api.model.Quarter;
import com.example.sec_api.repository.QuarterRepository;
import com.example.sec_api.service.EdgarService;
import com.example.sec_api.service.FinancialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;

@RestController
public class MainController {

    private final WebService webService;
    private final AssetService assetService;
    private final AssetRepository assetRepository;
    private final ListingService listingService;
    private final QuarterRepository quarterRepository;
    private final EdgarService edgarService;
    private final FinancialService financialService;
    private final ObjectMapper mapper = new ObjectMapper();

    public MainController(WebService webService, AssetService assetService, AssetRepository assetRepository,
            ListingService listingService,
            QuarterRepository quarterRepository,
            EdgarService edgarService, FinancialService financialService) {
        this.webService = webService;
        this.assetService = assetService;
        this.assetRepository = assetRepository;
        this.listingService = listingService;
        this.quarterRepository = quarterRepository;
        this.edgarService = edgarService;
        this.financialService = financialService;
    }

    @GetMapping("/admin/load")
    public String load() {
        // create dictionary mapping tickers to fund type (equity or fund)
        // nasdaq list first, then others
        Map<String, Boolean> tickerToType = new HashMap<>();
        String csv = webService.fetchNasdaqData("https://www.nasdaqtrader.com/dynamic/symdir/nasdaqlisted.txt");
        String[] rows = csv.split("\\r?\\n");
        for (String row : rows) {
            String[] columns = row.split("\\|");
            if (columns.length > 6) {
                Boolean isFund = "Y".equals(columns[6]);
                tickerToType.put(columns[0], isFund);
            } else {
                // skip or handle unexpected row format
                System.out.println("Skipping malformed row: " + row);
            }
        }

        csv = webService.fetchNasdaqData("https://www.nasdaqtrader.com/dynamic/symdir/otherlisted.txt");
        rows = csv.split("\\r?\\n");
        for (String row : rows) {
            String[] columns = row.split("\\|");
            if (columns.length > 6) {
                Boolean isFund = "Y".equals(columns[4]);
                tickerToType.put(columns[0], isFund);
            } else {
                // skip or handle unexpected row format
                System.out.println("Skipping malformed row: " + row);
            }
        }

        List<String> snp500Tickers = webService.getSnP500List();
        Set<String> snp500Set = new HashSet<>(snp500Tickers);

        Map<Integer, Map<String, String>> secTickers = webService.fetchSecTickers();
        // assetRepository.deleteAll();
        for (Map<String, String> secTicker : secTickers.values()) {
            String ticker = secTicker.get("ticker");
            if (!snp500Set.contains(ticker)) {
                continue;
            }
            Asset asset = new Asset();
            asset.setCik(Long.parseLong(secTicker.get("cik_str")));
            Boolean isFund = tickerToType.get(ticker);
            if (isFund == null) {
                isFund = false;
            }
            asset.setIsFund(isFund);
            asset = assetService.createOrUpdateAsset(asset);
            Listing listing = new Listing();
            listing.setTicker(ticker);
            listing.setTitle(secTicker.get("title"));
            listing.setAsset(asset);
            listingService.createOrUpdateListing(listing);
        }
        return "S&P 500 Loaded: " + snp500Set.size() + " tickers processed.";
    }

    @GetMapping("/admin/test")
    public String test() throws Exception {
        String json = null;
        Long cik = 320193L;
        json = webService.fetchFinancials(cik);
        JsonNode root = mapper.readTree(json);
        JsonNode facts = null;
        facts = root.get("facts").get("us-gaap").get("RevenueFromContractWithCustomerExcludingAssessedTax").get("units")
                .get("USD");
        return facts.toString();
    }

    @GetMapping("/quarters")
    public ResponseEntity quarters(@RequestParam String ticker) {
        Asset asset = assetRepository.findByListings_Ticker(ticker);
        List<Quarter> quarters = quarterRepository.findByAsset(asset);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> quarterOutput = new ArrayList<>();
        for (Quarter q : quarters) {
            Map<String, Object> quarterMap = new HashMap<>();
            quarterMap.put("periodEnd", q.getPeriodEnd());
            quarterMap.put("netIncome", q.getNetIncomeLoss());
            quarterOutput.add(quarterMap);
        }
        Map<String, Object> response = Map.of(
                "ticker", ticker,
                "cik", asset.getCik().toString(),
                "quarters", quarterOutput);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test2")
    public ResponseEntity test2() throws Exception {
        webService.getSnP500List();
        return ResponseEntity.ok("MAXWELL");
    }

    @GetMapping("/company-fact-sheet")
    public ResponseEntity companyFactSheet(@RequestParam String ticker) {
        Asset asset = assetRepository.findByListings_Ticker(ticker);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        List<Quarter> quarters = quarterRepository.findByAsset(asset);
        Map<String, Object> metrics = financialService.calculateMetrics(quarters);

        Map<String, Object> response = new HashMap<>();
        response.put("ticker", ticker);
        response.put("cik", asset.getCik().toString());

        if (!metrics.isEmpty()) {
            response.put("ttmNetIncome", formatNumber(metrics.get("ttmNetIncome")));
            response.put("ttmRevenue", formatNumber(metrics.get("ttmRevenue")));
            response.put("ttmOperatingCashFlow", formatNumber(metrics.get("ttmOperatingCashFlow")));
            response.put("ttmOperatingIncome", formatNumber(metrics.get("ttmOperatingIncome")));
            response.put("ttmGrossProfit", formatNumber(metrics.get("ttmGrossProfit")));
            response.put("ttmCostOfRevenue", formatNumber(metrics.get("ttmCostOfRevenue")));

            response.put("ttmNetIncomeYoY", formatPercent(metrics.get("ttmNetIncomeYoY")));
            response.put("ttmRevenueYoY", formatPercent(metrics.get("ttmRevenueYoY")));

            // Balance Sheet
            response.put("latestAssets", formatNumber(metrics.get("latestAssets")));
            response.put("latestLiabilities", formatNumber(metrics.get("latestLiabilities")));
            response.put("latestEquity", formatNumber(metrics.get("latestEquity")));
            response.put("latestLongTermDebt", formatNumber(metrics.get("latestLongTermDebt")));
            response.put("latestInventory", formatNumber(metrics.get("latestInventory")));
            response.put("latestCash", formatNumber(metrics.get("latestCash")));
            response.put("latestShares", formatNumber(metrics.get("latestShares")));
            response.put("latestEps", formatNumber(metrics.get("latestEps")));

            // Ratios
            response.put("netMargin", formatPercent(metrics.get("netMargin")));
            response.put("grossMargin", formatPercent(metrics.get("grossMargin")));
            response.put("debtToAssets", formatPercent(metrics.get("debtToAssets")));
            response.put("cashToLiabilities", formatPercent(metrics.get("cashToLiabilities")));
            response.put("roA", formatPercent(metrics.get("roA")));
            response.put("ocfToNetIncome", formatNumber(metrics.get("ocfToNetIncome")));

            if (!quarters.isEmpty()) {
                Quarter latest = quarters.get(0);
                response.put("latestQuarterEnd", latest.getPeriodEnd().toString());
            }
        }

        return ResponseEntity.ok(response);
    }

    private String formatNumber(Object val) {
        if (val == null)
            return "0.00";
        if (val instanceof Number) {
            return String.format("%.2f", ((Number) val).doubleValue());
        }
        return val.toString();
    }

    private String formatPercent(Object val) {
        if (val == null)
            return "N/A";
        return String.format("%.2f%%", (Double) val * 100);
    }

    @GetMapping("/admin/quarters")
    public String financials() {
        List<String> snp500Tickers = webService.getSnP500List();
        List<Asset> assets = assetRepository.findByTickersIn(snp500Tickers);
        List<String> errors = Collections.synchronizedList(new ArrayList<String>());

        assets.parallelStream().forEach(asset -> {
            try {
                edgarService.updateFinancials(asset);
            } catch (Exception e) {
                errors.add("cik:" + asset.getCik() + " error: " + e.getMessage());
            }
        });
        return errors.isEmpty() ? "Success" : String.join("\n", errors);
    }

}
