package com.aria.service.pricing;
import com.aria.service.CompetitorPriceService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CompetitorCsvTest {
    @Test void quotedFieldsBomSemicolonAndCrLfAreHandled(){
        String csv="\uFEFFsku;barcode;unit;price;currency;observedAt;url\r\n\"SKU;1\";4006381333931;шт;950.00;KZT;2026-10-01T10:00:00Z;\"https://example.org/a?q=\"\"b\"\"\"\r\n";
        var rows=CompetitorCsv.read(csv);assertThat(rows).hasSize(1);assertThat(rows.getFirst().getFirst()).isEqualTo("SKU;1");assertThat(rows.getFirst().get(6)).isEqualTo("https://example.org/a?q=\"b\"");
    }
    @Test void malformedInputIsRejected(){
        assertThatThrownBy(()->CompetitorCsv.read("sku,wrong\nA,B")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->CompetitorCsv.read("sku,barcode,unit,price,currency,observedAt,url\n\"unclosed")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->CompetitorCsv.read("x".repeat(200001))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void gtinCheckDigitMustMatch(){assertThat(CompetitorPriceService.validGtin("4006381333931")).isTrue();assertThat(CompetitorPriceService.validGtin("4006381333932")).isFalse();assertThat(CompetitorPriceService.validGtin("SKU1")).isFalse();}
}
