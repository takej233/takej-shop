package com.takej.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.takej.mall.product.entity.Product;
import com.takej.mall.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.product.dto.CreateProductRequest;
import com.takej.mall.common.BusinessException;
import com.takej.mall.common.ErrorCode;
import com.takej.mall.product.dto.UpdateProductRequest;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.takej.mall.product.dto.AdjustStockRequest;
import com.takej.mall.product.entity.ProductStockLog;
import com.takej.mall.product.mapper.ProductStockLogMapper;
import org.springframework.transaction.annotation.Transactional;
import com.takej.mall.product.enums.ProductStockSource;



/**
 * 商品业务服务。
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductStockLogMapper productStockLogMapper;

    public ProductService(
            ProductMapper productMapper,
            ProductStockLogMapper productStockLogMapper) {

        this.productMapper = productMapper;
        this.productStockLogMapper = productStockLogMapper;
    }
    /**
     * 查询商品列表，只返回上架且未删除的商品。
     */
    public List<Product> listOnSale() {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();

        query.eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0)
                .orderByDesc(Product::getSort)
                .orderByDesc(Product::getId);

        return productMapper.selectList(query);
    }


    /**
     * 批量查询上架且未删除的商品。
     * 供购物车、订单列表等场景一次性补全商品信息，避免前端发 N 次请求。
     * 传入的 ID 里如果有不存在、已下架或已删除的，会被静默忽略，
     * 调用方需要用返回结果里是否包含该 ID 来判断商品是否可购买。
     */
    public List<Product> listOnSaleByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();

        query.in(Product::getId, ids)
                .eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0);

        return productMapper.selectList(query);
    }


    /**
     * 根据商品 ID 查询已上架商品。
     */
    /**
     * 根据商品 ID 查询已上架商品。
     */
    public Product getOnSaleById(Long id) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();

        query.eq(Product::getId, id)
                .eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0);

        Product product = productMapper.selectOne(query);

        // 不存在、已下架或已删除，都视为商品不存在
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        return product;
    }

    /**
     * 新增商品。
     */
    public Long create(CreateProductRequest request) {
        Product product = new Product();

        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setMainImage(request.getMainImage());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        // 新增商品默认上架、未删除
        product.setStatus(1);
        product.setDeleted(0);

        // 没有填写排序值时使用 0
        product.setSort(
                request.getSort() == null ? 0 : request.getSort()
        );

        productMapper.insert(product);

        // 自增主键会回写到 product.id
        return product.getId();
    }


    /**
     * 修改商品基本信息。
     * 库存不属于本接口的职责，只能通过库存接口调整。
     */
    public void update(Long id, UpdateProductRequest request) {
        Product product = productMapper.selectById(id);

        // 商品不存在或已删除时，返回 404
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setMainImage(request.getMainImage());
        product.setPrice(request.getPrice());

        if (request.getSort() != null) {
            product.setSort(request.getSort());
        }

        productMapper.updateById(product);
    }


    /**
     * 逻辑删除商品。
     */
    public void delete(Long id) {
        Product product = productMapper.selectById(id);

        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 不删除数据库记录，只标记为已删除
        product.setDeleted(1);
        productMapper.updateById(product);
    }


    /**
     * 修改商品上下架状态。
     * 只影响商品能否被商城查询和下单，不改动库存。
     */
    public void updateStatus(Long productId, Integer status) {
        Product product = productMapper.selectById(productId);

        // 商品不存在或已逻辑删除。
        if (product == null
                || Integer.valueOf(1).equals(product.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        product.setStatus(status);
        productMapper.updateById(product);
    }


    /**
     * 原子扣减商品库存，并记录一条库存变动日志。
     * 只有商品存在、已上架、未删除且库存足够时才会扣减成功。
     */
    @Transactional(rollbackFor = Exception.class)          // ← 新增这一行
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);

        // 商品不存在、已下架或已逻辑删除。
        if (product == null
                || Integer.valueOf(1).equals(product.getDeleted())
                || !Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 库存不足时返回专用错误码。
        if (product.getStock() == null
                || product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        LambdaUpdateWrapper<Product> update =
                new LambdaUpdateWrapper<>();

        update.eq(Product::getId, productId)
                .eq(Product::getStatus, 1)
                .eq(Product::getDeleted, 0)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - {0}", quantity);

        int affectedRows = productMapper.update(null, update);

        // 查询后到更新前库存可能被其他请求扣完。
        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        // 同一事务内回读，得到本次扣减后的真实库存。   // ← 从这里开始新增
        int stockAfter = productMapper
                .selectById(productId)
                .getStock();
        int stockBefore = stockAfter + quantity;

        writeStockLog(
                productId,
                ProductStockSource.ORDER_DECREASE,
                -quantity,
                stockBefore,
                stockAfter,
                "下单扣减",
                null
        );
    }

    /**
     * 原子恢复商品库存，并记录一条库存变动日志。
     * 取消或关闭订单时，即使商品已经下架或逻辑删除，也必须归还已扣库存。
     */
    @Transactional(rollbackFor = Exception.class)          // ← 新增这一行
    public void increaseStock(Long productId, Integer quantity) {
        LambdaUpdateWrapper<Product> update =
                new LambdaUpdateWrapper<>();

        update.eq(Product::getId, productId)
                .setSql("stock = stock + {0}", quantity);

        int affectedRows = productMapper.update(null, update);

        if (affectedRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 同一事务内回读，得到本次归还后的真实库存。   // ← 从这里开始新增
        int stockAfter = productMapper
                .selectById(productId)
                .getStock();
        int stockBefore = stockAfter - quantity;

        writeStockLog(
                productId,
                ProductStockSource.ORDER_RESTORE,
                quantity,
                stockBefore,
                stockAfter,
                "订单归还",
                null
        );
    }




    /**
     * 写入一条库存变动日志。
     * 调用方应保证它与对应的库存修改处于同一个事务中。
     */
    private void writeStockLog(
            Long productId,
            int source,
            int changeQuantity,
            int stockBefore,
            int stockAfter,
            String reason,
            Long operatorId) {

        ProductStockLog stockLog = new ProductStockLog();
        stockLog.setProductId(productId);
        stockLog.setSource(source);
        stockLog.setChangeQuantity(changeQuantity);
        stockLog.setStockBefore(stockBefore);
        stockLog.setStockAfter(stockAfter);
        stockLog.setReason(reason);
        stockLog.setOperatorId(operatorId);

        productStockLogMapper.insert(stockLog);
    }


    /**
     * 人工调整商品库存，并记录一条调整日志。
     * 锁行、改库存、写日志在同一个事务中完成，任何一步失败整体回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long productId, AdjustStockRequest request) {

        // 变动为 0 没有意义，按参数错误处理。
        if (request.getChangeQuantity() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        // FOR UPDATE 锁住这一行，避免和其他扣减、恢复请求同时改库存。
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<>();
        query.eq(Product::getId, productId)
                .eq(Product::getDeleted, 0)
                .last("FOR UPDATE");

        Product product = productMapper.selectOne(query);

        // 商品不存在或已逻辑删除。
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        int stockBefore = product.getStock();
        int stockAfter = stockBefore + request.getChangeQuantity();

        // 调整后不允许出现负库存。
        if (stockAfter < 0) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        product.setStock(stockAfter);
        productMapper.updateById(product);

        // 库存改完立刻写审计记录，与库存修改同属一个事务。
        writeStockLog(
                productId,
                ProductStockSource.MANUAL_ADJUST,
                request.getChangeQuantity(),
                stockBefore,
                stockAfter,
                request.getReason(),
                request.getOperatorId()
        );
    }




    /**
     * 查询后台管理所需的全部商品。
     * 与面向商城用户的 listOnSale() 不同，这里包含已下架和逻辑删除商品。
     */
    public List<Product> listAllForAdmin() {
        LambdaQueryWrapper<Product> query =
                new LambdaQueryWrapper<>();

        // 后台按商品 ID 倒序查看，便于优先看到最近创建的商品。
        query.orderByDesc(Product::getId);

        return productMapper.selectList(query);
    }

    /**
     * 查询某个商品最近的库存变动记录。
     * source 为 null 时返回全部来源，否则只返回指定来源。
     * 按 id 倒序只取最近 50 条，避免一次把全部历史拉出来。
     */
    public List<ProductStockLog> listStockLogs(
            Long productId,
            Integer source) {

        LambdaQueryWrapper<ProductStockLog> query =
                new LambdaQueryWrapper<>();

        query.eq(ProductStockLog::getProductId, productId);

        // 只在传了来源时才加这个条件，不传就查全部。
        if (source != null) {
            query.eq(ProductStockLog::getSource, source);
        }

        query.orderByDesc(ProductStockLog::getId)
                .last("LIMIT 50");

        return productStockLogMapper.selectList(query);
    }



}
