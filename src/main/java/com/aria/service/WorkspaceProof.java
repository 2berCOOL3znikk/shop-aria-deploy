package com.aria.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/** Versioned nonce-bound proof; no password, contact address or database credentials in the payload. */
public final class WorkspaceProof {
 private WorkspaceProof(){}
 public static String sign(String key,String request,String binding,String ownerHash,String nonce,Instant deadline,Instant claimed){
  return mac(key,String.join("\n","aria-workspace-v1",request,binding,ownerHash,nonce,deadline.toString(),claimed==null?"pending":claimed.toString()));
 }
 public static String requestProof(String key,String request,String nonce){return mac(key,String.join("\n","aria-workspace-request-v1",request,nonce));}
 private static String mac(String key,String payload){
  try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));}
  catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Не удалось подтвердить готовность кабинета");}
 }
 public static boolean equal(String first,String second){return first!=null&&second!=null&&MessageDigest.isEqual(first.getBytes(StandardCharsets.US_ASCII),second.getBytes(StandardCharsets.US_ASCII));}
}
