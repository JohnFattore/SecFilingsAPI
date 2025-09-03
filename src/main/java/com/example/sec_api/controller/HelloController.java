package com.example.sec_api.controller;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.sec_api.service.WebService;
import com.example.sec_api.service.AssetService;
import com.example.sec_api.repository.AssetRepository;
import com.example.sec_api.service.ListingService;
import com.example.sec_api.repository.ListingRepository;
import com.example.sec_api.service.QuarterService;
import com.example.sec_api.repository.QuarterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;

@RestController
public class HelloController {

    private final WebService webService;
    private final AssetService assetService;
    private final AssetRepository assetRepository;
    private final ListingService listingService;
    private final ListingRepository listingRepository;
    private final QuarterService quarterService;
    private final QuarterRepository quarterRepository;

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
        Map<Integer, Map<String, String>> assets = webService.fetchSecTickers();
        //assetRepository.deleteAll();
        for (Map<String, String> asset : assets.values()) {
            assetService.createOrUpdateAsset(Long.parseLong(asset.get("cik_str")));
            listingService.createListing(asset.get("ticker"), asset.get("title"), Long.parseLong(asset.get("cik_str")));
        }
        return "Success";
    }

    private static List<LocalDate> getLast3Quarters(LocalDate period) {
        List<LocalDate> last3Quarters = new ArrayList<>();
        int year = period.getYear();
        int month = period.getMonthValue();
        if (month == 3) {
            last3Quarters.add(LocalDate.of(year-1, 12, 31));
            last3Quarters.add(LocalDate.of(year-1, 9, 30));
            last3Quarters.add(LocalDate.of(year-1, 6, 30));
        }
        else if (month == 6) {
            last3Quarters.add(LocalDate.of(year, 3, 31));
            last3Quarters.add(LocalDate.of(year-1, 12, 31));
            last3Quarters.add(LocalDate.of(year-1, 9, 30));
        }
        else if (month == 9) {
            last3Quarters.add(LocalDate.of(year, 6, 30));
            last3Quarters.add(LocalDate.of(year, 3, 31));
            last3Quarters.add(LocalDate.of(year-1, 12, 31));
        }
        else if (month == 12) {
            last3Quarters.add(LocalDate.of(year, 9, 30));
            last3Quarters.add(LocalDate.of(year, 6, 30));
            last3Quarters.add(LocalDate.of(year, 3, 31));
        }
        else {
            throw new RuntimeException("quarter not found");
        }
        return last3Quarters;
    }

    // probably dont need
    private static LocalDate parseQuarter(String quarterStr) {
        String yearPart = quarterStr.substring(2, 6);
        int year = Integer.parseInt(yearPart);
        int q = Integer.parseInt(quarterStr.substring(7));

        int month;
        int day;

        switch (q) {
            case 1: month = 3; day = 31; break;
            case 2: month = 6; day = 30; break;
            case 3: month = 9; day = 30; break;
            case 4: month = 12; day = 31; break;
            default: throw new IllegalArgumentException("Invalid quarter: " + q);
        }

        return LocalDate.of(year, month, day);
    }

    private static LocalDate nextQuarter(LocalDate current) {
        int month = current.getMonthValue();
        if (month == 3) {
            return LocalDate.of(current.getYear(), 6, 30);
        }
        else if (month == 6) {
            return LocalDate.of(current.getYear(), 9, 30);
        }
        else if (month == 9) {
            return LocalDate.of(current.getYear(), 12, 31);
        }
        else if (month == 12) {
            return LocalDate.of(current.getYear() + 1, 3, 31);
        }
        else {
            throw new RuntimeException("quarter not found");
        }
    }

    @GetMapping("/admin/financials")
    public String financials() throws Exception {
        Long cik = 789019L; // MSFT //  320193L AAPL
        String json = webService.fetchFinancials(cik);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        JsonNode facts = root.get("facts").get("us-gaap").get("NetIncomeLoss").get("units").get("USD");
        Map<LocalDate, Long> netIncomeQuarter = new HashMap<>();
        Map<Integer, Long> netIncomeYear = new HashMap<>();
        //List<String> forms = Arrays.asList("10-K", "10-Q");
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
            quarterService.createQuarter(cik, q.getKey(), q.getValue());
        }

        // Pretty print it
        return netIncomeQuarter.toString();
        //return mapper.writerWithDefaultPrettyPrinter()
        //             .writeValueAsString(facts);
    }
}