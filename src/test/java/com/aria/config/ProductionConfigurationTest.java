package com.aria.config;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
class ProductionConfigurationTest {
    private MockEnvironment valid(){return new MockEnvironment()
        .withProperty("spring.datasource.url","jdbc:postgresql://localhost:55432/aria_test")
        .withProperty("spring.jpa.hibernate.ddl-auto","validate").withProperty("spring.flyway.enabled","true")
        .withProperty("aria.admin.email","owner@example.test").withProperty("aria.admin.password","valid-test-admin-password")
        .withProperty("jwt.secret","test-only-production-signing-secret-01234567890123456789");}
    @Test void acceptsExplicitPostgresAndStrongSecrets(){assertThatCode(()->ProductionConfiguration.validate(valid())).doesNotThrowAnyException();}
    @Test void refusesH2BeforeDatabaseCreation(){assertThatThrownBy(()->ProductionConfiguration.validate(valid().withProperty("spring.datasource.url","jdbc:h2:mem:unsafe"))).isInstanceOf(IllegalStateException.class).hasMessageContaining("PostgreSQL");}
    @Test void refusesEveryDestructiveOrDemoOverride(){
        for(String flag:new String[]{"aria.seed-demo-users","aria.seed-demo-catalog","aria.demo-market-enabled","aria.reset-data-on-start","spring.h2.console.enabled"})
            assertThatThrownBy(()->ProductionConfiguration.validate(valid().withProperty(flag,"true"))).isInstanceOf(IllegalStateException.class).hasMessageContaining(flag);
    }
    @Test void refusesWeakSecretsAndAutomaticSchemaUpdates(){
        assertThatThrownBy(()->ProductionConfiguration.validate(valid().withProperty("jwt.secret","aria-local-development-token-signing-key-change-before-any-public-host"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->ProductionConfiguration.validate(valid().withProperty("aria.admin.password","admin123"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->ProductionConfiguration.validate(valid().withProperty("spring.jpa.hibernate.ddl-auto","update"))).isInstanceOf(IllegalStateException.class);
    }
}
