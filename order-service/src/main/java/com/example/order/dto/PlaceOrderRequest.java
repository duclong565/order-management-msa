package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull
    private UUID recipientAddressId;
    @NotNull
    private UUID paymentMethodId;
    private UUID discountId;
}
