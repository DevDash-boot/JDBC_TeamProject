package com.tenco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data   @AllArgsConstructor     @NoArgsConstructor
@Builder
public class PurchaseDto {
    private int purchaseId;
    private int productId;
    private int qauntity;
    private int unitPrice;
    private int totlaPrice;


    private String name;

    public String toString3() {
        return "PurchaseDto{" +
                "name='" + name + '\'' +
                ", product_id=" + productId +
                ", qauntity=" + qauntity +
                ", unitPrice=" + unitPrice +
                '}';
    }



}
