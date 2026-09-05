package com.example.auth.service;

import com.example.auth.common.ErrorCode;
import com.example.auth.dto.AddressResponse;
import com.example.auth.dto.CreateAddressRequest;
import com.example.auth.entity.Address;
import com.example.auth.entity.User;
import com.example.auth.exception.ApplicationException;
import com.example.auth.repository.AddressRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public AddressResponse getById(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND));
        return toResponse(address);
    }

    @Transactional
    public AddressResponse create(CreateAddressRequest request) {
        UUID userId = currentUserProvider.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_NOT_FOUND));

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setZipCode(request.getZipCode());
        Address saved = addressRepository.save(address);

        return toResponse(saved);
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
