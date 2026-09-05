package com.example.order.service;

import com.example.order.client.UserClient;
import com.example.order.client.UserResponse;
import com.example.order.common.OrderStatus;
import com.example.order.dto.AdminOrderListItemResponse;
import com.example.order.dto.BulkConfirmRequest;
import com.example.order.dto.BulkConfirmResponse;
import com.example.order.dto.CarrierShareResponse;
import com.example.order.dto.OrderOperationsSummaryResponse;
import com.example.order.entity.Carrier;
import com.example.order.entity.Order;
import com.example.order.entity.TrackingLog;
import com.example.order.repository.CarrierShareProjection;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderSummaryProjection;
import com.example.order.repository.TrackingLogRepository;
import com.example.order.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final OrderRepository orderRepository;
    private final TrackingLogRepository trackingLogRepository;
    private final UserClient userClient;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public Page<AdminOrderListItemResponse> getOrders(OrderStatus status, String search, Pageable pageable) {
        Page<Order> page = orderRepository.searchForAdmin(status, normalizeSearch(search), pageable);

        // gom hết userId trong page, gọi auth-service đúng 1 lần thay vì mỗi row 1 lần.
        List<UUID> userIds = page.getContent().stream().map(Order::getUserId).distinct().toList();
        Map<UUID, UserResponse> customers = userClient.getUsersByIds(userIds).stream()
                .collect(Collectors.toMap(UserResponse::getId, Function.identity()));

        return page.map(order -> toListItem(order, customers));
    }

    @Transactional(readOnly = true)
    public OrderOperationsSummaryResponse getSummary() {
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant thisMonthStart = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant lastMonthStart = LocalDate.now(zone).minusMonths(1)
                .withDayOfMonth(1).atStartOfDay(zone).toInstant();

        OrderSummaryProjection stats = orderRepository.summarize(now, thisMonthStart, lastMonthStart);

        long totalOrders = stats.getTotalOrders();
        long withCarrier = stats.getWithCarrierCount();

        OrderOperationsSummaryResponse response = new OrderOperationsSummaryResponse();
        response.setTotalRevenueMtd(stats.getRevenueThisMonth());
        response.setRevenueChangePercent(percentChange(stats.getRevenueLastMonth(), stats.getRevenueThisMonth()));
        response.setTotalOrders(totalOrders);
        response.setPendingCount(stats.getPendingCount());
        response.setShippingCount(stats.getShippingCount());
        response.setFailedCount(stats.getFailedCount());
        response.setFailedRatePercent(ratio(stats.getFailedCount(), totalOrders));

        response.setIngestionVelocityPercent(
                orderRepository.ingestionVelocityPercent(thisMonthStart, now));
        response.setCarrierConfirmationRatePercent(ratio(withCarrier, totalOrders));

        List<CarrierShareResponse> distribution = buildCarrierDistribution(withCarrier);
        response.setCarrierDistribution(distribution);
        response.setInNetworkPercent(inNetworkPercent(distribution, withCarrier));

        return response;
    }

    private List<CarrierShareResponse> buildCarrierDistribution(long totalWithCarrier) {
        List<CarrierShareResponse> result = new ArrayList<>();
        for (CarrierShareProjection row : orderRepository.carrierDistribution()) {
            result.add(new CarrierShareResponse(
                    row.getCarrierName(),
                    Boolean.TRUE.equals(row.getInNetwork()),
                    row.getOrderCount(),
                    ratio(row.getOrderCount(), totalWithCarrier)
            ));
        }
        return result;
    }

    private BigDecimal inNetworkPercent(List<CarrierShareResponse> distribution, long totalWithCarrier) {
        long inNetwork = distribution.stream()
                .filter(CarrierShareResponse::isInNetwork)
                .mapToLong(CarrierShareResponse::getOrderCount)
                .sum();
        return ratio(inNetwork, totalWithCarrier);
    }

    @Transactional
    public BulkConfirmResponse bulkConfirm(BulkConfirmRequest request) {
        List<UUID> requestedIds = request.getOrderIds();
        List<Order> orders = orderRepository.findAllByIdIn(requestedIds);

        Set<UUID> foundIds = new HashSet<>();
        List<UUID> confirmed = new ArrayList<>();
        List<BulkConfirmResponse.SkippedOrder> skipped = new ArrayList<>();

        UUID actorId = currentUserProvider.getUserId();

        for (Order order : orders) {
            foundIds.add(order.getId());

            OrderStatus current = order.getStatus();
            if (!current.canTransitionTo(OrderStatus.CONFIRMED)) {
                skipped.add(new BulkConfirmResponse.SkippedOrder(order.getId(),
                        "Invalid transition: " + current + " to " + OrderStatus.CONFIRMED));
                continue;
            }

            order.setStatus(OrderStatus.CONFIRMED);

            TrackingLog log = new TrackingLog();
            log.setOrder(order);
            log.setUserId(actorId);
            log.setStatus(OrderStatus.CONFIRMED);
            log.setTitle("Order Confirmed");
            log.setNote("Confirmed in bulk from operations console.");
            trackingLogRepository.save(log);

            confirmed.add(order.getId());
        }

        for (UUID requestedId : requestedIds) {
            if (!foundIds.contains(requestedId)) {
                skipped.add(new BulkConfirmResponse.SkippedOrder(requestedId, "Order not found"));
            }
        }

        return new BulkConfirmResponse(requestedIds.size(), confirmed.size(), confirmed, skipped);
    }

    private AdminOrderListItemResponse toListItem(Order order, Map<UUID, UserResponse> customers) {
        UserResponse customer = customers.get(order.getUserId());
        Carrier carrier = order.getCarrier();

        return new AdminOrderListItemResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                displayName(customer),
                order.getCreatedAt(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getPaymentStatus(),
                carrier != null ? carrier.getName() : null
        );
    }

    private String displayName(UserResponse user) {
        if (user == null) {
            return null;
        }
        return user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : user.getUsername();
    }

    private String normalizeSearch(String search) {
        return (search == null || search.isBlank()) ? null : search.trim();
    }

    private BigDecimal ratio(long part, long whole) {
        if (whole == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }
}
