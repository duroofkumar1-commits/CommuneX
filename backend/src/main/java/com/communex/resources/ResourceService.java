
package com.communex.resources;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.communex.common.ActivityLogService;
import com.communex.resources.dto.QuantityAdjustmentRequest;
import com.communex.resources.dto.ResourceRequest;
import com.communex.resources.dto.ResourceResponse;
import com.communex.users.User;
import com.communex.users.UserRepository;

@Service
public class ResourceService {

    private final ResourceRepository repo;
    private final InventoryTransactionRepository txs;
    private final UserRepository users;
    private final ActivityLogService activity;

    public ResourceService(
            ResourceRepository repo,
            InventoryTransactionRepository txs,
            UserRepository users,
            ActivityLogService activity
    ) {
        this.repo = repo;
        this.txs = txs;
        this.users = users;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> search(String q, boolean low) {

        // Always pass a non-null String to the repository.
        String searchTerm = q == null ? "" : q.trim();

        return repo.search(searchTerm, low)
                .stream()
                .map(ResourceResponse::from)
                .toList();
    }

    @Transactional
    public ResourceResponse create(String email, ResourceRequest r) {

        ResourceItem x = new ResourceItem();

        x.setName(r.name().trim());
        x.setCategory(r.category().trim());
        x.setDescription(trim(r.description()));
        x.setLocation(trim(r.location()));
        x.setQuantity(r.quantity());
        x.setUnit(r.unit().trim());
        x.setLowStockThreshold(r.lowStockThreshold());

        x = repo.save(x);

        recordTransaction(
                x,
                email,
                r.quantity(),
                "Initial stock record"
        );

        activity.record(
                email,
                "RESOURCE_CREATED",
                "RESOURCE",
                x.getId(),
                x.getName()
        );

        return ResourceResponse.from(x);
    }

    @Transactional
    public ResourceResponse adjust(
            Long id,
            String email,
            QuantityAdjustmentRequest r
    ) {

        ResourceItem x = repo.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Resource not found"
                        )
                );

        BigDecimal next = x.getQuantity().add(r.delta());

        if (next.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inventory cannot become negative"
            );
        }

        x.setQuantity(next);
        x = repo.save(x);

        recordTransaction(
                x,
                email,
                r.delta(),
                r.reason().trim()
        );

        activity.record(
                email,
                "INVENTORY_ADJUSTED",
                "RESOURCE",
                id,
                r.reason().trim()
        );

        return ResourceResponse.from(x);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> history(Long id) {

        if (!repo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resource not found"
            );
        }

        return txs.findTop100ByResourceIdOrderByCreatedAtDesc(id)
                .stream()
                .map(t -> java.util.Map.<String, Object>of(
                        "id", t.getId(),
                        "delta", t.getDelta(),
                        "quantityAfter", t.getQuantityAfter(),
                        "reason", t.getReason(),
                        "updatedBy", t.getUpdatedBy().getName(),
                        "createdAt", t.getCreatedAt()
                ))
                .toList();
    }

    private void recordTransaction(
            ResourceItem item,
            String email,
            BigDecimal delta,
            String reason
    ) {

        User u = users.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        InventoryTransaction tx = new InventoryTransaction();

        tx.setResource(item);
        tx.setUpdatedBy(u);
        tx.setDelta(delta);
        tx.setQuantityAfter(item.getQuantity());
        tx.setReason(reason);

        txs.save(tx);
    }

    private String trim(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
