package com.takej.mall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车数据访问接口。
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}