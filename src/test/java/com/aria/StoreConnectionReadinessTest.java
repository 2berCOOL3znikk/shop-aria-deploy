package com.aria;

import com.aria.integration.IntegrationVault;
import com.aria.integration.WooCommerceReader;
import com.aria.service.StoreConnectionReadiness;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StoreConnectionReadinessTest {
    @Test void blockedAddressNeverRequestsCredentialsOrMakesAnExternalCall() {
        var reader=mock(WooCommerceReader.class);var vault=mock(IntegrationVault.class);
        when(reader.validate("https://127.0.0.1")).thenThrow(new IllegalArgumentException("blocked"));when(vault.available()).thenReturn(true);
        assertThat(new StoreConnectionReadiness(reader,vault).inspect("https://127.0.0.1"))
                .containsEntry("readyForCredentials",false).containsEntry("status","ADDRESS_REVIEW_REQUIRED").containsEntry("externalConnectionVerified",false);
        verify(reader).validate("https://127.0.0.1");verify(vault).available();verifyNoMoreInteractions(reader,vault);
    }
    @Test void missingEncryptionBlocksKeysEvenForAnAllowedAddress() {
        var reader=mock(WooCommerceReader.class);var vault=mock(IntegrationVault.class);
        when(reader.validate("https://shop.example")).thenReturn(URI.create("https://shop.example/wp-json/wc/v3/"));
        assertThat(new StoreConnectionReadiness(reader,vault).inspect("https://shop.example"))
                .containsEntry("addressAllowed",true).containsEntry("secureCredentials",false).containsEntry("readyForCredentials",false).containsEntry("status","STORAGE_UNAVAILABLE");
        verify(reader).validate("https://shop.example");verify(vault).available();verifyNoMoreInteractions(reader,vault);
    }
    @Test void readinessNeverClaimsThatWooCommerceOrItsCredentialsWereVerified() {
        var reader=mock(WooCommerceReader.class);var vault=mock(IntegrationVault.class);
        when(reader.validate("https://shop.example")).thenReturn(URI.create("https://shop.example/wp-json/wc/v3/"));when(vault.available()).thenReturn(true);
        assertThat(new StoreConnectionReadiness(reader,vault).inspect("https://shop.example"))
                .containsEntry("readyForCredentials",true).containsEntry("status","READY_FOR_CREDENTIALS").containsEntry("externalConnectionVerified",false).hasSize(5);
        verify(reader).validate("https://shop.example");verify(vault).available();verifyNoMoreInteractions(reader,vault);
    }
}
