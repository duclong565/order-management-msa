package com.example.order.repository;

import com.example.order.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarrierRepository extends JpaRepository<Carrier, UUID> {
}
