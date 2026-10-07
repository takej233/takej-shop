package com.takej.mall.cart.controller;

import com.takej.mall.cart.dto.AddCartItemRequest;
import com.takej.mall.cart.service.CartItemService;
import com.takej.mall.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takej.mall.cart.dto.UpdateCartItemRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.takej.mall.cart.entity.CartItem;
import org.springframework.web.bind.annotation.GetMapping;
import com.takej.mall.cart.dto.UpdateCartItemCheckedRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


/**
 * 购物车接口。
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartItemService cartItemService;

    public CartController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    /**
     * 将商品加入当前用户的购物车。
     */
    @PostMapping("/items")
    public Result<Void> addItem(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddCartItemRequest request) {

        cartItemService.add(
                Long.valueOf(userId),
                request
        );

        return Result.success();
    }

    /**
     * 修改购物车商品数量。
     */
    @PutMapping("/items/{productId}")
    public Result<Void> updateQuantity(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        cartItemService.updateQuantity(
                Long.valueOf(userId),
                productId,
                request
        );

        return Result.success();
    }

    /**
     * 查询当前登录用户的购物车。
     */
    @GetMapping("/items")
    public Result<List<CartItem>> listItems(
            @RequestHeader("X-User-Id") String userId) {

        return Result.success(
                cartItemService.listByUserId(Long.valueOf(userId))
        );
    }


    /**
     * 删除当前用户购物车中的商品。
     */
    @DeleteMapping("/items/{productId}")
    public Result<Void> deleteItem(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long productId) {

        cartItemService.delete(
                Long.valueOf(userId),
                productId
        );

        return Result.success();
    }

    /**
     * 删除当前用户所有已选中的购物车商品。
     */
    @DeleteMapping("/items/selected")
    public Result<Void> deleteCheckedItems(
            @RequestHeader("X-User-Id") String userId) {

        cartItemService.deleteCheckedItems(
                Long.valueOf(userId)
        );

        return Result.success();
    }

    /**
     * 修改当前用户购物车商品的选中状态。
     */
    @PutMapping("/items/{productId}/checked")
    public Result<Void> updateChecked(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemCheckedRequest request) {

        cartItemService.updateChecked(
                Long.valueOf(userId),
                productId,
                request
        );

        return Result.success();
    }





}