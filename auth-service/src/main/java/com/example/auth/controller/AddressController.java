package com.example.auth.controller;

import com.example.auth.common.BaseResponse;
import com.example.auth.dto.AddressResponse;
import com.example.auth.dto.CreateAddressRequest;
import com.example.auth.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ResponseEntity<BaseResponse<AddressResponse>> create(@Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(addressService.create(request)));
    }
}
