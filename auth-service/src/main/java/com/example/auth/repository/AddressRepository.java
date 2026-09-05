package com.example.auth.repository;

import com.example.auth.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
}
