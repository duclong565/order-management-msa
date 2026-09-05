package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarrierShareResponse {
    private String carrierName;
    private boolean inNetwork;
    private long orderCount;

    private BigDecimal percentage;
}
