package com.example.order.client;

import com.example.order.common.ErrorCode;
import com.example.order.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClientImpl implements UserClient {

    private static final String AUTH_SERVICE_URL = "http://auth-service";

    private final IdentityForwardingWebClient identityForwardingWebClient;

    @Override
    public UserResponse getUserById(UUID userId) {
        ApiEnvelope<UserResponse> response = identityForwardingWebClient.client()
                .get()
                .uri(AUTH_SERVICE_URL + "/users/{id}", userId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        clientResponse -> Mono.error(new ApplicationException(ErrorCode.USER_NOT_FOUND)))
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<UserResponse>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }
        return response.getData();
    }

    @Override
    public List<UserResponse> getUsersByIds(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        ApiEnvelope<List<UserResponse>> response = identityForwardingWebClient.client()
                .post()
                .uri(AUTH_SERVICE_URL + "/users/get-by-ids")
                .bodyValue(userIds)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<List<UserResponse>>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.USER_NOT_FOUND);
        }
        return response.getData();
    }

    @Override
    public AddressResponse getAddressById(UUID addressId) {
        ApiEnvelope<AddressResponse> response = identityForwardingWebClient.client()
                .get()
                .uri(AUTH_SERVICE_URL + "/addresses/{id}", addressId)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        clientResponse -> Mono.error(new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND)))
                .bodyToMono(new ParameterizedTypeReference<ApiEnvelope<AddressResponse>>() {})
                .block();

        if (response == null || response.getData() == null) {
            throw new ApplicationException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return response.getData();
    }
}
