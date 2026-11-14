package com.aria;
import com.aria.security.WorkspaceGatewayFilter;
import com.aria.service.CloudRegistrationService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkspaceGatewaySecurityTest {
 private CloudRegistrationService registry;private WorkspaceGatewayFilter filter;
 @BeforeEach void setup(){registry=mock(CloudRegistrationService.class);when(registry.enabled()).thenReturn(true);filter=new WorkspaceGatewayFilter(registry,new MockEnvironment().withProperty("aria.organization.key","aria-v4-staging"));}
 private String token(String binding){return "e30."+Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"iss\":\"aria:"+binding+"\",\"organization\":\""+binding+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8))+".signature-is-not-authority";}
 private MockHttpServletRequest request(String path){var r=new MockHttpServletRequest("GET",path);r.setServletPath(path);return r;}
 @Test void unknownIssuerOrConflictingViewCannotReachAnyBackend()throws Exception{for(String hint:List.of("","business-other")){var r=request("/api/admin/control/products");r.addHeader("Authorization","Bearer "+token("business-new"));if(!hint.isEmpty())r.addHeader("X-ARIA-Workspace",hint);var response=new MockHttpServletResponse();filter.doFilter(r,response,(a,b)->{throw new AssertionError("Must not fall back to platform");});assertThat(response.getStatus()).isEqualTo(401);}}
 @Test void workspaceHintWithoutBearerCannotOpenBusinessApis()throws Exception{var r=request("/api/admin/control/products");r.addHeader("X-ARIA-Workspace","business-other");var called=new AtomicInteger();filter.doFilter(r,new MockHttpServletResponse(),(a,b)->called.incrementAndGet());assertThat(called.get()).isEqualTo(1);verify(registry,never()).slotByBinding(anyString());}
 @Test void platformAdministrationIsNeverForwardedUsingTenantClaims()throws Exception{var r=request("/api/admin/workspace-pool");r.addHeader("Authorization","Bearer "+token("business-other"));filter.doFilter(r,new MockHttpServletResponse(),(a,b)->{});verify(registry,never()).slotByBinding(anyString());}
 @Test void ambiguousPathsAndInternalBootstrapAreRejectedBeforeRouting()throws Exception{for(String path:List.of("/api/internal/workspace/bootstrap","/api/../internal/workspace/bootstrap","/api//admin/control/products","/api/admin%2fcontrol/products")){var r=request(path);var response=new MockHttpServletResponse();filter.doFilter(r,response,(a,b)->{throw new AssertionError("Rejected path reached application");});assertThat(response.getStatus()).isIn(400,404);}}
 @Test void malformedLoginDoesNotExposeAParserErrorOrReachAnotherService()throws Exception{var r=request("/api/auth/login");r.setMethod("POST");r.setContent("{".getBytes(java.nio.charset.StandardCharsets.UTF_8));var response=new MockHttpServletResponse();filter.doFilter(r,response,(a,b)->{throw new AssertionError("Malformed login reached application");});assertThat(response.getStatus()).isEqualTo(400);verify(registry,never()).loginSlot(anyString());}
}
