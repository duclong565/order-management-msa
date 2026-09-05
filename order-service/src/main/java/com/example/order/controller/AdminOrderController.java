package com.example.order.controller;

import com.example.order.common.BaseResponse;
import com.example.order.common.OrderStatus;
import com.example.order.dto.AdminOrderListItemResponse;
import com.example.order.dto.BulkConfirmRequest;
import com.example.order.dto.BulkConfirmResponse;
import com.example.order.dto.OrderOperationsSummaryResponse;
import com.example.order.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<AdminOrderListItemResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AdminOrderListItemResponse> page = adminOrderService.getOrders(status, search, pageable);

        Map<String, Object> metadata = Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );

        return ResponseEntity.ok(BaseResponse.success(page.getContent(), metadata));
    }

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<OrderOperationsSummaryResponse>> getSummary() {
        return ResponseEntity.ok(BaseResponse.success(adminOrderService.getSummary()));
    }

    @PostMapping("/bulk-confirm")
    public ResponseEntity<BaseResponse<BulkConfirmResponse>> bulkConfirm(
            @Valid @RequestBody BulkConfirmRequest request
    ) {
        BulkConfirmResponse result = adminOrderService.bulkConfirm(request);
        return ResponseEntity.ok(BaseResponse.success(result,
                "Confirmed " + result.getConfirmedCount() + "/" + result.getRequestedCount() + " orders"));
    }
}
