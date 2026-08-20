package com.example.usermanagement.system.client.fallback;

import com.example.usermanagement.system.client.ExternalRiskDataClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExternalRiskDataClientFallbackFactory implements FallbackFactory<ExternalRiskDataClient> {
    private static final Logger log = LoggerFactory.getLogger(ExternalRiskDataClientFallbackFactory.class);

    @Override
    public ExternalRiskDataClient create(Throwable cause) {
        log.warn("external risk data provider is unavailable; local AI analysis will be used", cause);
        return (request, authorization, apiKey) -> Map.of(
                "_external_status", "UNAVAILABLE",
                "_external_message", "External data provider is temporarily unavailable"
        );
    }
}
