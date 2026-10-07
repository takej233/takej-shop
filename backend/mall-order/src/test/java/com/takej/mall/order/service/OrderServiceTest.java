package com.takej.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.takej.mall.order.client.CartClient;
import com.takej.mall.order.client.ProductClient;
import com.takej.mall.order.entity.OrderItem;
import com.takej.mall.order.mapper.OrderInfoMapper;
import com.takej.mall.order.mapper.OrderItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.takej.mall.order.entity.OrderInfo;
import java.math.BigDecimal;
import com.takej.mall.order.dto.CartItemSnapshot;
import com.takej.mall.order.dto.ProductSnapshot;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductClient productClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // 单元测试没有启动完整 MyBatis，需要手动初始化实体元数据。
        if (TableInfoHelper.getTableInfo(OrderInfo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(
                            new MybatisConfiguration(),
                            ""
                    ),
                    OrderInfo.class
            );
        }

        if (TableInfoHelper.getTableInfo(OrderItem.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(
                            new MybatisConfiguration(),
                            ""
                    ),
                    OrderItem.class
            );
        }

        orderService = new OrderService(
                productClient,
                cartClient,
                orderInfoMapper,
                orderItemMapper
        );
    }

    /**
     * 抢占过期订单成功时，应恢复每条订单明细的库存。
     */
    @Test
    void expireOrderShouldRestoreStockWhenClaimSucceeds() {
        OrderItem item = new OrderItem();
        item.setOrderId(100L);
        item.setProductId(2L);
        item.setQuantity(3);

        when(orderInfoMapper.update(
                isNull(),
                any(LambdaUpdateWrapper.class)
        )).thenReturn(1);

        when(orderItemMapper.selectList(
                any(LambdaQueryWrapper.class)
        )).thenReturn(List.of(item));

        boolean result = orderService.expireOrder(100L);

        assertTrue(result);
        verify(productClient).increaseStock(2L, 3);
    }

    /**
     * 订单已经被其他操作处理时，不应再次恢复库存。
     */
    @Test
    void expireOrderShouldNotRestoreStockWhenClaimFails() {
        when(orderInfoMapper.update(
                isNull(),
                any(LambdaUpdateWrapper.class)
        )).thenReturn(0);

        boolean result = orderService.expireOrder(100L);

        assertFalse(result);
        verifyNoInteractions(productClient, orderItemMapper);
    }



    /**
     * 清理购物车失败时，应归还已经扣减的库存。
     */
    @Test
    void createOrderFromCartShouldRestoreStockWhenCartCleanupFails() {
        // 购物车里有一件已选中的商品：商品 2，数量 3
        CartItemSnapshot cartItem = new CartItemSnapshot();
        cartItem.setProductId(2L);
        cartItem.setQuantity(3);
        cartItem.setChecked(1);

        when(cartClient.listItems(1L)).thenReturn(List.of(cartItem));

        ProductSnapshot product = new ProductSnapshot();
        product.setId(2L);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("10.00"));

        when(productClient.getOnSaleProduct(2L)).thenReturn(product);

        // 模拟 MyBatis-Plus 插入后回填主键
        when(orderInfoMapper.insert(any(OrderInfo.class)))
                .thenAnswer(invocation -> {
                    OrderInfo order = invocation.getArgument(0);
                    order.setId(100L);
                    return 1;
                });

        // restoreStock 会按 orderId 查询订单明细
        OrderItem item = new OrderItem();
        item.setOrderId(100L);
        item.setProductId(2L);
        item.setQuantity(3);

        when(orderItemMapper.selectList(
                any(LambdaQueryWrapper.class)
        )).thenReturn(List.of(item));

        // 让清理购物车这一步失败
        doThrow(new RuntimeException("cart service down"))
                .when(cartClient)
                .deleteCheckedItems(1L);

        assertThrows(
                RuntimeException.class,
                () -> orderService.createOrderFromCart(1L)
        );

        // 关键断言：库存被加回去
        verify(productClient).increaseStock(2L, 3);
    }
}