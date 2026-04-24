package com.ftf.order.service;

import com.ftf.order.model.InventoryItem;
import com.ftf.order.model.InventorySyncLog;
import com.ftf.order.repository.InventoryItemRepository;
import com.ftf.order.repository.InventorySyncLogRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

//Handles syncing current database with new information

@Service
public class InventorySyncService {

    private final RestClient restClient;
    private final InventoryItemRepository itemRepository;
    private final InventorySyncLogRepository syncLogRepository;
    private final String allItemsUrl;

    public InventorySyncService(InventoryItemRepository itemRepository,
            InventorySyncLogRepository syncLogRepository,
            @Value("${teams.inventory.base-url}") String baseUrl,
            @Value("${teams.inventory.all-items-path}") String allItemsPath) {
        this.restClient = RestClient.create();
        this.itemRepository = itemRepository;
        this.syncLogRepository = syncLogRepository;
        this.allItemsUrl = baseUrl + allItemsPath;
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
            List<Map<String, Object>> apiItems = restClient.get()
                    .uri(allItemsUrl).retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (apiItems == null) {
                throw new RuntimeException("Inventory API returned null response");
            }

            int inserted = 0;
            int updated = 0;

            // track seen names so we can deactivate rows that dropped out of the feed
            Set<String> seenNames = new HashSet<>();

            for (Map<String, Object> apiItem : apiItems) {

                String name = (String) apiItem.get("product_name");
                if (name == null) continue;
                seenNames.add(name);

                BigDecimal price = toBigDecimal(apiItem.get("price_per_unit"));
                int quantity = toInt(apiItem.get("total_stored_quantity"));
                LocalDateTime lastStockDate = toLocalDateTime(apiItem.get("last_stock_date"));

                // nested category object
                Long categoryId = null;
                String categoryName = null;
                Long parentCategoryId = null;
                String parentCategoryName = null;
                Integer categoryLevel = null;
                Object categoryObj = apiItem.get("category");
                if (categoryObj instanceof Map<?, ?> category) {
                    categoryId = toLong(category.get("category_id"));
                    categoryName = (String) category.get("category_name");
                    parentCategoryId = toLong(category.get("parent_category_id"));
                    parentCategoryName = (String) category.get("parent_category_name");
                    categoryLevel = toInteger(category.get("level_of_category"));
                }
                // top-level parent_category_name overrides nested if provided
                Object topParentName = apiItem.get("parent_category_name");
                if (topParentName instanceof String s) {
                    parentCategoryName = s;
                }

                Optional<InventoryItem> existing = itemRepository.findByName(name);

                if (existing.isPresent()) {
                    InventoryItem item = existing.get();
                    boolean changed = false;

                    if (item.getPrice().compareTo(price) != 0) {
                        item.setPrice(price);
                        changed = true;
                    }
                    if (item.getQuantity() != quantity) {
                        item.setQuantity(quantity);
                        changed = true;
                    }
                    if (!Objects.equals(item.getCategoryId(), categoryId)) {
                        item.setCategoryId(categoryId);
                        changed = true;
                    }
                    if (!Objects.equals(item.getCategoryName(), categoryName)) {
                        item.setCategoryName(categoryName);
                        changed = true;
                    }
                    if (!Objects.equals(item.getParentCategoryId(), parentCategoryId)) {
                        item.setParentCategoryId(parentCategoryId);
                        changed = true;
                    }
                    if (!Objects.equals(item.getParentCategoryName(), parentCategoryName)) {
                        item.setParentCategoryName(parentCategoryName);
                        changed = true;
                    }
                    if (!Objects.equals(item.getCategoryLevel(), categoryLevel)) {
                        item.setCategoryLevel(categoryLevel);
                        changed = true;
                    }
                    if (!Objects.equals(item.getLastStockDate(), lastStockDate)) {
                        item.setLastStockDate(lastStockDate);
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
                } else {
                    InventoryItem item = new InventoryItem();
                    item.setName(name);
                    item.setPrice(price);
                    item.setQuantity(quantity);
                    item.setCategoryId(categoryId);
                    item.setCategoryName(categoryName);
                    item.setParentCategoryId(parentCategoryId);
                    item.setParentCategoryName(parentCategoryName);
                    item.setCategoryLevel(categoryLevel);
                    item.setLastStockDate(lastStockDate);
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
                if (!seenNames.contains(item.getName())) {
                    item.setActive(false);
                    item.setLastSyncedAt(LocalDateTime.now());
                    itemRepository.save(item);
                    deactivated++;
                }
            }

            log.setRecordsProcessed(apiItems.size());
            log.setRecordsInserted(inserted);
            log.setRecordsUpdated(updated);
            log.setRecordsDeactivated(deactivated);
            log.setStatus("SUCCESS");
            log.setSyncFinishedAt(LocalDateTime.now());
            syncLogRepository.save(log);

            return log;

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setSyncFinishedAt(LocalDateTime.now());
            syncLogRepository.save(log);
            throw new RuntimeException("Inventory sync failed: " + e.getMessage(), e);
        }
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    private static Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.valueOf(v.toString());
    }

    private static LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        try {
            return LocalDateTime.parse(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
