package com.example.order.controller;

import com.example.order.common.BaseResponse;
import com.example.order.common.ReturnStatus;
import com.example.order.dto.CreateReturnRequest;
import com.example.order.dto.ReceiveReturnRequest;
import com.example.order.dto.ReturnDetailResponse;
import com.example.order.dto.ReturnListItemResponse;
import com.example.order.dto.ReturnSummaryResponse;
import com.example.order.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class
ReturnController {
    private final ReturnService returnService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<ReturnListItemResponse>>> getReturns(
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReturnListItemResponse> page = returnService.getReturns(status, search, pageable);

        Map<String, Object> metadata = Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );

        return ResponseEntity.ok(BaseResponse.success(page.getContent(), metadata));
    }

    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<ReturnSummaryResponse>> getSummary() {
        return ResponseEntity.ok(BaseResponse.success(returnService.getSummary()));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<ReturnDetailResponse>> createReturn(
            @Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(returnService.createReturn(request), "Return request created"));
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<BaseResponse<ReturnDetailResponse>> getReturn(@PathVariable UUID returnId) {
        return ResponseEntity.ok(BaseResponse.success(returnService.getReturn(returnId)));
    }

    //todo: them user guard
    @PostMapping("/{returnId}/receive")
    public ResponseEntity<BaseResponse<ReturnDetailResponse>> markReceived(
            @PathVariable UUID returnId,
            @Valid @RequestBody(required = false) ReceiveReturnRequest request
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                returnService.markReceived(returnId, request), "Return marked as received"));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(required = false) String search
    ) {
        StreamingResponseBody body = outputStream -> returnService.writeCsv(status, search, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"returns.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}
