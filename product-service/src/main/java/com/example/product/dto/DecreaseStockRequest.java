package com.example.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Trừ kho cho nhiều variant trong CÙNG một transaction - hoặc trừ hết, hoặc không trừ gì.
// Gọi từng variant một (mỗi call một transaction riêng) sẽ để lại trạng thái trừ nửa vời
// khi variant thứ n hết hàng.
@Getter
@Setter
@NoArgsConstructor
public class DecreaseStockRequest {

    @NotEmpty
    private List<@Valid DecreaseStockLine> items;
}
