package com.example.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.order.common.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull
    private OrderStatus status;

    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String location;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 500)
    private String note;
}
