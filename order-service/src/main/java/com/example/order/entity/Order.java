package com.example.order.entity;

import com.example.order.common.OrderStatus;
import com.example.order.common.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order extends BaseEntity {
    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING) //annotation cho JPA lưu kiểu dữ liệu enum dạng string không phải int, default là ordinal (0,1,2,...)
    private OrderStatus status = OrderStatus.PENDING; //lưu "pending"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @Column(name = "discount_value", precision = 12, scale = 2) //số có tối đa 12 chữ số, 2 chữ số sau dấu phẩy
    private BigDecimal discountValue;

    @Column(name = "subtotal_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalPrice;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "payment_last4", length = 4)
    private String paymentLast4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private Carrier carrier;

    @Column(name = "tracking_number", unique = true, length = 50)
    private String trackingNumber;

    @Column(name = "estimated_delivery_date")
    private Instant estimatedDeliveryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_address_id")
    private Address senderAddress;

    @Column(name = "sender_address", length = 500)
    private String senderAddressSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_address_id")
    private Address recipientAddress;

    @Column(name = "recipient_address", length = 500)
    private String recipientAddressSnapshot;
}
