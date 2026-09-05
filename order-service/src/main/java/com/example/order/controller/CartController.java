package com.example.order.controller;

import com.example.order.common.BaseResponse;
import com.example.order.dto.CartResponse;
import com.example.order.dto.OrderSummaryResponse;
import com.example.order.dto.UpdateCartItemRequest;
import com.example.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/me")
    public ResponseEntity<BaseResponse<CartResponse>> getMyCart() {
        return ResponseEntity.ok(BaseResponse.success(cartService.getMyCart()));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<BaseResponse<Void>> updateItemQuantity(
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
            ) {
        cartService.updateItemQuantity(cartItemId, request.getQuantity());

        return ResponseEntity.ok(BaseResponse.success(null, "Cart Item Updated Successfully"));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<BaseResponse<Void>> removeItem(
            @PathVariable UUID cartItemId
    ) {
        cartService.removeItem(cartItemId);

        return ResponseEntity.ok(BaseResponse.success(null, "Cart Item Removed Successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<OrderSummaryResponse>> getOrderSummary(
            @RequestParam(required = false) UUID discountId
    ) {
        return ResponseEntity.ok(BaseResponse.success(cartService.getOrderSummary(discountId)));
    }
}
