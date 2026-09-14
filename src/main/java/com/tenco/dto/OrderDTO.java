package com.tenco.dto;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderDTO {
    private int orderId;
    private String paymentType;
    private int totalPrice;
    private Timestamp orderDate;

    public OrderDTO(String paymentType, int totalPrice) {
        this.paymentType = paymentType;
        this.totalPrice = totalPrice;
    }
}


