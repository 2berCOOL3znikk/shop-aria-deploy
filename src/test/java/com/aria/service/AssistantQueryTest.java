package com.aria.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AssistantQueryTest {
    @Test void rubleOrTengeBudgetDoesNotInventExchangeRate(){var q=AssistantQuery.parse("Найди товары до 2 000 ₸");assertThat(q.budget()).isEqualByComparingTo("2000");assertThat(q.intent()).isEqualTo("BUDGET");assertThat(q.terms()).isEmpty();}
    @Test void discountNegationIsPreserved(){assertThat(AssistantQuery.parse("Что сейчас со скидкой?").discountsOnly()).isTrue();assertThat(AssistantQuery.parse("Товары без скидок").withoutDiscounts()).isTrue();assertThat(AssistantQuery.parse("Товары без скидок").discountsOnly()).isFalse();}
    @Test void comparisonAndCategoryRequestsAreRecognized(){assertThat(AssistantQuery.parse("Сравни эти товары").intent()).isEqualTo("COMPARE");assertThat(AssistantQuery.parse("Посоветуй недорогие продукты").terms()).contains("продукты");}
    @Test void foreignCurrencyIsNotSilentlyTreatedAsTenge(){assertThatThrownBy(()->AssistantQuery.parse("до 10 USD")).isInstanceOf(IllegalArgumentException.class);assertThatCode(()->AssistantQuery.parse("Подбери рубашку до 2000 ₸")).doesNotThrowAnyException();}
    @Test void compactBudgetAndIsoCurrencyDoNotBecomeSearchTerms(){var q=AssistantQuery.parse("Покажи все товары до2000 KZT");assertThat(q.budget()).isEqualByComparingTo("2000");assertThat(q.terms()).isEmpty();}
    @Test void englishBudgetsPreserveThousandsAndDecimals(){
        for(String text:java.util.List.of("Find products under 2,000 KZT","Show all products up to 2 000 ₸","Please find items no more than 2000 tenge")){
            var q=AssistantQuery.parse(text);assertThat(q.budget()).isEqualByComparingTo("2000");assertThat(q.terms()).isEmpty();
        }
        assertThat(AssistantQuery.parse("milk less than 1,500.50 KZT").budget()).isEqualByComparingTo("1500.50");
        assertThat(AssistantQuery.parse("milk under 125,50 ₸").budget()).isEqualByComparingTo("125.50");
        assertThatThrownBy(()->AssistantQuery.parse("Find milk under 10 dollars")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void englishShoppingIntentsDoNotBecomeUnrelatedSearchTerms(){
        var sale=AssistantQuery.parse("What is currently on sale?");assertThat(sale.discountsOnly()).isTrue();assertThat(sale.terms()).isEmpty();
        var fullPrice=AssistantQuery.parse("Show products without discounts");assertThat(fullPrice.withoutDiscounts()).isTrue();assertThat(fullPrice.discountsOnly()).isFalse();assertThat(fullPrice.terms()).isEmpty();
        assertThat(AssistantQuery.parse("Compare these selected products").intent()).isEqualTo("COMPARE");
        assertThat(AssistantQuery.parse("Find similar chocolate").intent()).isEqualTo("ALTERNATIVES");
        assertThat(AssistantQuery.parse("Recommend cheap milk").terms()).containsExactly("milk");
    }
}
