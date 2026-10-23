package com.communex.resources;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    // Retrieve the latest 100 transactions for a resource
    List<InventoryTransaction> findTop100ByResourceIdOrderByCreatedAtDesc(
            Long resourceId
    );
}