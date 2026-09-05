package com.example.order.dto;

import com.example.order.common.ReturnReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {
    @NotNull
    private UUID orderId;

    @NotNull
    private ReturnReason reason;

    @Size(max = 500)
    private String reasonNote;

    @NotEmpty
    @Valid
    private List<CreateReturnItemRequest> items;
}
