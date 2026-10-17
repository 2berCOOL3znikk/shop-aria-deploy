package com.aria.service;

import com.aria.entity.Product;
import com.aria.entity.WishlistItem;
import com.aria.repository.ProductRepository;
import com.aria.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private FavoriteServicebak favoriteService;

    @Test
    void addFavorite_isIdempotent() {
        // given
        Product product = new Product();
        product.setId(1L);
        product.setName("Товар");

        WishlistItem existingItem = new WishlistItem(product);
        when(wishlistRepository.findByProductId(1L)).thenReturn(Optional.of(existingItem));

        // when
        favoriteService.addFavorite(1L);

        // then: не должен вызывать save, так как товар уже в избранном
        verify(wishlistRepository, never()).save(any(WishlistItem.class));
    }

    @Test
    void addFavorite_savesNewItem_whenNotAlreadyFavorite() {
        // given
        Product product = new Product();
        product.setId(2L);
        product.setName("Новый товар");

        when(wishlistRepository.findByProductId(2L)).thenReturn(Optional.empty());
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(wishlistRepository.save(any(WishlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        favoriteService.addFavorite(2L);

        // then
        verify(wishlistRepository, times(1)).save(any(WishlistItem.class));
    }

    @Test
    void removeFavorite_deletesItem() {
        // given
        WishlistItem item = new WishlistItem();
        item.setId(1L);

        when(wishlistRepository.findByProductId(1L)).thenReturn(Optional.of(item));

        // when
        favoriteService.removeFavorite(1L);

        // then
        verify(wishlistRepository, times(1)).delete(item);
    }

    @Test
    void removeFavorite_doesNothing_whenItemNotExists() {
        // given
        when(wishlistRepository.findByProductId(999L)).thenReturn(Optional.empty());

        // when
        favoriteService.removeFavorite(999L);

        // then: не должен вызывать delete
        verify(wishlistRepository, never()).delete(any());
    }

    @Test
    void isFavorite_returnsTrue_whenItemExists() {
        // given
        WishlistItem item = new WishlistItem();
        when(wishlistRepository.existsByProductId(1L)).thenReturn(true);

        // when
        boolean result = favoriteService.isFavorite(1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isFavorite_returnsFalse_whenItemNotExists() {
        // given
        when(wishlistRepository.existsByProductId(2L)).thenReturn(false);

        // when
        boolean result = favoriteService.isFavorite(2L);

        // then
        assertThat(result).isFalse();
    }
}
