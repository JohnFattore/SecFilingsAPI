package com.example.sec_api.service;

import java.util.*;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

@Service
public class WebService {

    private final RestTemplate restTemplate;
    private final HttpEntity<String> secEntity;
    private final HttpEntity<String> entity;
    // constructor
    public WebService() {
        this.restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "johnefattore@gmail.com"); // required by SEC

        // Save entity with headers
        this.secEntity = new HttpEntity<>(headers);
        this.entity = new HttpEntity<>(new HttpHeaders());
    }

    // public method
    public Map<Integer, Map<String, String>> fetchSecTickers() {
        String url = "https://www.sec.gov/files/company_tickers.json";
        ResponseEntity<Map<Integer, Map<String, String>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                secEntity,
                new ParameterizedTypeReference<Map<Integer, Map<String, String>>>() {}
        );
        return response.getBody();
    }

    public String fetchFinancials(Long cik) {
        String paddedCik = String.format("%010d", cik);
        String url = "https://data.sec.gov/api/xbrl/companyfacts/CIK" + paddedCik + ".json";
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                secEntity,
                String.class
        );
        return response.getBody();
    }
/*
    public String fetchNasdaqData(String url) {
    ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<Map<Integer, Map<String, String>>>() {}
    );
    return response.getBody();
    }
*/
}
