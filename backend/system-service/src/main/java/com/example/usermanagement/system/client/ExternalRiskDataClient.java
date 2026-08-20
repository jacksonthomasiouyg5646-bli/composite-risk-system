package com.example.usermanagement.system.client;

import com.example.usermanagement.system.client.fallback.ExternalRiskDataClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Canonical adapter for third-party enterprise risk data providers.
 * The provider only receives the minimal lookup identity assembled by the service.
 */
@FeignClient(
        name = "external-risk-data",
        contextId = "externalRiskDataClient",
        url = "${risk.ai.external-data.base-url:http://host.docker.internal:19090}",
        path = "${risk.ai.external-data.api-path:/api/v1/risk-profiles}",
        fallbackFactory = ExternalRiskDataClientFallbackFactory.class
)
public interface ExternalRiskDataClient {
    @PostMapping(value = "${risk.ai.external-data.query-path:/query}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> queryCustomerRisk(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey
    );
}
