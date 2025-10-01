package com.example.sec_api.controller;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sec_api.service.WebService;
import com.example.sec_api.model.Asset;
import com.example.sec_api.repository.AssetRepository;
import com.example.sec_api.service.AssetService;
import com.example.sec_api.model.Listing;
import com.example.sec_api.repository.ListingRepository;
import com.example.sec_api.service.ListingService;
import com.example.sec_api.model.Quarter;
import com.example.sec_api.repository.QuarterRepository;
import com.example.sec_api.service.QuarterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import static com.example.sec_api.util.QuarterUtils.*;
import java.util.stream.Collectors;

@RestController
public class HelloController {

    private final WebService webService;
    private final AssetService assetService;
    private final AssetRepository assetRepository;
    private final ListingService listingService;
    private final ListingRepository listingRepository;
    private final QuarterService quarterService;
    private final QuarterRepository quarterRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public HelloController(WebService webService, AssetService assetService, AssetRepository assetRepository, ListingService listingService, ListingRepository listingRepository, QuarterService quarterService, QuarterRepository quarterRepository) {
        this.webService = webService;
        this.assetService = assetService;
        this.assetRepository = assetRepository;
        this.listingService = listingService;
        this.listingRepository = listingRepository;
        this.quarterService = quarterService;
        this.quarterRepository = quarterRepository;
    }

    @GetMapping("/admin/load")
    public String load() {
        // create dictionary mapping tickers to fund type (equity or fund)
        // nasdaq list first, then others
        Map<String, Boolean> tickerToType = new HashMap();
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

        Map<Integer, Map<String, String>> secTickers = webService.fetchSecTickers();
        //assetRepository.deleteAll();
        for (Map<String, String> secTicker : secTickers.values()) {
            Asset asset = new Asset();
            String ticker = secTicker.get("ticker");
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
        return tickerToType.toString();
    }

    @GetMapping("/admin/test")
    public String test() throws Exception{
        String json = null;
        Long cik = 320193L;
        json = webService.fetchFinancials(cik);
        JsonNode root = mapper.readTree(json);
        JsonNode facts = null;
        facts = root.get("facts").get("us-gaap").get("RevenueFromContractWithCustomerExcludingAssessedTax").get("units").get("USD");
        return facts.toString();
    }

    @GetMapping("/quarters")
    public ResponseEntity quarters(@RequestParam String ticker) {
        Asset asset = assetRepository.findByListings_Ticker(ticker);
        List<Quarter> quarters = quarterRepository.findByAsset(asset);
        System.out.println(quarters);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> quarterOutput = new ArrayList<>();
        for (Quarter q: quarters) {
            Map<String, Object> quarterMap = new HashMap<>();
            quarterMap.put("periodEnd", q.getPeriodEnd());
            quarterMap.put("netIncome", q.getNetIncomeLoss());
            quarterOutput.add(quarterMap);
        }
        Map<String, Object> response = Map.of(
            "ticker", ticker,
            "cik", asset.getCik().toString(),
            "quarters", quarterOutput
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/company-fact-sheet")
    public ResponseEntity companyFactSheet(@RequestParam String ticker) {
        Asset asset = assetRepository.findByListings_Ticker(ticker);
        if (asset == null) {
            return ResponseEntity.notFound().build();
        }
        List<Quarter> quarters = quarterRepository.findByAsset(asset);
        quarters.sort(Comparator.comparing(Quarter::getPeriodEnd).reversed());
        double ttmNetIncome = 0.0;
        int counter = 0;
        for (Quarter q: quarters) {
            if (counter >= 4) {
                break;
            }
            ttmNetIncome += q.getNetIncomeLoss();
            counter += 1;
        }

        Map<String, Object> response = Map.of(
            "ticker", ticker,
            "cik", asset.getCik().toString(),
            "ttmNetIncome", String.format("%.2f",ttmNetIncome)
        );
        return ResponseEntity.ok(response);
    }

    private Map<LocalDate, Map<String, Long>> getQuarterlyFacts(String[] facts, JsonNode root) {
        Map<LocalDate, Map<String, Long>> quarterData = new HashMap<>();
        for (String fact : facts) {
            JsonNode factsNode = null;
            factsNode = root.get("facts").get("us-gaap").get(fact).get("units").get("USD");
            Map<LocalDate, Long> quarters = new HashMap<>();
            Map<Integer, Long> years = new HashMap<>();

            // get a dict of dates: netIncome
            for (JsonNode q : factsNode) {
                if (q.get("frame") != null) {
                    if (q.get("frame").asText().length() == 6) {
                        years.put(Integer.parseInt(q.get("frame").asText().substring(2, 6)), q.get("val").asLong());
                    }
                    else if (q.get("frame") != null) {
                        quarters.put(parseQuarter(q.get("frame").asText()), q.get("val").asLong());
                    }
                }
            }

            // check to ensure quarters is not empty
            if (!quarters.isEmpty()) {
                // fill in missing quarters, mainly those with 10-K not 10-Q
                LocalDate period = Collections.min(quarters.keySet());
                LocalDate lastPeriod = LocalDate.of(2025, 9, 30);
                while (period.isBefore(lastPeriod)) {
                    if (quarters.get(period) == null) {
                        Long quarterFact = years.get(period.getYear());
                        if (quarterFact != null) {
                            List<LocalDate> last3Quarters = getLast3Quarters(period);
                            for (LocalDate q : last3Quarters) {
                                if (quarters.get(q) == null) {
                                    break;
                                }
                                quarterFact -= quarters.get(q);
                            }
                            quarters.put(period, quarterFact);
                        }
                    }
                    period = nextQuarter(period);
                }
                for (Map.Entry<LocalDate, Long> q : quarters.entrySet()) {
                    LocalDate quarterEnd = q.getKey();
                    if (quarterData.get(quarterEnd) == null) {
                        quarterData.put(quarterEnd, new HashMap<>());
                    }
                    Map<String, Long> currentQuarter = quarterData.get(quarterEnd);
                    currentQuarter.put(fact, q.getValue());
                }
            }
        }
        return quarterData;
    }

    @GetMapping("/admin/quarters")
    public String financials() throws Exception {
        List<Asset> assets = assetRepository.findByIsFund(false)    
        .stream()
        .filter(asset -> 789019L == asset.getCik())
        .collect(Collectors.toList());
        List<String> errors = new ArrayList<String>();
        for (Asset asset : assets) {
            Long cik = asset.getCik();
            try {
                String json = null;
                json = webService.fetchFinancials(cik);
                JsonNode root = mapper.readTree(json);

                String[] accountingConcepts = {"NetIncomeLoss", "RevenueFromContractWithCustomerExcludingAssessedTax"};
                Map<LocalDate, Map<String, Long>> quarterData = getQuarterlyFacts(accountingConcepts, root);

                for (Map.Entry<LocalDate, Map<String, Long>> q : quarterData.entrySet()) {
                    Map<String, Long> facts = q.getValue();
                    Quarter quarter = new Quarter();
                    quarter.setAsset(asset);
                    quarter.setPeriodEnd(q.getKey());
                    quarter.setNetIncomeLoss(facts.get("NetIncomeLoss"));
                    quarter.setRevenue(facts.get("RevenueFromContractWithCustomerExcludingAssessedTax"));
                    quarterService.createOrUpdateQuarter(quarter);
                }
            }
            catch (Exception e) {
                errors.add("cik:" + cik.toString() + " error: " + e.getMessage());
            }
        }
        return "Maxwell";//String.join("\n", errors);
    }   
}