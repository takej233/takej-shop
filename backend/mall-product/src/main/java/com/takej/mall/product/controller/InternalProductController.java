package com.takej.mall.product.controller;

import com.takej.mall.common.Result;
import com.takej.mall.product.dto.DecreaseStockRequest;
import com.takej.mall.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takej.mall.product.dto.IncreaseStockRequest;
import com.takej.mall.product.service.ProductCacheService;
import com.takej.mall.product.entity.Product;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import com.takej.mall.product.dto.CreateProductRequest;
import org.springframework.web.bind.annotation.PutMapping;
import com.takej.mall.product.dto.UpdateProductRequest;
import com.takej.mall.product.dto.AdjustStockRequest;
import com.takej.mall.product.entity.ProductStockLog;
import com.takej.mall.product.dto.UpdateProductStatusRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;



/**
 * 商品服务内部接口，只供其他微服务调用。
 */
@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductService productService;

    private final ProductCacheService productCacheService;

    public InternalProductController(
            ProductService productService,
            ProductCacheService productCacheService) {

        this.productService = productService;
        this.productCacheService = productCacheService;
    }





    /**
     * 查询全部商品，供后台管理服务内部调用。
     */
    @GetMapping("/all")
    public Result<List<Product>> listAllForAdmin() {
        return Result.success(productService.listAllForAdmin());
    }


    /**
     * 扣减指定商品的库存。
     */
    @PostMapping("/{productId}/decrease-stock")
    public Result<Void> decreaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody DecreaseStockRequest request) {

        productService.decreaseStock(productId, request.getQuantity());

        // 库存变化后清理商品列表和详情缓存。
        productCacheService.evictProductCache(productId);

        return Result.success();
    }

    /**
     * 恢复指定商品的库存。
     */
    @PostMapping("/{productId}/increase-stock")
    public Result<Void> increaseStock(
            @PathVariable Long productId,
            @Valid @RequestBody IncreaseStockRequest request) {

        productService.increaseStock(productId, request.getQuantity());

        // 库存变化后清理商品列表和详情缓存。
        productCacheService.evictProductCache(productId);

        return Result.success();
    }


    /**
     * 创建商品，供后台管理服务内部调用。
     */
    @PostMapping
    public Result<Long> createForAdmin(
            @Valid @RequestBody CreateProductRequest request) {

        Long productId = productService.create(request);

        // 新增商品后清理商品列表缓存。
        productCacheService.evictListCache();

        return Result.success(productId);
    }


    /**
     * 修改商品基本信息，供后台管理服务内部调用。
     * 该接口不会修改商品库存。
     */
    @PutMapping("/{productId}")
    public Result<Void> updateForAdmin(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {

        productService.update(productId, request);

        // 修改后清理商品列表和详情缓存。
        productCacheService.evictProductCache(productId);

        return Result.success();
    }


    /**
     * 修改商品上下架状态，供后台管理服务内部调用。
     */
    @PutMapping("/{productId}/status")
    public Result<Void> updateStatus(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductStatusRequest request) {

        productService.updateStatus(productId, request.getStatus());

        // 上下架会同时影响商品列表和详情，两个缓存都要清。
        productCacheService.evictProductCache(productId);

        return Result.success();
    }


    /**
     * 人工调整商品库存，供后台管理服务内部调用。
     * 该接口会同时写入一条库存调整日志。
     */
    @PostMapping("/{productId}/adjust-stock")
    public Result<Void> adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody AdjustStockRequest request) {

        productService.adjustStock(productId, request);

        // 库存变化后清理商品列表和详情缓存。
        productCacheService.evictProductCache(productId);

        return Result.success();
    }


    /**
     * 查询某个商品的库存变动记录，供后台管理服务内部调用。
     * source 可选，用于只看某一类来源。
     */
    @GetMapping("/{productId}/stock-logs")
    public Result<List<ProductStockLog>> listStockLogs(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer source) {

        return Result.success(
                productService.listStockLogs(productId, source)
        );
    }

    /**
     * 逻辑删除商品，供后台管理服务内部调用。
     * 只标记 deleted = 1，不删除数据库记录。
     */
    @DeleteMapping("/{productId}")
    public Result<Void> deleteForAdmin(@PathVariable Long productId) {

        productService.delete(productId);

        // 删除后清理商品列表和详情缓存。
        productCacheService.evictProductCache(productId);

        return Result.success();
    }

}
