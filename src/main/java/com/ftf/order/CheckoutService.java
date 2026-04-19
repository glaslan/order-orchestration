package com.ftf.order;

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
                item.put("productName", oi.getProductName());
                item.put("price", oi.getPrice());
                item.put("quantity", oi.getQuantity());
                return item;
            }).toList();

            Map<String, Object> deliveryPayload = new HashMap<>();
            deliveryPayload.put("orderId", manifest.getId());
            deliveryPayload.put("customerId", customer.getId());
            deliveryPayload.put("pickup", pickup);
            deliveryPayload.put("items", itemList);

            // TODO: confirm delivery team's endpoint path once they confirm
            restTemplate.postForObject(deliveryUrl + "/delivery/order", deliveryPayload, Void.class);
        } catch (Exception e) {
            // Delivery notification failing should not roll back a successful payment
            System.err.println("Delivery notification failed for order " + manifest.getId() + ": " + e.getMessage());
        }
    }
}
