package com.aria.config;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
@Configuration(proxyBeanMethods=false) @Profile("workspace-slot")
public class WorkspaceSlotConfiguration {
 @Bean static BeanFactoryPostProcessor workspaceSlotBoundary(Environment e){
  String binding=e.getProperty("aria.organization.key",""),host=e.getProperty("aria.saas.slot-database-host","");
  if(!binding.matches("business-[a-f0-9]{32}")||!host.matches("ep-[a-z0-9-]+(?:\\.[a-z0-9-]+)?\\.eu-central-1\\.aws\\.neon\\.tech"))throw new IllegalStateException("Invalid dedicated slot boundary");
  String suffix=binding.substring(9),expected="jdbc:postgresql://"+host+"/aria_slot_"+suffix+"?sslmode=require";
  if(!expected.equals(e.getProperty("spring.datasource.url"))||!("aria_slot_owner_"+suffix).equals(e.getProperty("spring.datasource.username"))||!e.getProperty("aria.saas.slot-enabled",Boolean.class,false)||!e.getProperty("aria.saas.slot-key","").matches("[a-f0-9]{64}"))throw new IllegalStateException("Slot requires its own database, role, binding and bootstrap key before migrations");
  if(!e.getProperty("spring.flyway.enabled",Boolean.class,false)||!"validate".equals(e.getProperty("spring.jpa.hibernate.ddl-auto")))throw new IllegalStateException("Slot requires migrations and validation");
  return factory->{};
 }
}
