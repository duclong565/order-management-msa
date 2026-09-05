package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipientAddressResponse {
    private String recipientName;
    private String recipientPhone;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String zipCode;
}
