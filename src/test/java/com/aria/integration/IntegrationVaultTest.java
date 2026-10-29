package com.aria.integration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IntegrationVaultTest {
 @Test void authenticatesEncryptionAndUsesFreshNonce(){var v=new IntegrationVault(java.util.Base64.getEncoder().encodeToString(new byte[32]));var a=v.encrypt("only-test-credentials");assertNotEquals(a,v.encrypt("only-test-credentials"));assertEquals("only-test-credentials",v.decrypt(a));assertFalse(a.contains("only-test"));assertThrows(IllegalStateException.class,()->v.decrypt(a.substring(0,a.length()-4)+"AAAA"));}
 @Test void refusesStorageWithoutServerKey(){assertFalse(new IntegrationVault("").available());assertThrows(IllegalStateException.class,()->new IntegrationVault("").encrypt("test"));}
 @Test void rejectsUnapprovedAndUnsafeDestinations(){var w=new WooCommerceReader("");for(var url:java.util.List.of("https://example.com","http://localhost","https://127.0.0.1","https://user:pass@example.com"))assertThrows(IllegalArgumentException.class,()->w.validate(url));}
}
