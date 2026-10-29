package com.aria.config;
import com.aria.service.AiManagerService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.*;
import java.util.Set;
@Configuration
public class ManagerActivityConfiguration implements WebMvcConfigurer {
 private final AiManagerService manager;
 public ManagerActivityConfiguration(AiManagerService manager){this.manager=manager;}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){
  @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception exception){
   if(exception==null&&response.getStatus()<400)try{manager.activity(!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod())&&!request.getRequestURI().startsWith("/api/cart"));}catch(RuntimeException ignored){org.slf4j.LoggerFactory.getLogger(ManagerActivityConfiguration.class).warn("Manager activity will be retried after the next request");}
  }
 }).addPathPatterns("/api/products/store","/api/orders/**","/api/cart/**","/api/admin/control/products/**","/api/admin/control/lots/**","/api/admin/control/purchases/**","/api/admin/control/pos/**");}
}
