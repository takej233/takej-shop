package com.takej.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单明细数据访问接口。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 统计某商品还有多少笔待支付订单。
     * 这里直接写联表 SQL，比"先查明细、再按订单号逐个回查"更直接。
     */
    @Select("""
            SELECT COUNT(*)
            FROM order_item i
            JOIN order_info o ON i.order_id = o.id
            WHERE i.product_id = #{productId}
              AND o.status = 0
              AND o.deleted = 0
            """)
    int countPendingOrdersByProduct(@Param("productId") Long productId);
}