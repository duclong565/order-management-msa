package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.AddressResponse;
import com.example.auth.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AddressResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(BaseResponse.success(addressService.getById(id)));
    }
}
