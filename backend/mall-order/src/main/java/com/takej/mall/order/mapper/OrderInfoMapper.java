package com.takej.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表数据访问接口。
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}