package com.aria.service;

import com.aria.integration.IntegrationVault;
import com.aria.integration.WooCommerceReader;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Checks existing address/storage policy without credentials, HTTP calls or writes. */
@Service
public class StoreConnectionReadiness {
    private final WooCommerceReader reader;
    private final IntegrationVault vault;
    public StoreConnectionReadiness(WooCommerceReader reader, IntegrationVault vault) {
        this.reader=reader;this.vault=vault;
    }
    public Map<String,Object> inspect(String url) {
        boolean allowed;
        try { reader.validate(url);allowed=true; }
        catch(IllegalArgumentException denied) { allowed=false; }
        boolean encrypted=vault.available();
        return Map.of("addressAllowed",allowed,"secureCredentials",encrypted,
                "readyForCredentials",allowed&&encrypted,"externalConnectionVerified",false,
                "status",!encrypted?"STORAGE_UNAVAILABLE":!allowed?"ADDRESS_REVIEW_REQUIRED":"READY_FOR_CREDENTIALS");
    }
}
