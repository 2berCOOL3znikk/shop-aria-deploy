package com.aria.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AssistantQueryTest {
    @Test void rubleOrTengeBudgetDoesNotInventExchangeRate(){var q=AssistantQuery.parse("Найди товары до 2 000 ₸");assertThat(q.budget()).isEqualByComparingTo("2000");assertThat(q.intent()).isEqualTo("BUDGET");assertThat(q.terms()).isEmpty();}
    @Test void discountNegationIsPreserved(){assertThat(AssistantQuery.parse("Что сейчас со скидкой?").discountsOnly()).isTrue();assertThat(AssistantQuery.parse("Товары без скидок").withoutDiscounts()).isTrue();assertThat(AssistantQuery.parse("Товары без скидок").discountsOnly()).isFalse();}
    @Test void comparisonAndCategoryRequestsAreRecognized(){assertThat(AssistantQuery.parse("Сравни эти товары").intent()).isEqualTo("COMPARE");assertThat(AssistantQuery.parse("Посоветуй недорогие продукты").terms()).contains("продукты");}
    @Test void foreignCurrencyIsNotSilentlyTreatedAsTenge(){assertThatThrownBy(()->AssistantQuery.parse("до 10 USD")).isInstanceOf(IllegalArgumentException.class);assertThatCode(()->AssistantQuery.parse("Подбери рубашку до 2000 ₸")).doesNotThrowAnyException();}
    @Test void compactBudgetAndIsoCurrencyDoNotBecomeSearchTerms(){var q=AssistantQuery.parse("Покажи все товары до2000 KZT");assertThat(q.budget()).isEqualByComparingTo("2000");assertThat(q.terms()).isEmpty();}
}
