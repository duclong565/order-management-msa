package com.example.order.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private UUID userId;
    private String recipientName;
    private String recipientPhone;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String zipCode;
}
