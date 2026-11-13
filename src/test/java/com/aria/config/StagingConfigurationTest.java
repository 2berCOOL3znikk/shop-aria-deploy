package com.aria.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class StagingConfigurationTest {
 private MockEnvironment valid(){return new MockEnvironment().withProperty("spring.datasource.url","jdbc:postgresql://"+StagingConfiguration.HOST+"/aria_staging?sslmode=require").withProperty("spring.datasource.username","aria_staging_owner").withProperty("aria.organization.key","aria-v4-staging").withProperty("spring.flyway.enabled","true").withProperty("spring.jpa.hibernate.ddl-auto","validate");}
 @Test void acceptsOnlyTheDedicatedStagingDatabase(){assertThatCode(()->StagingConfiguration.validate(valid())).doesNotThrowAnyException();}
 @Test void rejectsAnotherHostDatabaseOrConnectionOptionsBeforeMigrations(){for(String url:new String[]{"jdbc:postgresql://production.example/neondb?sslmode=require","jdbc:postgresql://"+StagingConfiguration.HOST+"/neondb?sslmode=require","jdbc:postgresql://"+StagingConfiguration.HOST+"/aria_staging?sslmode=disable"})assertThatThrownBy(()->StagingConfiguration.validate(valid().withProperty("spring.datasource.url",url))).isInstanceOf(IllegalStateException.class);}
 @Test void rejectsAnotherCompanyRoleAndUnsafeSchemaSettings(){for(var setting:java.util.Map.of("aria.organization.key","demo","spring.datasource.username","neondb_owner","spring.flyway.enabled","false","spring.jpa.hibernate.ddl-auto","update").entrySet())assertThatThrownBy(()->StagingConfiguration.validate(valid().withProperty(setting.getKey(),setting.getValue()))).isInstanceOf(IllegalStateException.class);}
}
