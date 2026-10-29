package com.aria.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
@Component
public class IntegrationVault {
 private final byte[] key;
 public IntegrationVault(@Value("${aria.integrations.encryption-key:}") String value){
  key=value.isBlank()?null:Base64.getDecoder().decode(value);
  if(key!=null&&key.length!=32)throw new IllegalStateException("Integration encryption key must be 32 bytes");
 }
 public boolean available(){return key!=null;}
 public String encrypt(String text){
  requireKey();try{byte[] nonce=new byte[12];new java.security.SecureRandom().nextBytes(nonce);var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));return Base64.getEncoder().encodeToString(nonce)+":"+Base64.getEncoder().encodeToString(c.doFinal(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Не удалось защитить доступы");}
 }
 public String decrypt(String value){
  requireKey();try{var parts=value.split(":");var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(c.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("Доступы недоступны. Настройте подключение заново");}
 }
 private void requireKey(){if(key==null)throw new IllegalStateException("Сохранение ключей пока не настроено на сервере");}
}
