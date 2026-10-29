package com.aria.security;

import com.aria.entity.AppUser;
import com.aria.enums.Role;
import com.aria.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationTokenTest {
 private JwtService service(String company){
  var users=mock(AppUserRepository.class);when(users.findByEmail("same@example.test")).thenReturn(Optional.of(new AppUser("same@example.test","hash","Owner",Role.ADMIN)));
  var service=new JwtService(users);
  ReflectionTestUtils.setField(service,"secret","test-key-shared-deliberately-between-two-isolated-companies-123456789");
  ReflectionTestUtils.setField(service,"organizationKey",company);return service;
 }
 @Test void rejectsOtherCompanyEvenWithSameEmailAndSigningKey(){
  var a=service("company-a");var b=service("company-b");var token=a.generateToken("same@example.test",Role.ADMIN);
  assertTrue(a.isTokenValid(token,"same@example.test"));
  assertThrows(io.jsonwebtoken.IncorrectClaimException.class,()->b.isTokenValid(token,"same@example.test"));
  assertThrows(io.jsonwebtoken.IncorrectClaimException.class,()->b.extractEmail(token));
 }
 @Test void rejectsLegacyUnscopedToken(){
  var key=io.jsonwebtoken.security.Keys.hmacShaKeyFor("test-key-shared-deliberately-between-two-isolated-companies-123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var token=io.jsonwebtoken.Jwts.builder().subject("same@example.test").signWith(key).compact();
  assertThrows(io.jsonwebtoken.MissingClaimException.class,()->service("demo").extractEmail(token));
 }
}
