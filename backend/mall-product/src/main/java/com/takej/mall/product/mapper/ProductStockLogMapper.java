package com.takej.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.product.entity.ProductStockLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存调整记录数据访问接口。
 */
@Mapper
public interface ProductStockLogMapper extends BaseMapper<ProductStockLog> {
}