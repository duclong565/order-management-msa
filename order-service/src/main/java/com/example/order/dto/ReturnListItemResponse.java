package com.example.order.dto;

import com.example.order.common.ReturnOriginType;
import com.example.order.common.ReturnReason;
import com.example.order.common.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnListItemResponse {
    private UUID returnId;

    private String returnCode;

    private String customerName;
    private UUID orderId;
    private String orderCode;

    private ReturnReason reason;
    private String reasonNote;

    private ReturnOriginType originType;

    private ReturnStatus status;

    private Instant createdAt;
}
