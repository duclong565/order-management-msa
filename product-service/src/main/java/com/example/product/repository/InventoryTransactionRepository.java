package com.example.product.repository;

import com.example.product.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
}
