package com.ftf.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.ftf.order.config.HelperFunctions;
import com.ftf.order.model.CartItem;
import com.ftf.order.model.CustomerInfo;
import com.ftf.order.model.DeliveryOrder;
import com.ftf.order.model.InventoryItem;
import com.ftf.order.model.OrderItem;
import com.ftf.order.model.OrderManifest;
import com.ftf.order.repository.CartItemRepository;
import com.ftf.order.repository.InventoryItemRepository;
import com.ftf.order.repository.OrderItemRepository;
import com.ftf.order.repository.OrderManifestRepository;

@Service
public class CheckoutService {

    private final CartItemRepository cartItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OrderManifestRepository orderManifestRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${teams.customer.url}")
    private String customerUrl;

    @Value("${teams.delivery.url}")
    private String deliveryUrl;

    public CheckoutService(CartItemRepository cartItemRepository,
                           InventoryItemRepository inventoryItemRepository,
                           OrderManifestRepository orderManifestRepository,
                           OrderItemRepository orderItemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.orderManifestRepository = orderManifestRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public OrderManifest checkout(CustomerInfo customer, String subscriptionId, boolean pickup) {
        List<CartItem> cartItems = cartItemRepository.findByCustomerId(customer.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Validate stock availability for every item before creating anything
        for (CartItem cartItem : cartItems) {
            InventoryItem inv = inventoryItemRepository.findById(cartItem.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("An item in your cart no longer exists"));
            if (!inv.isActive()) {
                throw new RuntimeException(inv.getName() + " is no longer available");
            }
            if (inv.getQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Not enough stock for " + inv.getName()
                        + " (requested " + cartItem.getQuantity() + ", available " + inv.getQuantity() + ")");
            }
        }

        // Create the order manifest record
        OrderManifest manifest = new OrderManifest();
        manifest.setCustomerId(customer.getId());
        manifest.setCustomerName(customer.getName());
        manifest.setCustomerEmail(customer.getEmail());
        manifest.setPickup(pickup);
        manifest.setStatus("PENDING");
        orderManifestRepository.save(manifest);

        // Snapshot line items and compute total
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            InventoryItem inv = inventoryItemRepository.findById(cartItem.getInventoryItemId()).get();
            OrderItem oi = new OrderItem();
            oi.setOrderManifestId(manifest.getId());
            oi.setInventoryItemId(inv.getId());
            oi.setProductName(inv.getName());
            oi.setPrice(inv.getPrice());
            oi.setQuantity(cartItem.getQuantity());
            orderItemRepository.save(oi);
            orderItems.add(oi);
            total = total.add(inv.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Send billing manifest to customer team
        manifest.setStatus("PAYMENT_SENT");
        manifest.setUpdatedAt(LocalDateTime.now());
        orderManifestRepository.save(manifest);

        try {
            Map<String, Object> billingPayload = new HashMap<>();
            billingPayload.put("subscriptionId", subscriptionId);
            billingPayload.put("orderId", manifest.getId());
            billingPayload.put("amount", total);

            @SuppressWarnings("unchecked")
            Map<String, Object> billingResponse = restTemplate.postForObject(
                    customerUrl + "/billing/manifest", billingPayload, Map.class);

            boolean paid = billingResponse != null && Boolean.TRUE.equals(billingResponse.get("success"));

            if (paid) {
                manifest.setStatus("PAID");
                manifest.setUpdatedAt(LocalDateTime.now());
                orderManifestRepository.save(manifest);

                sendToDelivery(manifest, customer, orderItems, pickup);
                HelperFunctions helper = new HelperFunctions();
                helper.SendSoldItems("http://134.122.40.121:5180/api/inventory_intelligence/inventory/sold_items", orderItems, inventoryItemRepository, restTemplate);

                manifest.setStatus("DELIVERED");
                manifest.setUpdatedAt(LocalDateTime.now());
                orderManifestRepository.save(manifest);

                // Cart cleared only on successful payment
                cartItemRepository.deleteByCustomerId(customer.getId());
            } else {
                String reason = billingResponse != null ? (String) billingResponse.get("reason") : "No response";
                manifest.setStatus("FAILED");
                manifest.setErrorMessage("Payment declined: " + reason);
                manifest.setUpdatedAt(LocalDateTime.now());
                orderManifestRepository.save(manifest);
            }

        } catch (Exception e) {
            manifest.setStatus("FAILED");
            manifest.setErrorMessage("Billing error: " + e.getMessage());
            manifest.setUpdatedAt(LocalDateTime.now());
            orderManifestRepository.save(manifest);
        }

        return manifest;
    }

    private void sendToDelivery(OrderManifest manifest, CustomerInfo customer,
                                List<OrderItem> orderItems, boolean pickup) {
        try {
            List<Map<String, Object>> itemList = orderItems.stream().map(oi -> {
                Map<String, Object> item = new HashMap<>();
                InventoryItem inventoryItem = inventoryItemRepository.findById(oi.getInventoryItemId()).orElse(null);
                
                item.put("name", oi.getProductName());
                item.put("price", oi.getPrice());
                item.put("quantity", oi.getQuantity());
                item.put("id", oi.getInventoryItemId());
                if (inventoryItem != null) {
                    item.put("category", inventoryItem.getCategoryName());
                } else {
                    item.put("category", "Unknown");
                }
                return item;
            }).toList();

            DeliveryOrder order = new DeliveryOrder(manifest.getId(), customer.getId(), LocalDateTime.now().toString(), pickup, itemList);

            restTemplate.postForObject(deliveryUrl + "/api/delivery-requests", order, Void.class);
        } catch (Exception e) {
            // Delivery notification failing should not roll back a successful payment
            System.err.println("Delivery notification failed for order " + manifest.getId() + ": " + e.getMessage());
        }
    }
}
