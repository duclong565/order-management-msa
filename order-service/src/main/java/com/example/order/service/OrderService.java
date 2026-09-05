package com.example.order.service;

import com.example.order.client.AddressResponse;
import com.example.order.client.DiscountResponse;
import com.example.order.client.ProductClient;
import com.example.order.client.ProductVariantResponse;
import com.example.order.client.UserClient;
import com.example.order.client.UserResponse;
import com.example.order.dto.*;
import com.example.order.entity.*;
import com.example.order.common.ErrorCode;
import com.example.order.common.OrderStatus;
import com.example.order.common.PaymentStatus;
import com.example.order.common.UserRole;
import com.example.order.exception.ApplicationException;
import com.example.order.pricing.PricingCalculator;
import com.example.order.repository.*;
import com.example.order.security.CurrentUserProvider;
import com.example.order.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingCalculator  pricingCalculator;
    private final TrackingLogRepository trackingLogRepository;
    private final CurrentUserProvider currentUserProvider;

    // gom hết productVariantId cần thiết, gọi product-service đúng 1 lần,
    // dựng Map để tra cứu O(1) - không gọi ProductClient bên trong vòng lặp.
    private Map<UUID, ProductVariantResponse> fetchVariantsByCartItems(List<CartItem> items) {
        List<UUID> variantIds = items.stream().map(CartItem::getProductVariantId).distinct().toList();
        return productClient.getVariantsByIds(variantIds).stream()
                .collect(Collectors.toMap(ProductVariantResponse::getVariantId, Function.identity()));
    }

    private Map<UUID, ProductVariantResponse> fetchVariantsByOrderItems(List<OrderItem> items) {
        List<UUID> variantIds = items.stream().map(OrderItem::getProductVariantId).distinct().toList();
        return productClient.getVariantsByIds(variantIds).stream()
                .collect(Collectors.toMap(ProductVariantResponse::getVariantId, Function.identity()));
    }

    // tương tự - gom userId của mọi tracking log trong đơn, gọi auth-service đúng 1 lần.
    private Map<UUID, UserResponse> fetchUsersByTrackingLogs(List<TrackingLog> logs) {
        List<UUID> userIds = logs.stream()
                .map(TrackingLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserResponse::getId, Function.identity()));
    }

    private void decreaseStock(List<CartItem> items) {
        for (CartItem item : items) {
            productClient.decreaseStock(item.getProductVariantId(), item.getQuantity());
        }
    }

    private OrderItem toOrderItem(Order order, CartItem cartItem, Map<UUID, ProductVariantResponse> variants) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductVariantId(cartItem.getProductVariantId());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setUnitPrice(variants.get(cartItem.getProductVariantId()).getPrice());
        return orderItem;
    }

    private OrderResponse toResponse(Order order,
                                     List<OrderItem> orderItems,
                                     List<TrackingLog> trackingLogs) {
        Map<UUID, ProductVariantResponse> variants = fetchVariantsByOrderItems(orderItems);
        Map<UUID, UserResponse> actors = fetchUsersByTrackingLogs(trackingLogs);

        OrderResponse response = new OrderResponse();

        response.setOrderId(order.getId());
        response.setOrderCode(order.getOrderCode());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setItems(orderItems.stream().map(item -> toItemResponse(item, variants)).toList());

        response.setSubtotalPrice(order.getSubtotalPrice());
        response.setDiscountId(order.getDiscountId());
        response.setDiscountValue(order.getDiscountValue());
        response.setShippingFee(order.getShippingFee());
        response.setTotalPrice(order.getTotalPrice());

        response.setRecipientAddress(order.getRecipientAddressSnapshot());
        response.setRecipient(order.getRecipientAddressId() == null
                ? null
                : toRecipient(userClient.getAddressById(order.getRecipientAddressId())));

        PaymentMethod paymentMethod = order.getPaymentMethod();
        response.setPaymentMethodName(paymentMethod != null ? paymentMethod.getName() : null);
        response.setPaymentLast4(order.getPaymentLast4());

        response.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        Carrier carrier = order.getCarrier();
        response.setCarrierName(carrier != null ? carrier.getName() : null);
        response.setCarrierDescription(carrier != null ? carrier.getDescription() : null);

        response.setTimeline(trackingLogs.stream().map(log -> toTrackingEvent(log, actors)).toList());
        response.setCurrentLocation(latestLocation(trackingLogs));

        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    private OrderItemResponse toItemResponse(OrderItem orderItem, Map<UUID, ProductVariantResponse> variants) {
        ProductVariantResponse variant = variants.get(orderItem.getProductVariantId());
        BigDecimal lineTotal = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductVariantId(),
                variant != null ? variant.getProductName() : null,
                variant != null ? variant.getVariantName() : null,
                variant != null ? variant.getSku() : null,
                variant != null ? variant.getImageUrl() : null,
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                lineTotal
        );
    }

    private RecipientAddressResponse toRecipient(AddressResponse address) {
        if (address == null) {
            return null;
        }
        return new RecipientAddressResponse(
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

    private OrderTrackingEventResponse toTrackingEvent(TrackingLog log, Map<UUID, UserResponse> actors) {
        UserResponse actor = log.getUserId() != null ? actors.get(log.getUserId()) : null;

        return new OrderTrackingEventResponse(
                log.getId(),

                log.getTitle() != null ? log.getTitle() : log.getStatus().name(),
                log.getNote(),
                log.getStatus(),
                log.getLocation(),
                log.getLatitude(),
                log.getLongitude(),
                actor != null ? actor.getUsername() : null,
                actor != null ? actor.getRole() : null,
                log.getCreatedAt()
        );
    }

    private String latestLocation(List<TrackingLog> trackingLogs) {
        return trackingLogs.stream()
                .filter(log -> log.getLocation() != null && !log.getLocation().isBlank())
                .reduce((first, second) -> second)
                .map(TrackingLog::getLocation)
                .orElse(null);
    }

    private String formatAddress(AddressResponse address) {
        return Stream.of(address.getLine1(), address.getLine2(), address.getCity(),
                address.getState(), address.getZipCode(), address.getCountry())
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        UUID userId = currentUserProvider.getUserId();

        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.CART_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApplicationException(ErrorCode.CART_EMPTY);
        }

        AddressResponse address = userClient.getAddressById(request.getRecipientAddressId());
        if (!Objects.equals(address.getUserId(), userId)) {
            throw new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        decreaseStock(items);

        Map<UUID, ProductVariantResponse> variants = fetchVariantsByCartItems(items);
        BigDecimal subtotal = items.stream()
                .map(item -> variants.get(item.getProductVariantId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DiscountResponse discount = null;
        if (request.getDiscountId() != null) {
            discount = productClient.getDiscountById(request.getDiscountId());
            pricingCalculator.validateDiscountActive(discount);
        }

        BigDecimal discountAmount = pricingCalculator.calculateDiscountAmount(discount, subtotal);
        BigDecimal shippingFee = pricingCalculator.getShippingFee();
        BigDecimal totalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        Order order = new Order();
        order.setOrderCode("ORD-" + orderRepository.nextOrderCodeSequence());
        order.setUserId(cart.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setDiscountId(discount != null ? discount.getId() : null);
        order.setDiscountValue(discountAmount);
        order.setSubtotalPrice(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(paymentMethod);
        order.setRecipientAddressId(address.getId());
        order.setRecipientAddressSnapshot(formatAddress(address));
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = items.stream()
                .map(item -> toOrderItem(savedOrder, item, variants))
                .toList();
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAll(items);
        cart.setDiscountId(null);
        cartRepository.save(cart);

        TrackingLog placedLog = new TrackingLog();
        placedLog.setOrder(savedOrder);
        placedLog.setUserId(cart.getUserId());
        placedLog.setStatus(OrderStatus.PENDING);
        placedLog.setTitle("Order Placed");
        placedLog.setNote("Digital order confirmed and payment verified.");
        trackingLogRepository.save(placedLog);

        return toResponse(savedOrder, orderItems, List.of(placedLog));
    }

    @Transactional(readOnly = true)
    public List<OrderListItemResponse> getMyOrders() {
        UUID userId = currentUserProvider.getUserId();

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(o -> new OrderListItemResponse(
                        o.getId(),
                        o.getStatus(),
                        o.getPaymentStatus(),
                        o.getTotalPrice(),
                        o.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(UUID orderId) {
        CustomUserDetails currentUser = currentUserProvider.getPrincipal();

        Order order = (currentUser.role() == UserRole.ADMIN
                ? orderRepository.findDetailById(orderId)
                : orderRepository.findDetailByIdAndUserId(orderId, currentUser.userId()))
                .orElseThrow(() -> new ApplicationException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<TrackingLog> trackingLogs = trackingLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        return toResponse(order, items, trackingLogs);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus current = order.getStatus();
        OrderStatus target = request.getStatus();

        if (!current.canTransitionTo(target)) {
            throw new ApplicationException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid transition: " + current + " to " + target);
        }

        order.setStatus(target);

        UUID actorId = currentUserProvider.getUserId();

        TrackingLog log = new TrackingLog();
        log.setOrder(order);
        log.setUserId(actorId);
        log.setStatus(target);
        log.setTitle(request.getTitle());
        log.setLocation(request.getLocation());
        log.setLatitude(request.getLatitude());
        log.setLongitude(request.getLongitude());
        log.setNote(request.getNote());
        trackingLogRepository.save(log);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<TrackingLog> trackingLogs = trackingLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        return toResponse(order, items, trackingLogs);
    }
}
