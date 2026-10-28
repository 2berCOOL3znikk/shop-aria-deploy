package com.aria.config;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class DatabaseUrlConfigurationTest {
    @Test void extractsRenderCredentialsWithoutIncludingThemInJdbcUrl(){
        var values=DatabaseUrlConfiguration.parse("jdbc:postgresql://owner:secret%3Awith%40special+chars@db.internal:5432/aria?sslmode=require");
        assertThat(values).containsEntry("spring.datasource.url","jdbc:postgresql://db.internal:5432/aria?sslmode=require")
            .containsEntry("spring.datasource.username","owner").containsEntry("spring.datasource.password","secret:with@special+chars");
    }
    @Test void acceptsJdbcUrlWithoutUserinfoAndPostgresAlias(){
        assertThat(DatabaseUrlConfiguration.parse("jdbc:postgresql://localhost:55432/aria")).containsOnlyKeys("spring.datasource.url");
        assertThat(DatabaseUrlConfiguration.parse("postgres://owner:password@db/aria")).containsEntry("spring.datasource.url","jdbc:postgresql://db/aria");
    }
    @Test void parsingFailureDoesNotExposeConnectionSecret(){
        assertThatThrownBy(()->DatabaseUrlConfiguration.parse("postgresql://owner:never-print-this@/aria")).isInstanceOf(IllegalStateException.class).hasMessageNotContaining("never-print-this");
    }
}
