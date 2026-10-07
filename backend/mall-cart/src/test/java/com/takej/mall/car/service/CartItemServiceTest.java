package com.takej.mall.car.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.cart.dto.AddCartItemRequest;
import com.takej.mall.cart.entity.CartItem;
import com.takej.mall.cart.mapper.CartItemMapper;
import com.takej.mall.cart.service.CartItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;

    private CartItemService cartItemService;

    @BeforeEach
    void setUp() {
        cartItemService = new CartItemService(cartItemMapper);
    }

    /**
     * 第一次加入商品时，应创建购物车记录。
     */
    @Test
    void addShouldInsertWhenItemDoesNotExist() {
        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(2L);
        request.setQuantity(1);

        cartItemService.add(100L, request);

        verify(cartItemMapper).insert(any(CartItem.class));
    }

    /**
     * 商品已在购物车中时，应累加数量并重新选中。
     */
    @Test
    void addShouldIncreaseQuantityWhenItemExists() {
        CartItem item = new CartItem();
        item.setUserId(100L);
        item.setProductId(2L);
        item.setQuantity(1);
        item.setChecked(0);
        item.setDeleted(0);

        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(item);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(2L);
        request.setQuantity(2);

        cartItemService.add(100L, request);

        assertEquals(3, item.getQuantity());
        assertEquals(1, item.getChecked());
        verify(cartItemMapper).updateById(item);
    }

    /**
     * 已逻辑删除的商品重新加入时，应从本次数量重新开始。
     */
    @Test
    void addShouldResetQuantityWhenItemWasDeleted() {
        CartItem item = new CartItem();
        item.setUserId(100L);
        item.setProductId(2L);
        item.setQuantity(3);
        item.setChecked(0);
        item.setDeleted(1);

        when(cartItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(item);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(2L);
        request.setQuantity(2);

        cartItemService.add(100L, request);

        assertEquals(2, item.getQuantity());
        assertEquals(1, item.getChecked());
        assertEquals(0, item.getDeleted());
        verify(cartItemMapper).updateById(item);
    }
}