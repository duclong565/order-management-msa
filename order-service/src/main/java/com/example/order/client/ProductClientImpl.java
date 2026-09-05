package com.example.order.client;

import com.example.order.common.ErrorCode;
import com.example.order.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {

    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    private final IdentityForwardingWebClient identityForwardingWebClient;

    @Override
    public List<ProductVariantResponse> getVariantsByIds(List<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of();
        }

        ApiEnvelope<List<ProductVariantResponse>> response = client()
                .post()
                .uri(PRODUCT_SERVICE_URL + "/product-variants/get-by-ids")
                .bodyValue(variantIds)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<List<ProductVariantResponse>>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
        return response.getData();
    }

    @Override
    public long getStock(UUID variantId) {
        ApiEnvelope<Long> response = client()
                .get()
                .uri(PRODUCT_SERVICE_URL + "/product-variants/{id}/stock", variantId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<Long>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
        return response.getData();
    }

    @Override
    public void decreaseStock(UUID variantId, int quantity) {
        client()
                .post()
                .uri(PRODUCT_SERVICE_URL + "/product-variants/{id}/decrease-stock", variantId)
                .bodyValue(new DecreaseStockRequestBody(quantity))
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        clientResponse -> Mono.error(new ApplicationException(ErrorCode.INSUFFICIENT_STOCK,
                                "Insufficient stock for variant: " + variantId)))
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public DiscountResponse getDiscountById(UUID discountId) {
        ApiEnvelope<DiscountResponse> response = client()
                .get()
                .uri(PRODUCT_SERVICE_URL + "/discounts/{id}", discountId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        clientResponse -> Mono.error(new ApplicationException(ErrorCode.DISCOUNT_NOT_FOUND)))
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<DiscountResponse>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.DISCOUNT_NOT_FOUND);
        }
        return response.getData();
    }

    @Override
    public WarehouseResponse getWarehouseById(UUID warehouseId) {
        ApiEnvelope<WarehouseResponse> response = client()
                .get()
                .uri(PRODUCT_SERVICE_URL + "/warehouses/{id}", warehouseId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        clientResponse -> Mono.error(new ApplicationException(ErrorCode.WAREHOUSE_NOT_FOUND)))
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<WarehouseResponse>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }
        return response.getData();
    }

    private WebClient client() {
        return identityForwardingWebClient.client();
    }

    private record DecreaseStockRequestBody(int quantity) {
    }
}
