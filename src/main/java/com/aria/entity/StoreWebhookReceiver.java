package com.aria.entity;
import jakarta.persistence.*;
@Entity @Table(name="store_webhook_receivers")
public class StoreWebhookReceiver {
 @Id public Long storeId;
 @Column(unique=true,length=64) public String routeToken;
 @Column(length=4000) @com.fasterxml.jackson.annotation.JsonIgnore public String encryptedSecret;
 @Column(nullable=false) public boolean enabled;
 public java.time.LocalDateTime lastAcceptedAt;
 @Version public long version;
}
