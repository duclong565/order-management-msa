package com.example.order.service;

import com.example.order.common.ErrorCode;
import com.example.order.common.OrderStatus;
import com.example.order.common.ReturnOriginType;
import com.example.order.common.ReturnStatus;
import com.example.order.dto.CreateReturnItemRequest;
import com.example.order.dto.CreateReturnRequest;
import com.example.order.dto.ReceiveReturnRequest;
import com.example.order.dto.ReturnDetailResponse;
import com.example.order.dto.ReturnItemResponse;
import com.example.order.dto.ReturnListItemResponse;
import com.example.order.dto.ReturnSummaryResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.ProductVariant;
import com.example.order.entity.ReturnRequest;
import com.example.order.entity.ReturnRequestItem;
import com.example.order.entity.Warehouse;
import com.example.order.exception.ApplicationException;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ReturnRequestItemRepository;
import com.example.order.repository.ReturnRequestRepository;
import com.example.order.repository.ReturnSummaryProjection;
import com.example.order.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReturnService {
    private static final int INSPECTION_SLA_HOURS = 48;

    private static final int CARRIER_DELAY_HOURS = 72;

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public Page<ReturnListItemResponse> getReturns(ReturnStatus status, String search, Pageable pageable) {
        return returnRequestRepository
                .search(status, normalizeSearch(search), pageable)
                .map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public ReturnDetailResponse getReturn(UUID returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findDetailById(returnId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RETURN_NOT_FOUND));

        return toDetail(returnRequest);
    }

    @Transactional
    public ReturnDetailResponse markReceived(UUID returnId, ReceiveReturnRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findDetailById(returnId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RETURN_NOT_FOUND));

        ReturnStatus current = returnRequest.getStatus();
        if (!current.canTransitionTo(ReturnStatus.WAREHOUSE_RECEIVED)) {
            throw new ApplicationException(ErrorCode.INVALID_RETURN_TRANSITION,
                    "Invalid transition: " + current + " to " + ReturnStatus.WAREHOUSE_RECEIVED);
        }

        if (request != null && request.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_REQUEST, "Warehouse not found"));
            returnRequest.setWarehouse(warehouse);
        }

        returnRequest.setStatus(ReturnStatus.WAREHOUSE_RECEIVED);

        returnRequest.setReceivedAt(Instant.now());

        return toDetail(returnRequest);
    }

    @Transactional
    public ReturnDetailResponse createReturn(CreateReturnRequest request) {
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new ApplicationException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().isReturnable()) {
            throw new ApplicationException(ErrorCode.ORDER_NOT_RETURNABLE,
                    "Order in status " + order.getStatus() + " cannot be returned");
        }

        Map<UUID, OrderItem> orderItems = orderItemRepository
                .findByOrderIdWithVariant(order.getId())
                .stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        validateNoDuplicateItems(request.getItems());

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setReturnCode(nextReturnCode());
        returnRequest.setOrder(order);
        returnRequest.setUser(order.getUser());
        returnRequest.setReason(request.getReason());
        returnRequest.setReasonNote(request.getReasonNote());
        returnRequest.setOriginType(resolveOriginType(order));
        returnRequest.setStatus(ReturnStatus.PENDING_ACTION);
        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        List<ReturnRequestItem> items = request.getItems().stream()
                .map(line -> toReturnItem(saved, orderItems, line))
                .toList();
        returnRequestItemRepository.saveAll(items);

        return toDetail(saved);
    }

    private ReturnRequestItem toReturnItem(ReturnRequest returnRequest,
                                           Map<UUID, OrderItem> orderItems,
                                           CreateReturnItemRequest line) {
        OrderItem orderItem = orderItems.get(line.getOrderItemId());
        if (orderItem == null) {
            throw new ApplicationException(ErrorCode.ORDER_ITEM_NOT_FOUND,
                    "Order item " + line.getOrderItemId() + " does not belong to this order");
        }

        int alreadyReturned = returnRequestItemRepository.totalReturnedQuantity(orderItem.getId());
        int remaining = orderItem.getQuantity() - alreadyReturned;
        if (line.getQuantity() > remaining) {
            throw new ApplicationException(ErrorCode.RETURN_QUANTITY_EXCEEDED,
                    "Can return at most " + remaining + " of order item " + orderItem.getId()
                            + " (bought " + orderItem.getQuantity() + ", already returned " + alreadyReturned + ")");
        }

        ReturnRequestItem item = new ReturnRequestItem();
        item.setReturnRequest(returnRequest);
        item.setOrderItem(orderItem);
        item.setQuantity(line.getQuantity());
        return item;
    }

    private void validateNoDuplicateItems(List<CreateReturnItemRequest> items) {
        Set<UUID> seen = new HashSet<>();
        for (CreateReturnItemRequest line : items) {
            if (!seen.add(line.getOrderItemId())) {
                throw new ApplicationException(ErrorCode.INVALID_REQUEST,
                        "Duplicated orderItemId: " + line.getOrderItemId());
            }
        }
    }

    private ReturnOriginType resolveOriginType(Order order) {
        return order.getStatus() == OrderStatus.FAILED
                ? ReturnOriginType.FAILED_DELIVERY
                : ReturnOriginType.CUSTOMER_REQUESTED;
    }

    private String nextReturnCode() {
        return "RET-" + returnRequestRepository.nextReturnCodeSequence();
    }

    // todo: thanh 1 repo call
    @Transactional(readOnly = true)
    public ReturnSummaryResponse getSummary() {
        Instant now = Instant.now();
        ZoneId zone = ZoneId.systemDefault();

        Instant thisMonthStart = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant lastMonthStart = LocalDate.now(zone).minusMonths(1)
                .withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant quarterStart = currentQuarterStart(zone);
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgo = now.minus(14, ChronoUnit.DAYS);
        Instant monthAgo = now.minus(30, ChronoUnit.DAYS);
        Instant inspectionThreshold = now.minus(INSPECTION_SLA_HOURS, ChronoUnit.HOURS);
        Instant carrierThreshold = now.minus(CARRIER_DELAY_HOURS, ChronoUnit.HOURS);

        ReturnSummaryProjection stats = returnRequestRepository.summarize(
                now, thisMonthStart, lastMonthStart, quarterStart,
                weekAgo, twoWeeksAgo, monthAgo, inspectionThreshold, carrierThreshold);

        return new ReturnSummaryResponse(
                stats.getActiveReturns(),
                percentChange(stats.getLastMonthCount(), stats.getThisMonthCount()),
                stats.getAwaitingInspection(),
                round1(stats.getAvgCycleHours()),
                stats.getTotalRefunds(),
                round1(stats.getAvgProcessingThisWeek()),
                percentChange(stats.getAvgProcessingLastWeek().doubleValue(),
                              stats.getAvgProcessingThisWeek().doubleValue()).negate(),
                stats.getUrgentInspectionCount(),
                stats.getCarrierDelayCount()
        );
    }

    @Transactional(readOnly = true)
    public void writeCsv(ReturnStatus status, String search, OutputStream outputStream) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
             Stream<ReturnRequest> rows = returnRequestRepository.streamForExport(status, normalizeSearch(search))) {
            writer.write("Return ID,Customer,Order ID,Reason,Origin Type,Status,Refund Amount,Initiated At\n");

            rows.forEach(r -> writeCsvRow(writer, r));
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeCsvRow(Writer writer, ReturnRequest r) {
        try {
            writer.write(String.join(",",
                    csv(r.getReturnCode()),
                    csv(r.getUser().getUsername()),
                    csv(r.getOrder().getOrderCode()),
                    csv(r.getReason().name()),
                    csv(r.getOriginType().name()),
                    csv(r.getStatus().name()),
                    csv(r.getRefundAmount() == null ? "" : r.getRefundAmount().toPlainString()),
                    csv(r.getCreatedAt().toString())));
            writer.write("\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private ReturnListItemResponse toListItem(ReturnRequest r) {
        return new ReturnListItemResponse(
                r.getId(),
                r.getReturnCode(),
                r.getUser().getUsername(),
                r.getOrder().getId(),
                r.getOrder().getOrderCode(),
                r.getReason(),
                r.getReasonNote(),
                r.getOriginType(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }

    private ReturnDetailResponse toDetail(ReturnRequest r) {
        List<ReturnItemResponse> items = returnRequestItemRepository
                .findByReturnRequestIdWithVariant(r.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new ReturnDetailResponse(
                r.getId(),
                r.getReturnCode(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getUser().getEmail(),
                r.getOrder().getId(),
                r.getOrder().getOrderCode(),
                r.getReason(),
                r.getReasonNote(),
                r.getOriginType(),
                r.getStatus(),
                items,
                r.getRefundAmount(),
                r.getCarrier() == null ? null : r.getCarrier().getName(),
                r.getTrackingNumber(),
                r.getWarehouse() == null ? null : r.getWarehouse().getName(),
                r.getCreatedAt(),
                r.getReceivedAt(),
                r.getRestockedAt(),
                r.getRefundedAt()
        );
    }

    private ReturnItemResponse toItemResponse(ReturnRequestItem rri) {
        OrderItem orderItem = rri.getOrderItem();
        ProductVariant variant = orderItem.getProductVariant();
        BigDecimal lineTotal = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(rri.getQuantity()));

        return new ReturnItemResponse(
                rri.getId(),
                orderItem.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getName(),
                orderItem.getUnitPrice(),
                rri.getQuantity(),
                lineTotal
        );
    }

    private String normalizeSearch(String search) {
        return (search == null || search.isBlank()) ? null : search.trim();
    }

    private Instant currentQuarterStart(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        int firstMonthOfQuarter = ((today.getMonthValue() - 1) / 3) * 3 + 1;
        return today.withMonth(firstMonthOfQuarter).withDayOfMonth(1).atStartOfDay(zone).toInstant();
    }

    private BigDecimal percentChange(double previous, double current) {
        if (previous == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((current - previous) / previous * 100)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal round1(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
