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
import static com.example.sec_api.util.QuarterUtils.*;

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
        Integer counter = 10;
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
    public String test() {
        Asset asset = assetRepository.findByListings_Ticker("AAPL");
        return asset.getCik().toString();
    }


    @GetMapping("/admin/quarters")
    public String financials() throws Exception {
        List<Asset> assets = assetRepository.findByIsFund(false);
        for (Asset asset : assets) {
            Long cik = asset.getCik();
            String json = webService.fetchFinancials(cik);
            JsonNode root = mapper.readTree(json);
            JsonNode facts = root.get("facts").get("us-gaap").get("NetIncomeLoss").get("units").get("USD");
            Map<LocalDate, Long> netIncomeQuarter = new HashMap<>();
            Map<Integer, Long> netIncomeYear = new HashMap<>();
            // get a dict of dates: netIncome
            for (JsonNode q : facts) {
                if (q.get("frame") != null) {
                    if (q.get("frame").asText().length() == 6) {
                        netIncomeYear.put(Integer.parseInt(q.get("frame").asText().substring(2, 6)), q.get("val").asLong());
                    }
                    else if (q.get("frame") != null) {
                        netIncomeQuarter.put(parseQuarter(q.get("frame").asText()), q.get("val").asLong());
                    }
                }
            }

            // check to ensure netIncomeQuarter is not empty
            if (!netIncomeQuarter.isEmpty()) {
                // fill in missing quarters, mainly those with 10-K not 10-Q
                LocalDate period = Collections.min(netIncomeQuarter.keySet());
                LocalDate lastPeriod = LocalDate.of(2025, 9, 30);
                while (period.isBefore(lastPeriod)) {
                    if (netIncomeQuarter.get(period) == null) {
                        Long quarterlyNetIncome = netIncomeYear.get(period.getYear());
                        if (quarterlyNetIncome != null) {
                            List<LocalDate> last3Quarters = getLast3Quarters(period);
                            for (LocalDate q : last3Quarters) {
                                if (netIncomeQuarter.get(q) == null) {
                                    break;
                                }
                                quarterlyNetIncome -= netIncomeQuarter.get(q);
                            }
                            netIncomeQuarter.put(period, quarterlyNetIncome);
                        }
                    }
                    period = nextQuarter(period);
                }
                
                for (Map.Entry<LocalDate, Long> q : netIncomeQuarter.entrySet()) {
                    Quarter quarter = new Quarter();
                    quarter.setAsset(asset);
                    quarter.setPeriodEnd(q.getKey());
                    quarter.setNetIncomeLoss(q.getValue());
                    quarterService.createOrUpdateQuarter(quarter);
                }
            }


        }
        return "Success";
    }   
}
