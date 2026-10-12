package com.enterprise.payment.user.service;

import com.enterprise.payment.user.dto.AddressResponse;
import com.enterprise.payment.user.dto.CreateAddressRequest;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressResponse addAddress(UUID userId, CreateAddressRequest request, String correlationId);

    List<AddressResponse> getAddressesByUserId(UUID userId);
}
