package com.tenco.dto;

import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Order {
    private int orderId;
    private String paymentType;
    private int totalPrice;
    private Timestamp orderDate;
    private String status; // 추가: 주문 상태 (예: 'ORDERED', 'CANCELLED')

    @Builder.Default
    private List<OrderItem> itemList = new ArrayList<>();

    // 결제 타입 및 총액용 간단 생성자
    public Order(String paymentType, int totalPrice) {
        this.paymentType = paymentType;
        this.totalPrice = totalPrice;
    }

    // 기존 생성자 (status 없이 사용할 경우)
    public Order(int orderId, String paymentType, int totalPrice, Timestamp orderDate) {
        this.orderId = orderId;
        this.paymentType = paymentType;
        this.totalPrice = totalPrice;
        this.orderDate = orderDate;
    }

    // status 포함 생성자 (비어있던 내부 로직 채움)
    public Order(int orderId, String paymentType, int totalPrice, String status, Timestamp orderDate) {
        this.orderId = orderId;
        this.paymentType = paymentType;
        this.totalPrice = totalPrice;
        this.status = status;
        this.orderDate = orderDate;
    }
}