package com.aria.service;
/** No personal data. Durable task intent joins the business transaction; worker wakes only after commit. */
public record StoreChanged(Long productId,String cause) {}
