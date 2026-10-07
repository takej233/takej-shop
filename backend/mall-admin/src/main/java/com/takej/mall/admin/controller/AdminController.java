package com.takej.mall.admin.controller;

import com.takej.mall.admin.dto.AdminIdentityResponse;
import com.takej.mall.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.takej.mall.admin.client.ProductAdminClient;
import com.takej.mall.admin.dto.AdminProductItem;

import java.util.List;
import com.takej.mall.admin.dto.AdminCreateProductRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.takej.mall.admin.dto.AdminUpdateProductRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import com.takej.mall.admin.dto.AdminAdjustStockRequest;
import com.takej.mall.admin.dto.AdminStockLogItem;
import com.takej.mall.admin.client.OrderAdminClient;
import com.takej.mall.admin.dto.AdminOrderItem;
import com.takej.mall.admin.dto.AdminOrderDetail;
import com.takej.mall.admin.service.AdminOperationConstants;
import com.takej.mall.admin.service.AdminOperationLogService;
import com.takej.mall.admin.entity.AdminOperationLog;
import com.takej.mall.admin.dto.AdminUpdateProductStatusRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.takej.mall.common.BusinessException;
import com.takej.mall.admin.service.AdminPermissionService;
import com.takej.mall.admin.annotation.RequirePermission;
import com.takej.mall.admin.service.AdminPermissionConstants;


