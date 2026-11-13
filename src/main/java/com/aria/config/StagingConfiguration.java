package com.aria.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Refuse a misrouted staging process before Flyway can open any database. */
@Configuration(proxyBeanMethods=false) @Profile("staging")
public class StagingConfiguration {
 static final String HOST="ep-weathered-art-ba8o4nc5.c-8.eu-central-1.aws.neon.tech";
 @Bean static BeanFactoryPostProcessor stagingDatabaseBoundary(Environment env){validate(env);return factory->{};}
 static void validate(Environment env){
  String expected="jdbc:postgresql://"+HOST+"/aria_staging?sslmode=require";
  if(!expected.equals(env.getProperty("spring.datasource.url"))||!"aria_staging_owner".equals(env.getProperty("spring.datasource.username"))||!"aria-v4-staging".equals(env.getProperty("aria.organization.key")))
   throw new IllegalStateException("Staging requires its dedicated database, role and company binding; migrations were not started");
  if(!env.getProperty("spring.flyway.enabled",Boolean.class,false)||!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")))throw new IllegalStateException("Staging requires migrations and schema validation");
 }
}
