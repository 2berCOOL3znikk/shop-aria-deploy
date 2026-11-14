package com.aria.service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
public final class CloudWorkspaceProof {
 private CloudWorkspaceProof(){}
 public static String sign(String secret,String message){
  try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));}
  catch(Exception e){throw new IllegalStateException("Cannot verify workspace proof");}
 }
 public static boolean equal(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII),b.getBytes(StandardCharsets.US_ASCII));}
 public static String random(){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
}
