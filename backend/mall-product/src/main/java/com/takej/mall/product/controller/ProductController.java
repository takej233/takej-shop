package com.takej.mall.product.controller;

import com.takej.mall.common.Result;
import com.takej.mall.product.entity.Product;
import com.takej.mall.product.service.ProductCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.takej.mall.product.service.ProductService;
import org.springframework.web.bind.annotation.PathVariable;
import com.takej.mall.product.dto.CreateProductRequest;
import com.takej.mall.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.takej.mall.product.dto.UpdateProductRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;


/**
 * 商品接口。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCacheService productCacheService;
    private final ProductService productService;

    public ProductController(
            ProductCacheService productCacheService,
            ProductService productService) {
        this.productCacheService = productCacheService;
        this.productService = productService;
    }

    /**
     * 查询已上架商品，优先从 Redis 读取。
     */
    @GetMapping
    public Result<List<Product>> list() {
        return Result.success(productCacheService.listOnSale());
    }


    /**
     * 查询商品详情，只返回上架且未删除的商品。
     */
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productCacheService.getOnSaleById(id));
    }


    /**
     * 批量查询商品，供购物车等场景一次性补全商品信息。
     * 只返回上架且未删除的商品；请求里的 ID 如果查不到会被忽略，
     * 调用方按返回结果里有没有对应 ID 来判断商品是否仍可购买。
     *
     * @param ids 商品 ID 列表，例如 /batch?ids=1,2,3
     */
    @GetMapping("/batch")
    public Result<List<Product>> batch(@RequestParam List<Long> ids) {
        return Result.success(productService.listOnSaleByIds(ids));
    }






}