package com.tenco.dto;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
// status 주문상태 추가
public class OrderDTO {
    private int orderId;
    private String paymentType;
    private int totalPrice;
    private String status;
    private Timestamp orderDate;

    public OrderDTO(String paymentType, int totalPrice) {
        this.paymentType = paymentType;
        this.totalPrice = totalPrice;
        this.status = "ORDERED"; // 기본값
    }
}


