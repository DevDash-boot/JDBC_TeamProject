package com.tenco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class StoreInfo {
    private int storeId;
    private String storeName;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String storeTell;
    private String Location;

}
