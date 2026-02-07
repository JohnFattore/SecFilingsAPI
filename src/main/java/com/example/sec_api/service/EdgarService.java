package com.example.sec_api.service;

import com.example.sec_api.model.Asset;
import com.example.sec_api.model.Quarter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class EdgarService {

    private final WebService webService;
    private final QuarterService quarterService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, List<String>> FIELD_TO_TAGS = Map.ofEntries(
            Map.entry("netIncomeLoss",
                    List.of("NetIncomeLoss", "NetIncomeLossAvailableToCommonStockholdersBasic", "NetIncomeLossNet",
                            "ProfitLoss")),
            Map.entry("revenue",
                    List.of("RevenueFromContractWithCustomerExcludingAssessedTax", "Revenues", "SalesRevenueNet",
                            "SalesRevenueGoodsNet", "SalesRevenueServicesNet")),
            Map.entry("operatingCashFlow",
                    List.of("NetCashProvidedByUsedInOperatingActivities",
                            "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations")),
            Map.entry("costOfRevenue",
                    List.of("CostOfGoodsAndServicesSold", "CostOfSales", "CostOfRevenue")),
            Map.entry("grossProfit", List.of("GrossProfit")),
            Map.entry("operatingIncome", List.of("OperatingIncomeLoss")),
            Map.entry("assets", List.of("Assets")),
            Map.entry("liabilities", List.of("Liabilities")),
            Map.entry("equity", List.of("StockholdersEquity", "CommonStockholdersEquity")),
            Map.entry("longTermDebt",
                    List.of("LongTermDebtNoncurrent", "LongTermDebt", "LongTermDebtCurrent")),
            Map.entry("inventory", List.of("InventoryNet", "InventoryFinishedGoods")),
            Map.entry("cash", List.of("CashAndCashEquivalentsAtCarryingValue", "CashAndCashEquivalents")),
            Map.entry("epsBasic", List.of("EarningsPerShareBasic")));

    private static final Set<String> STOCK_FIELDS = Set.of("assets", "liabilities", "equity", "longTermDebt",
            "inventory", "cash");

    public EdgarService(WebService webService, QuarterService quarterService) {
        this.webService = webService;
        this.quarterService = quarterService;
    }

    public void updateFinancials(Asset asset) throws Exception {
        String json = webService.fetchFinancials(asset.getCik());
        JsonNode root = mapper.readTree(json);

        Map<String, Map<String, FactData>> quarterData = getQuarterlyFacts(FIELD_TO_TAGS, root);
        List<Quarter> quarters = new ArrayList<>();

        for (Map.Entry<String, Map<String, FactData>> entry : quarterData.entrySet()) {
            Map<String, FactData> facts = entry.getValue();

            // Extract dates from identifying facts
            LocalDate periodStart = null;
            LocalDate periodEnd = null;

            // Try to find dates from stock fields or flow fields
            for (FactData fd : facts.values()) {
                if (fd.startDate != null && fd.endDate != null) {
                    periodStart = fd.startDate;
                    periodEnd = fd.endDate;
                    break;
                }
            }

            if (periodStart == null || periodEnd == null)
                continue;

            // Strict filtering for ~3 month quarters (80 to 100 days)
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
            if (daysDiff < 80)
                continue;

            Quarter quarter = new Quarter();
            quarter.setAsset(asset);
            quarter.setPeriodStart(periodStart);
            quarter.setPeriodEnd(periodEnd);

            FactData netIncome = facts.get("netIncomeLoss");
            if (netIncome != null) {
                quarter.setNetIncomeLoss(netIncome.asLong());
            }

            quarter.setRevenue(facts.containsKey("revenue") ? facts.get("revenue").asLong() : null);
            quarter.setCostOfRevenue(facts.containsKey("costOfRevenue") ? facts.get("costOfRevenue").asLong() : null);
            quarter.setGrossProfit(facts.containsKey("grossProfit") ? facts.get("grossProfit").asLong() : null);
            quarter.setOperatingIncome(
                    facts.containsKey("operatingIncome") ? facts.get("operatingIncome").asLong() : null);
            quarter.setAssets(facts.containsKey("assets") ? facts.get("assets").asLong() : null);
            quarter.setLiabilities(facts.containsKey("liabilities") ? facts.get("liabilities").asLong() : null);
            quarter.setEquity(facts.containsKey("equity") ? facts.get("equity").asLong() : null);
            quarter.setLongTermDebt(facts.containsKey("longTermDebt") ? facts.get("longTermDebt").asLong() : null);
            quarter.setInventory(facts.containsKey("inventory") ? facts.get("inventory").asLong() : null);
            quarter.setOperatingCashFlow(
                    facts.containsKey("operatingCashFlow") ? facts.get("operatingCashFlow").asLong() : null);
            quarter.setCash(facts.containsKey("cash") ? facts.get("cash").asLong() : null);
            quarter.setEpsBasic(facts.containsKey("epsBasic") ? facts.get("epsBasic").asDouble() : null);

            quarters.add(quarter);
        }
        quarterService.updateAssetQuarters(asset, quarters);
    }

    private static class FactData {
        Object value;
        LocalDate startDate;
        LocalDate endDate;

        FactData(Object value, LocalDate startDate, LocalDate endDate) {
            this.value = value;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        double asDouble() {
            if (value instanceof Number)
                return ((Number) value).doubleValue();
            return 0.0;
        }

        long asLong() {
            if (value instanceof Number)
                return ((Number) value).longValue();
            return 0L;
        }
    }

    private Map<String, Map<String, FactData>> getQuarterlyFacts(Map<String, List<String>> fieldToTags,
            JsonNode root) {
        Map<String, Map<String, FactData>> quarterData = new HashMap<>();
        JsonNode factsNode = root.get("facts");
        if (factsNode == null)
            return quarterData;

        JsonNode usGaap = factsNode.get("us-gaap");
        JsonNode dei = factsNode.get("dei");

        for (Map.Entry<String, List<String>> fieldEntry : fieldToTags.entrySet()) {
            String fieldName = fieldEntry.getKey();
            List<String> tags = fieldEntry.getValue();
            boolean isStock = STOCK_FIELDS.contains(fieldName);

            // Grouping: end date string -> duration -> FactData
            Map<String, Map<Integer, FactData>> dataPoints = new HashMap<>();

            for (String tag : tags) {
                JsonNode conceptNode = (usGaap != null && usGaap.has(tag)) ? usGaap.get(tag)
                        : (dei != null && dei.has(tag)) ? dei.get(tag) : null;

                if (conceptNode == null)
                    continue;

                JsonNode units = conceptNode.get("units");
                if (units == null)
                    continue;

                JsonNode unitNode = units.get("USD");
                if (unitNode == null)
                    unitNode = units.get("USD/shares");
                if (unitNode == null)
                    unitNode = units.get("USD-per-shares");
                if (unitNode == null)
                    unitNode = units.get("pure");
                if (unitNode == null)
                    unitNode = units.get("shares");

                if (unitNode == null)
                    continue;

                for (JsonNode entry : unitNode) {
                    if (!entry.has("end"))
                        continue;

                    String endStr = entry.get("end").asText();
                    LocalDate endDate = LocalDate.parse(endStr);

                    LocalDate startDate = null;
                    int durationMonths = 0; // 0 for stock/instant

                    if (entry.has("start")) {
                        try {
                            startDate = LocalDate.parse(entry.get("start").asText());
                            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);

                            // Approximate duration logic for bucketing
                            if (days > 80 && days < 100)
                                durationMonths = 3;
                            else if (days > 170 && days < 190)
                                durationMonths = 6;
                            else if (days > 260 && days < 280)
                                durationMonths = 9;
                            else if (days > 350 && days < 375)
                                durationMonths = 12;
                            else
                                durationMonths = (int) (days / 30);
                        } catch (Exception ignored) {
                        }
                    }

                    if (isStock) {
                        // Stock fields are point-in-time, effectively duration 0 or treat as 3 for
                        // grouping convenience?
                        // Actually, we group by end date.
                        // For stock fields, start date doesn't matter as much, but we need it for the
                        // model.
                        // Usually stock fields don't have start date in SEC JSON, or start=end.
                        // If missing, assume it's a snapshot.
                        if (startDate == null)
                            startDate = endDate;
                        durationMonths = 0;
                    } else {
                        if (startDate == null)
                            continue; // Flow metrics must have start date
                    }

                    JsonNode valNode = entry.get("val");
                    Object value = valNode.isNumber()
                            ? (valNode.isFloatingPointNumber() ? valNode.asDouble() : valNode.asLong())
                            : valNode.asText();

                    // Key by End Date for grouping similar durations
                    dataPoints.computeIfAbsent(endStr, k -> new HashMap<>()).putIfAbsent(durationMonths,
                            new FactData(value, startDate, endDate));
                }
            }

            // Process collected points
            for (Map.Entry<String, Map<Integer, FactData>> dateEntry : dataPoints.entrySet()) {
                Map<Integer, FactData> durations = dateEntry.getValue();

                FactData selectedData = null;
                if (isStock) {
                    // Take the snapshot (duration 0 usually, or whatever is there)
                    selectedData = durations.values().stream().findFirst().orElse(null);
                } else {
                    if (durations.containsKey(3)) {
                        selectedData = durations.get(3);
                    } else {
                        // Derive Q3 from YTD9 - YTD6, etc.
                        for (int d : new int[] { 6, 9, 12 }) {
                            if (durations.containsKey(d)) {
                                FactData currentYtd = durations.get(d);
                                // Look for previous YTD (d-3)
                                // Previous YTD should end exactly at currentYtd.startDate - 1 day?
                                // No, Q3 = YTD9 - YTD6.
                                // YTD9: 1/1 -> 9/30. YTD6: 1/1 -> 6/30.
                                // We need to find YTD6 which has Start=YTD9.Start and Duration=d-3.

                                // We need to search across ALL dataPoints to find the matching Start/Duration

                                // This requires a different lookup strategy.
                                // Let's try to look up by date.
                                // We need a YTD(d-3) that starts on currentYtd.startDate.

                                // But we know the EndDate of the previous YTD period.
                                // YTD6 end date should be roughly YTD9 end date - 3 months.

                                // Try to match distinct end dates in the map
                                for (Map.Entry<String, Map<Integer, FactData>> otherEntry : dataPoints.entrySet()) {
                                    Map<Integer, FactData> otherDurations = otherEntry.getValue();
                                    if (otherDurations.containsKey(d - 3)) {
                                        FactData prevCandidate = otherDurations.get(d - 3);
                                        // Must share same start date
                                        if (prevCandidate.startDate.equals(currentYtd.startDate)) {
                                            // Found valid predecessor
                                            double currentVal = currentYtd.asDouble();
                                            double prevVal = prevCandidate.asDouble();

                                            LocalDate derivedStart = prevCandidate.endDate.plusDays(1);

                                            selectedData = new FactData(
                                                    currentVal - prevVal,
                                                    derivedStart,
                                                    currentYtd.endDate);
                                            break;
                                        }
                                    }
                                }
                                if (selectedData != null)
                                    break;
                            }
                        }
                    }
                }

                if (selectedData != null) {
                    String periodKey = selectedData.startDate.toString() + "|" + selectedData.endDate.toString();
                    quarterData.computeIfAbsent(periodKey, k -> new HashMap<>()).putIfAbsent(fieldName, selectedData);
                }
            }
        }
        return quarterData;
    }
}
