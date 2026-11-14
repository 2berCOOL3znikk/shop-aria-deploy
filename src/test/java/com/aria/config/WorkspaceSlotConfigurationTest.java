package com.aria.config;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
class WorkspaceSlotConfigurationTest {
 private MockEnvironment valid(){String id="a".repeat(32),host="ep-new-test-ba123.c-8.eu-central-1.aws.neon.tech";return new MockEnvironment().withProperty("aria.organization.key","business-"+id).withProperty("aria.saas.slot-database-host",host).withProperty("spring.datasource.url","jdbc:postgresql://"+host+"/aria_slot_"+id+"?sslmode=require").withProperty("spring.datasource.username","aria_slot_owner_"+id).withProperty("aria.saas.slot-enabled","true").withProperty("aria.saas.slot-key","aa".repeat(32)).withProperty("spring.flyway.enabled","true").withProperty("spring.jpa.hibernate.ddl-auto","validate");}
 @Test void acceptsExactDedicatedBoundaryBeforeMigrations(){assertThatCode(()->WorkspaceSlotConfiguration.workspaceSlotBoundary(valid())).doesNotThrowAnyException();}
 @Test void rejectsOtherDatabaseOrRoleBeforeMigrations(){for(String key:new String[]{"spring.datasource.url","spring.datasource.username","aria.organization.key"})assertThatThrownBy(()->WorkspaceSlotConfiguration.workspaceSlotBoundary(valid().withProperty(key,"existing-workspace"))).isInstanceOf(IllegalStateException.class);}
 @Test void rejectsMissingKeyOrDisabledMigration(){assertThatThrownBy(()->WorkspaceSlotConfiguration.workspaceSlotBoundary(valid().withProperty("aria.saas.slot-key",""))).isInstanceOf(IllegalStateException.class);assertThatThrownBy(()->WorkspaceSlotConfiguration.workspaceSlotBoundary(valid().withProperty("spring.flyway.enabled","false"))).isInstanceOf(IllegalStateException.class);}
}
