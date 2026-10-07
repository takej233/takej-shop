package com.takej.mall.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.cart.dto.AddCartItemRequest;
import com.takej.mall.cart.entity.CartItem;
import com.takej.mall.cart.mapper.CartItemMapper;
import org.springframework.stereotype.Service;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.cart.dto.UpdateCartItemRequest;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.takej.mall.cart.dto.UpdateCartItemCheckedRequest;
import com.takej.mall.cart.client.ProductClient;


/**
 * 购物车业务服务。
 */
@Service
public class CartItemService {

    private final CartItemMapper cartItemMapper;

    /** 商品服务客户端：加购前确认商品可购买。 */
    private final ProductClient productClient;

    public CartItemService(CartItemMapper cartItemMapper,
                           ProductClient productClient) {
        this.cartItemMapper = cartItemMapper;
        this.productClient = productClient;
    }


    /**
     * 将商品加入指定用户的购物车。
     */
    public void add(Long userId, AddCartItemRequest request) {

        // 先确认商品仍然存在且上架。
        // 否则购物车里会留下指向不存在商品的记录，
        // 用户结算时整张订单都会失败。
        productClient.requireOnSale(request.getProductId());
        LambdaQueryWrapper<CartItem> query = new LambdaQueryWrapper<>();

        query.eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, request.getProductId());

        CartItem item = cartItemMapper.selectOne(query);

        if (item == null) {
            // 第一次加入该商品，创建购物车记录
            item = new CartItem();
            item.setUserId(userId);
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity());
            item.setChecked(1);
            item.setDeleted(0);

            cartItemMapper.insert(item);
            return;
        }

        // 商品已在购物车中：再次加入时累加数量，并重新选中。
        if (Integer.valueOf(0).equals(item.getDeleted())) {
            item.setQuantity(
                    item.getQuantity() + request.getQuantity()
            );
            item.setChecked(1);
            cartItemMapper.updateById(item);
            return;
        }

        // 商品曾被逻辑删除：重新加入购物车时，从本次数量重新开始。
        if (Integer.valueOf(1).equals(item.getDeleted())) {
            item.setQuantity(request.getQuantity());
            item.setChecked(1);
            item.setDeleted(0);
            cartItemMapper.updateById(item);
            return;
        }
    }


    /**
     * 修改当前用户购物车中的商品数量。
     */
    public void updateQuantity(
            Long userId,
            Long productId,
            UpdateCartItemRequest request) {

        LambdaQueryWrapper<CartItem> query = new LambdaQueryWrapper<>();

        query.eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId)
                .eq(CartItem::getDeleted, 0);

        CartItem item = cartItemMapper.selectOne(query);

        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        item.setQuantity(request.getQuantity());
        cartItemMapper.updateById(item);
    }



    /**
     * 修改当前用户购物车商品的选中状态。
     */
    public void updateChecked(
            Long userId,
            Long productId,
            UpdateCartItemCheckedRequest request) {

        LambdaQueryWrapper<CartItem> query = new LambdaQueryWrapper<>();

        query.eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId)
                .eq(CartItem::getDeleted, 0);

        CartItem item = cartItemMapper.selectOne(query);

        // 商品不在当前用户的购物车中。
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        item.setChecked(request.getChecked());
        cartItemMapper.updateById(item);
    }


    /**
     * 查询当前用户未删除的购物车项。
     */
    public List<CartItem> listByUserId(Long userId) {
        LambdaQueryWrapper<CartItem> query = new LambdaQueryWrapper<>();

        query.eq(CartItem::getUserId, userId)
                .eq(CartItem::getDeleted, 0)
                .orderByDesc(CartItem::getUpdateTime);

        return cartItemMapper.selectList(query);
    }


    /**
     * 删除当前用户购物车中的商品。
     */
    public void delete(Long userId, Long productId) {
        LambdaQueryWrapper<CartItem> query = new LambdaQueryWrapper<>();

        query.eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId)
                .eq(CartItem::getDeleted, 0);

        CartItem item = cartItemMapper.selectOne(query);

        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 逻辑删除，不真正删除数据库记录
        item.setDeleted(1);
        cartItemMapper.updateById(item);
    }


    /**
     * 逻辑删除当前用户所有已选中的购物车商品。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCheckedItems(Long userId) {
        LambdaUpdateWrapper<CartItem> update = new LambdaUpdateWrapper<>();

        update.eq(CartItem::getUserId, userId)
                .eq(CartItem::getChecked, 1)
                .eq(CartItem::getDeleted, 0)
                .set(CartItem::getDeleted, 1);

        cartItemMapper.update(null, update);
    }



}