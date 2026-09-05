package com.example.product.service;

import com.example.product.common.ErrorCode;
import com.example.product.dto.CreateWarehouseRequest;
import com.example.product.dto.WarehouseResponse;
import com.example.product.entity.Warehouse;
import com.example.product.exception.ApplicationException;
import com.example.product.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public WarehouseResponse getById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.WAREHOUSE_NOT_FOUND));
        return new WarehouseResponse(warehouse.getId(), warehouse.getName(), warehouse.getDescription());
    }

    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setDescription(request.getDescription());
        Warehouse saved = warehouseRepository.save(warehouse);
        return new WarehouseResponse(saved.getId(), saved.getName(), saved.getDescription());
    }
}
