package com.tenco.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    private int orderItemId;    // 주문 ID
    private int orderId;        // 주문자 ID
    private int productId;      // 상품 ID
    private int quantity;       // 주문 수량
    private int orderPrice;     // 주문 당시 가격
    private String productName; // 상품명

    public OrderItem(int orderId, int productId, int quantity, int orderPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }

    public OrderItem(int productId, int quantity, int orderPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }
}
