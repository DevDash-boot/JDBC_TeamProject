package com.tenco.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderItemDTO {
    private int orderItemId;
    private int orderId;
    private int productId;
    private int quantity;
    private int orderPrice;

    public OrderItemDTO(int productId, int quantity, int orderPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }
}
