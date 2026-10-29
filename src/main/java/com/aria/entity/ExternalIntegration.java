package com.aria.entity;

import jakarta.persistence.*;

/** Metadata only. No plaintext API credentials are accepted or stored here. */
@Entity @Table(name="external_integrations")
public class ExternalIntegration {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) @JoinColumn(name="store_id",nullable=false) public ConnectedStore store;
 @Column(nullable=false,length=40) public String provider;
 @Column(nullable=false,length=30) public String status="NOT_CONNECTED";
 @Column(name="credential_reference",length=200) public String credentialReference;
 @Version public long version;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(length=4000) public String encryptedCredentials;
}
