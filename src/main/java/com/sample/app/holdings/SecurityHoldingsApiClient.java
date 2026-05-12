package com.sample.app.holdings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SecurityHoldingsApiClient {

    private static final Logger log = LoggerFactory.getLogger(SecurityHoldingsApiClient.class);

    private static final String PATH = "/api/v1/securities/{instrumentId}/portfolio-holdings";

    private final RestClient restClient;

    public SecurityHoldingsApiClient(
            @Value("${app.holdings-api.base-url:http://localhost:18089}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .build();
    }

    /**
     * Calls the mocked Security Holdings API (WireMock in docker-compose).
     */
    public String fetchPortfolioHoldingsJson(String instrumentId) {
        String body = restClient.get()
                .uri(PATH, instrumentId == null ? "" : instrumentId)
                .retrieve()
                .body(String.class);

        if (body == null) {
            return "{\"error\":\"empty response from holdings API\"}";
        }
        if (instrumentId != null && !instrumentId.isBlank()) {
            body = body.replace("__INSTRUMENT_ID__", instrumentId);
        }
        log.debug("Holdings API response for {}: {}", instrumentId, body);
        return body;
    }
}
