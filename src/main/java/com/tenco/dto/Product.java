package com.tenco.dto;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder

public class Product {

    private int productId;
    private String productName;
    private int price;
    private String barcode;
    private LocalDate expirationDate;
    private int stock;
    private String category;
    private boolean status;

}
