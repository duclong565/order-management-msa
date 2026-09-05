package com.example.order.client;

import java.util.List;
import java.util.UUID;

public interface UserClient {

    UserResponse getUserById(UUID userId);

    List<UserResponse> getUsersByIds(List<UUID> userIds);

    AddressResponse getAddressById(UUID addressId);
}