/**
 * 后台管理基础接口。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {


    private final ProductAdminClient productAdminClient;
    private final OrderAdminClient orderAdminClient;
    private final AdminOperationLogService
            adminOperationLogService;
    private final AdminPermissionService adminPermissionService;

    public AdminController(
            ProductAdminClient productAdminClient,
            OrderAdminClient orderAdminClient,
            AdminOperationLogService adminOperationLogService,
            AdminPermissionService adminPermissionService) {

        this.productAdminClient = productAdminClient;
        this.orderAdminClient = orderAdminClient;
        this.adminOperationLogService = adminOperationLogService;
        this.adminPermissionService = adminPermissionService;
    }
    /**
     * 查看当前登录管理员身份。
     *
     * @param adminId   网关注入的管理员 ID
     * @param username  网关注入的管理员用户名
     * @return 当前管理员身份信息
     */
    @GetMapping("/me")
    public Result<AdminIdentityResponse> currentAdmin(
            @RequestHeader("X-Admin-Id") String adminId,
            @RequestHeader("X-Admin-Username") String username) {

        // 这里只信任网关注入的管理员请求头，不读取客户端普通用户请求头。
        AdminIdentityResponse response =
                new AdminIdentityResponse(
                        Long.valueOf(adminId),
                        username
                );

        return Result.success(response);
    }



    /**
     * 查询当前登录管理员拥有的权限码。
     * 前端可以用它决定哪些按钮该显示、哪些该禁用。
     */
    @GetMapping("/me/permissions")
    public Result<List<String>> myPermissions(
            @RequestHeader("X-Admin-Id") String adminId) {

        return Result.success(
                adminPermissionService.listPermissionCodes(
                        Long.valueOf(adminId)
                )
        );
    }

    /**
     * 查询后台商品列表。
     * 返回全部商品，包含已下架和逻辑删除商品。
     */
    @GetMapping("/products")
    public Result<List<AdminProductItem>> listProducts() {

        return Result.success(
                productAdminClient.listAllProducts()
        );
    }


    /**
     * 管理员新增商品。
     * 商品实际创建由商品服务负责，后台服务只负责管理员权限边界和请求转发。
     */
    @PostMapping("/products")
    @RequirePermission(AdminPermissionConstants.PRODUCT_CREATE)
    public Result<Long> createProduct(
            @RequestHeader("X-Admin-Id") String adminId,
            @Valid @RequestBody AdminCreateProductRequest request) {

        Long productId = productAdminClient.createProduct(request);

        // 业务成功之后再记日志，记的是"已经发生的事"。
        adminOperationLogService.record(
                Long.valueOf(adminId),
                AdminOperationConstants.CREATE_PRODUCT,
                AdminOperationConstants.TARGET_PRODUCT,
                productId,
                "新增商品：" + request.getName()
        );

        return Result.success(productId);
    }


    /**
     * 管理员修改商品基本信息。
     * 该接口不会修改商品库存。
     */
    @PutMapping("/products/{productId}")
    @RequirePermission(AdminPermissionConstants.PRODUCT_UPDATE)
    public Result<Void> updateProduct(
            @RequestHeader("X-Admin-Id") String adminId,
            @PathVariable Long productId,
            @Valid @RequestBody AdminUpdateProductRequest request) {

        productAdminClient.updateProduct(productId, request);

        adminOperationLogService.record(
                Long.valueOf(adminId),
                AdminOperationConstants.UPDATE_PRODUCT,
                AdminOperationConstants.TARGET_PRODUCT,
                productId,
                "修改商品：" + request.getName()
        );

        return Result.success();
    }




    /**
     * 管理员修改商品上下架状态。
     * 下架不影响库存，只影响商城前台能否看到和下单。
     */
    @PutMapping("/products/{productId}/status")
    @RequirePermission(AdminPermissionConstants.PRODUCT_STATUS)
    public Result<Void> updateProductStatus(
            @RequestHeader("X-Admin-Id") String adminId,
            @PathVariable Long productId,
            @Valid @RequestBody AdminUpdateProductStatusRequest request) {

        productAdminClient.updateProductStatus(productId, request);

        adminOperationLogService.record(
                Long.valueOf(adminId),
                AdminOperationConstants.UPDATE_PRODUCT_STATUS,
                AdminOperationConstants.TARGET_PRODUCT,
                productId,
                Integer.valueOf(1).equals(request.getStatus())
                        ? "商品上架"
                        : "商品下架"
        );

        return Result.success();
    }


    /**
     * 管理员逻辑删除商品。
     * 删除前会先确认这件商品没有未完成的订单；
     * 删除后商品不再出现在商城前台，但历史订单明细里保留了
     * 下单时的商品名称和价格快照，所以老订单不受影响。
     */


    @DeleteMapping("/products/{productId}")
    @RequirePermission(AdminPermissionConstants.PRODUCT_DELETE)
    public Result<Void> deleteProduct(
            @RequestHeader("X-Admin-Id") String adminId,
            @PathVariable Long productId) {

        // 还有待支付订单的商品不允许删除。
        int pendingCount = orderAdminClient
                .countPendingOrders(productId);

        if (pendingCount > 0) {
            throw new BusinessException(
                    409,
                    "该商品还有 " + pendingCount
                            + " 笔待支付订单，暂时不能删除"
            );
        }

        productAdminClient.deleteProduct(productId);

        adminOperationLogService.record(
                Long.valueOf(adminId),
                AdminOperationConstants.DELETE_PRODUCT,
                AdminOperationConstants.TARGET_PRODUCT,
                productId,
                "删除商品"
        );

        return Result.success();
    }


    /**
     * 管理员人工调整商品库存。
     * 操作人取自网关注入的管理员身份，不使用前端提交的操作人。
     */
    @PostMapping("/products/{productId}/adjust-stock")
    @RequirePermission(AdminPermissionConstants.STOCK_ADJUST)
    public Result<Void> adjustStock(
            @RequestHeader("X-Admin-Id") String adminId,
            @PathVariable Long productId,
            @Valid @RequestBody AdminAdjustStockRequest request) {

        productAdminClient.adjustStock(
                productId,
                Long.valueOf(adminId),
                request
        );

        // %+d 会让正数带上加号，例如 +5、-3。
        String detail = String.format(
                "调整库存 %+d，原因：%s",
                request.getChangeQuantity(),
                request.getReason() == null
                        ? "未填写"
                        : request.getReason()
        );

        adminOperationLogService.record(
                Long.valueOf(adminId),
                AdminOperationConstants.ADJUST_STOCK,
                AdminOperationConstants.TARGET_PRODUCT,
                productId,
                detail
        );

        return Result.success();
    }

    /**
     * 查询某个商品的库存变动记录，最新在前。
     * 可选参数 source：1人工调整，2下单扣减，3订单归还。
     */
    @GetMapping("/products/{productId}/stock-logs")
    public Result<List<AdminStockLogItem>> listStockLogs(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer source) {

        return Result.success(
                productAdminClient.listStockLogs(productId, source)
        );
    }

    /**
     * 查询全部订单，最新在前。
     */
    @GetMapping("/orders")
    public Result<List<AdminOrderItem>> listOrders() {

        return Result.success(
                orderAdminClient.listAllOrders()
        );
    }

    /**
     * 管理员关闭一笔待支付订单。
     * 操作人取自网关注入的管理员身份，关闭后会恢复订单占用的库存。
     */
    @PostMapping("/orders/{orderId}/close")
    @RequirePermission(AdminPermissionConstants.CLOSE_ORDER)
    public Result<Void> closeOrder(
            @RequestHeader("X-Admin-Id") String adminId,
            @PathVariable Long orderId) {

        orderAdminClient.closeOrder(
                orderId,
                Long.valueOf(adminId)
        );

        adminOperationLogService.record(
                Long.valueOf(adminId),
                AdminOperationConstants.CLOSE_ORDER,
                AdminOperationConstants.TARGET_ORDER,
                orderId,
                "关闭待支付订单"
        );

        return Result.success();
    }


    /**
     * 查询一笔订单的详情，包含商品明细。
     */
    @GetMapping("/orders/{orderId}")
    public Result<AdminOrderDetail> getOrderDetail(
            @PathVariable Long orderId) {

        return Result.success(
                orderAdminClient.getOrderDetail(orderId)
        );
    }

    /**
     * 查询最近的管理员操作日志，最新在前。
     */
    @GetMapping("/operation-logs")
    public Result<List<AdminOperationLog>> listOperationLogs() {

        return Result.success(
                adminOperationLogService.listRecentLogs()
        );
    }


}