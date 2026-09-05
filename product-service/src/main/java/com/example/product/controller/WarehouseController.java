package com.example.product.controller;

import com.example.product.common.BaseResponse;
import com.example.product.dto.WarehouseResponse;
import com.example.product.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<WarehouseResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(warehouseService.getById(id)));
    }
}
