package com.ftf.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

//Handles syncing current database with new information

@Service
public class InventorySyncService {

    // hardcoded API route for inv team
    private static final String INVENTORY_API_URL = "http://134.122.40.121:5180/api/inventory_intelligence/inventory/all_items";

    // HTTP client for calling APIs
    private final RestClient restClient;

    // JPA interface representing our database
    private final InventoryItemRepository itemRepository;

    // tracks sync events
    private final InventorySyncLogRepository syncLogRepository;

    // the method to call when we want to sync - client calls inv team API,
    // requires JPA interface for database & synclog
    public InventorySyncService(InventoryItemRepository itemRepository,
            InventorySyncLogRepository syncLogRepository) {
        this.restClient = RestClient.create();
        this.itemRepository = itemRepository;
        this.syncLogRepository = syncLogRepository;
    }

    // transactional means that the operation will be undone if not fully completed,
    // preventing corrupted data from system failure / partial completion
    @Transactional
    public InventorySyncLog syncInventory() {
        InventorySyncLog log = new InventorySyncLog();
        log.setSyncStartedAt(LocalDateTime.now());
        log.setStatus("IN_PROGRESS");
        syncLogRepository.save(log);

        try {
            // Spring parses JSON into a list of key-value pairs matching incoming structure
            List<Map<String, Object>> apiItems = restClient.get()
                    .uri(INVENTORY_API_URL).retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (apiItems == null) {
                throw new RuntimeException("Inventory API returned null response");
            }

            int inserted = 0;
            int updated = 0;

            // tracks all seen source IDs, deactivates database entries that arent in
            // new data (sold out or removed)
            Set<Long> seenSourceIds = new HashSet<>();

            for (Map<String, Object> apiItem : apiItems) {

                // pull values by name out of data, adds to JPA

                Long sourceId = ((Number) apiItem.get("id")).longValue();
                seenSourceIds.add(sourceId); // tracks sourceID for later
                String name = (String) apiItem.get("name");
                BigDecimal price = BigDecimal.valueOf(((Number) apiItem.get("price")).doubleValue());
                int quantity = ((Number) apiItem.get("quantity")).intValue();

                // pulls nested category object into id and name
                Long categoryId = null;
                String categoryName = null;
                Object categoryObj = apiItem.get("category");
                if (categoryObj instanceof Map<?, ?> category) {
                    categoryId = ((Number) category.get("id")).longValue();
                    categoryName = (String) category.get("name");
                }

                // check if current item already exists in our db
                Optional<InventoryItem> existing = itemRepository.findBySourceItemId(sourceId);

                // if current item already exists in our database, update all fields
                if (existing.isPresent()) {
                    InventoryItem item = existing.get();
                    boolean changed = false;

                    if (!item.getName().equals(name)) {
                        item.setName(name);
                        changed = true;
                    }
                    if (item.getPrice().compareTo(price) != 0) {
                        item.setPrice(price);
                        changed = true;
                    }
                    if (item.getQuantity() != quantity) {
                        item.setQuantity(quantity);
                        changed = true;
                    }
                    if (!java.util.Objects.equals(item.getCategoryId(), categoryId)) {
                        item.setCategoryId(categoryId);
                        changed = true;
                    }
                    if (!java.util.Objects.equals(item.getCategoryName(), categoryName)) {
                        item.setCategoryName(categoryName);
                        changed = true;
                    }
                    if (!item.isActive()) {
                        item.setActive(true);
                        changed = true;
                    }

                    if (changed) {
                        item.setLastSyncedAt(LocalDateTime.now());
                        itemRepository.save(item);
                        updated++;
                    }
                }

                // if doesnt exist in our database yet, new entry
                else {
                    InventoryItem item = new InventoryItem();
                    item.setSourceItemId(sourceId);
                    item.setName(name);
                    item.setPrice(price);
                    item.setQuantity(quantity);
                    item.setCategoryId(categoryId);
                    item.setCategoryName(categoryName);
                    item.setLastSyncedAt(LocalDateTime.now());
                    item.setActive(true);
                    itemRepository.save(item);
                    inserted++;
                }
            }

            // deactivate items that are no longer in the API response (sold out or removed)
            int deactivated = 0;
            List<InventoryItem> activeItems = itemRepository.findByActiveTrue();
            for (InventoryItem item : activeItems) {
                if (!seenSourceIds.contains(item.getSourceItemId())) {
                    item.setActive(false);
                    item.setLastSyncedAt(LocalDateTime.now());
                    itemRepository.save(item);
                    deactivated++;
                }
            }

            // sync log
            log.setRecordsProcessed(apiItems.size());
            log.setRecordsInserted(inserted);
            log.setRecordsUpdated(updated);
            log.setRecordsDeactivated(deactivated);
            log.setStatus("SUCCESS");
            log.setSyncFinishedAt(LocalDateTime.now());
            syncLogRepository.save(log);

            return log;

            // if sync fails, log is updated and error displayed
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setSyncFinishedAt(LocalDateTime.now());
            syncLogRepository.save(log);
            throw new RuntimeException("Inventory sync failed: " + e.getMessage(), e);
        }
    }
}
