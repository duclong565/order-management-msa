package com.example.order.service;

import com.example.order.dto.*;
import com.example.order.entity.*;
import com.example.order.common.OrderStatus;
import com.example.order.common.PaymentStatus;
import com.example.order.common.UserRole;
import com.example.order.common.ErrorCode;
import com.example.order.exception.ApplicationException;
import com.example.order.pricing.PricingCalculator;
import com.example.order.repository.*;
import com.example.order.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PricingCalculator  pricingCalculator;
    private final TrackingLogRepository trackingLogRepository;
    private final DiscountRepository discountRepository;
    private final CurrentUserProvider currentUserProvider;

    private void decreaseStock(List<CartItem> items) {
        for (CartItem item : items) {
            ProductVariant variant = item.getProductVariant();
            int updated = inventoryRepository.decreaseStock(variant.getId(), item.getQuantity());

            if (updated == 0) {
                throw new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                        "Insufficient stock for variant: " + variant.getName());
            }
        }
    }

    private OrderItem toOrderItem(Order order, CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductVariant(cartItem.getProductVariant());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setUnitPrice(cartItem.getProductVariant().getPrice());
        return orderItem;
    }

    private OrderResponse toResponse(Order order,
                                     List<OrderItem> orderItems,
                                     List<TrackingLog> trackingLogs) {
        OrderResponse response = new OrderResponse();

        response.setOrderId(order.getId());
        response.setOrderCode(order.getOrderCode());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setItems(orderItems.stream().map(this::toItemResponse).toList());

        response.setSubtotalPrice(order.getSubtotalPrice());
        response.setDiscountId(order.getDiscount() != null ? order.getDiscount().getId() : null);
        response.setDiscountValue(order.getDiscountValue());
        response.setShippingFee(order.getShippingFee());
        response.setTotalPrice(order.getTotalPrice());

        response.setRecipientAddress(order.getRecipientAddressSnapshot());
        response.setRecipient(toRecipient(order.getRecipientAddress()));

        PaymentMethod paymentMethod = order.getPaymentMethod();
        response.setPaymentMethodName(paymentMethod != null ? paymentMethod.getName() : null);
        response.setPaymentLast4(order.getPaymentLast4());

        response.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        Carrier carrier = order.getCarrier();
        response.setCarrierName(carrier != null ? carrier.getName() : null);
        response.setCarrierDescription(carrier != null ? carrier.getDescription() : null);

        response.setTimeline(trackingLogs.stream().map(this::toTrackingEvent).toList());
        response.setCurrentLocation(latestLocation(trackingLogs));

        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    private OrderItemResponse toItemResponse(OrderItem orderItem) {
        ProductVariant productVariant = orderItem.getProductVariant();
        Product product = productVariant.getProduct();
        BigDecimal lineTotal = orderItem.getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        return new OrderItemResponse(
                orderItem.getId(),
                productVariant.getId(),
                product.getName(),
                productVariant.getName(),
                productVariant.getSku(),
                product.getImageUrl(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                lineTotal
        );
    }

    private RecipientAddressResponse toRecipient(Address address) {
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

    private OrderTrackingEventResponse toTrackingEvent(TrackingLog log) {
        User actor = log.getUser();

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

    private String formatAddress(Address address) {
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

        List<CartItem> items = cartItemRepository.findByCartIdWithVariant(cart.getId());
        if (items.isEmpty()) {
            throw new ApplicationException(ErrorCode.CART_EMPTY);
        }

        Address address = addressRepository.findByIdAndUserId(request.getRecipientAddressId(), userId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        decreaseStock(items);

        BigDecimal subtotal = items.stream()
                .map(item -> item.getProductVariant().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Discount discount = null;
        if (request.getDiscountId() != null) {
            discount = discountRepository.findById(request.getDiscountId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.DISCOUNT_NOT_FOUND));
            pricingCalculator.validateDiscountActive(discount);
        }

        BigDecimal discountAmount = pricingCalculator.calculateDiscountAmount(discount, subtotal);
        BigDecimal shippingFee = pricingCalculator.getShippingFee();
        BigDecimal totalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        Order order = new Order();
        order.setOrderCode("ORD-" + orderRepository.nextOrderCodeSequence());
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setDiscount(discount);
        order.setDiscountValue(discountAmount);
        order.setSubtotalPrice(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotalPrice(totalPrice);
        order.setPaymentMethod(paymentMethod);
        order.setRecipientAddress(address);
        order.setRecipientAddressSnapshot(formatAddress(address));
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = items.stream()
                .map(item -> toOrderItem(savedOrder, item))
                .toList();
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteAll(items);
        cart.setDiscount(null);
        cartRepository.save(cart);

        TrackingLog placedLog = new TrackingLog();
        placedLog.setOrder(savedOrder);
        placedLog.setUser(cart.getUser());
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
        User currentUser = currentUserProvider.getPrincipal().user();

        Order order = (currentUser.getRole() == UserRole.ADMIN
                ? orderRepository.findDetailById(orderId)
                : orderRepository.findDetailByIdAndUserId(orderId, currentUser.getId()))
                .orElseThrow(() -> new ApplicationException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(orderId);
        List<TrackingLog> trackingLogs = trackingLogRepository.findByOrderIdWithUser(orderId);

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

        User actor = currentUserProvider.getPrincipal().user();

        TrackingLog log = new TrackingLog();
        log.setOrder(order);
        log.setUser(actor);
        log.setStatus(target);
        log.setTitle(request.getTitle());
        log.setLocation(request.getLocation());
        log.setLatitude(request.getLatitude());
        log.setLongitude(request.getLongitude());
        log.setNote(request.getNote());
        trackingLogRepository.save(log);

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(orderId);
        List<TrackingLog> trackingLogs = trackingLogRepository.findByOrderIdWithUser(orderId);
        return toResponse(order, items, trackingLogs);
    }
}
