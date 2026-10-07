package com.takej.mall.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takej.mall.product.entity.Product;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 商品缓存服务。
 */
@Service
public class ProductCacheService {

    private static final String PRODUCT_LIST_KEY =
            "takej:product:list:on-sale";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductService productService;

    public ProductCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ProductService productService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.productService = productService;
    }

    /**
     * 优先读取 Redis，未命中时查询数据库并写入缓存。
     */
    public List<Product> listOnSale() {
        String cachedJson = redisTemplate.opsForValue()
                .get(PRODUCT_LIST_KEY);

        if (cachedJson != null) {
            try {
                return objectMapper.readValue(
                        cachedJson,
                        new TypeReference<List<Product>>() {
                        }
                );
            } catch (Exception exception) {
                // 缓存格式异常时删除，改为查询数据库
                redisTemplate.delete(PRODUCT_LIST_KEY);
            }
        }

        List<Product> products = productService.listOnSale();

        try {
            String json = objectMapper.writeValueAsString(products);

            // 设置 10 分钟过期时间，避免缓存永久不更新
            redisTemplate.opsForValue().set(
                    PRODUCT_LIST_KEY,
                    json,
                    Duration.ofMinutes(10)
            );
        } catch (Exception exception) {
            // 缓存失败不影响商品查询结果
        }

        return products;
    }


    /**
     * 查询商品详情：优先读取 Redis，未命中时查询数据库。
     */
    public Product getOnSaleById(Long id) {
        String cacheKey = "takej:product:detail:" + id;

        String cachedJson = redisTemplate.opsForValue().get(cacheKey);

        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, Product.class);
            } catch (Exception exception) {
                // 缓存格式异常，删除后重新查询数据库
                redisTemplate.delete(cacheKey);
            }
        }

        Product product = productService.getOnSaleById(id);

        try {
            String json = objectMapper.writeValueAsString(product);

            // 商品详情缓存 10 分钟
            redisTemplate.opsForValue().set(
                    cacheKey,
                    json,
                    Duration.ofMinutes(10)
            );
        } catch (Exception exception) {
            // 缓存失败不影响正常返回
        }

        return product;
    }


    /**
     * 清除商品列表缓存。
     */
    public void evictListCache() {
        redisTemplate.delete(PRODUCT_LIST_KEY);
    }

    /**
     * 清除商品列表和指定商品详情缓存。
     */
    public void evictProductCache(Long productId) {
        redisTemplate.delete(PRODUCT_LIST_KEY);
        redisTemplate.delete("takej:product:detail:" + productId);
    }


}