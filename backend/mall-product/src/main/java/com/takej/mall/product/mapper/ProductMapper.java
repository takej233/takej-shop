package com.takej.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.takej.mall.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品数据访问接口。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

}