package com.takej.mall.order.enums;

/**
 * 订单状态定义。
 */
public enum OrderStatus {

    /** 用户已下单，但尚未付款。 */
    PENDING_PAYMENT(0),

    /** 用户已经完成付款。 */
    PAID(1),

    /** 用户在付款前取消订单。 */
    CANCELLED(2),


    /** 超过支付截止时间，系统自动关闭订单。 */
    EXPIRED(4),

    /** 用户确认收货，订单流程结束。 */
    COMPLETED(3);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}