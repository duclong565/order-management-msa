package com.example.auth.service;

import com.example.auth.common.ErrorCode;
import com.example.auth.dto.AddressResponse;
import com.example.auth.entity.Address;
import com.example.auth.exception.ApplicationException;
import com.example.auth.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public AddressResponse getById(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND));
        return toResponse(address);
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getUser().getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getZipCode()
        );
    }
}
