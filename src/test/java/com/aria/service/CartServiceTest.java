package com.aria.service;

import com.aria.entity.CartItem;
import com.aria.entity.Product;
import com.aria.enums.ProductStatus;
import com.aria.repository.CartItemRepository;
import com.aria.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addToCart_storesPriceAtAddTimeEqualToUnitPrice() {
        // given
        Product product = new Product();
        product.setId(1L);
        product.setName("Товар");
        product.setUnitPrice(new BigDecimal("99.99"));
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CartItem cartItem = cartService.addToCart(1L, 2);

        // then
        assertThat(cartItem.getPriceAtAddTime()).isEqualByComparingTo("99.99");
        assertThat(cartItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void addToCart_throwsIllegalStateException_whenStatusNotActive() {
        // given
        Product inactiveProduct = new Product();
        inactiveProduct.setId(2L);
        inactiveProduct.setName("Неактивный товар");
        inactiveProduct.setUnitPrice(new BigDecimal("50"));
        inactiveProduct.setStatus(ProductStatus.CANDIDATE);

        when(productRepository.findById(2L)).thenReturn(Optional.of(inactiveProduct));

        // when + then
        assertThatThrownBy(() -> cartService.addToCart(2L, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("недоступен для покупки");
    }

    @Test
    void calculateTotal_multipliesPriceByQuantity() {
        // given
        CartItem item1 = new CartItem();
        item1.setPriceAtAddTime(new BigDecimal("100"));
        item1.setQuantity(2);

        CartItem item2 = new CartItem();
        item2.setPriceAtAddTime(new BigDecimal("50"));
        item2.setQuantity(3);

        when(cartItemRepository.findAllByOrderByAddedAtDesc()).thenReturn(List.of(item1, item2));

        // when
        BigDecimal total = cartService.calculateTotal();

        // then: 100*2 + 50*3 = 200 + 150 = 350
        assertThat(total).isEqualByComparingTo("350");
    }

    @Test
    void checkout_emptiesTheCart() {
        // when
        cartService.checkout();

        // then
        verify(cartItemRepository, times(1)).deleteAll();
    }
}
