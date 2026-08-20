package com.example.usermanagement.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ExternalRiskDataProperties {
    @Value("${risk.ai.external-data.enabled:false}")
    private boolean enabled;

    @Value("${risk.ai.external-data.provider-name:External Risk Data}")
    private String providerName;

    @Value("${risk.ai.external-data.bearer-token:}")
    private String bearerToken;

    @Value("${risk.ai.external-data.api-key:}")
    private String apiKey;

    public boolean isEnabled() {
        return enabled;
    }

    public String getProviderName() {
        return providerName == null || providerName.isBlank() ? "External Risk Data" : providerName.trim();
    }

    public String getAuthorization() {
        if (bearerToken == null || bearerToken.isBlank()) {
            return "";
        }
        return "Bearer " + bearerToken.trim();
    }

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }
}
