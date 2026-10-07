package com.takej.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.takej.mall.common.BusinessException;
import com.takej.mall.product.entity.Product;
import com.takej.mall.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import com.takej.mall.product.mapper.ProductStockLogMapper;


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductStockLogMapper productStockLogMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productMapper,
                productStockLogMapper
        );
    }
    /**
     * 商品不存在时，应返回 404。
     */
    @Test
    void decreaseStockShouldReturnNotFoundWhenProductMissing() {
        when(productMapper.selectById(999999L))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.decreaseStock(999999L, 1)
        );

        assertEquals(404, exception.getCode());
    }

    /**
     * 库存不足时，应返回 409。
     */
    @Test
    void decreaseStockShouldReturnStockNotEnough() {
        Product product = new Product();
        product.setId(2L);
        product.setStatus(1);
        product.setDeleted(0);
        product.setStock(1);

        when(productMapper.selectById(2L))
                .thenReturn(product);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.decreaseStock(2L, 2)
        );

        assertEquals(409, exception.getCode());
    }

    /**
     * 库存足够且数据库更新成功时，不应抛出异常。
     */
    @Test
    void decreaseStockShouldSucceedWhenStockEnough() {
        Product product = new Product();
        product.setId(2L);
        product.setStatus(1);
        product.setDeleted(0);
        product.setStock(5);

        when(productMapper.selectById(2L))
                .thenReturn(product);

        when(productMapper.update(
                isNull(),
                any(LambdaUpdateWrapper.class)
        )).thenReturn(1);

        assertDoesNotThrow(
                () -> productService.decreaseStock(2L, 1)
        );
    }


    @Test
    void increaseStockShouldSucceedWhenProductExists() {
        Product product = new Product();
        product.setId(2L);
        product.setStock(5);

        when(productMapper.selectById(2L))
                .thenReturn(product);

        when(productMapper.update(
                isNull(),
                any(LambdaUpdateWrapper.class)
        )).thenReturn(1);

        assertDoesNotThrow(
                () -> productService.increaseStock(2L, 1)
        );
    }
    /**
     * 恢复库存时商品不存在，应返回 404。
     */
    @Test
    void increaseStockShouldReturnNotFoundWhenProductMissing() {
        when(productMapper.update(
                isNull(),
                any(LambdaUpdateWrapper.class)
        )).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.increaseStock(999999L, 1)
        );

        assertEquals(404, exception.getCode());
    }
}