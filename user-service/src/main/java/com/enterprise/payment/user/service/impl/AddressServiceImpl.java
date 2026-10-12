package com.enterprise.payment.user.service.impl;

import com.enterprise.payment.common.exception.ResourceNotFoundException;
import com.enterprise.payment.user.dto.AddressResponse;
import com.enterprise.payment.user.dto.CreateAddressRequest;
import com.enterprise.payment.user.entity.User;
import com.enterprise.payment.user.entity.UserAddress;
import com.enterprise.payment.user.repository.UserAddressRepository;
import com.enterprise.payment.user.repository.UserRepository;
import com.enterprise.payment.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, CreateAddressRequest request, String correlationId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // If newly added address is primary, reset any previous primary address for this user
        if (request.isPrimary()) {
            userAddressRepository.findByUserIdAndPrimaryTrue(userId).ifPresent(existingPrimary -> {
                existingPrimary.setPrimary(false);
                userAddressRepository.save(existingPrimary);
            });
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .addressType(request.getAddressType())
                .street(request.getStreet().trim())
                .city(request.getCity().trim())
                .state(request.getState() != null ? request.getState().trim() : null)
                .country(request.getCountry().trim().toUpperCase())
                .postalCode(request.getPostalCode().trim())
                .primary(request.isPrimary())
                .build();

        address = userAddressRepository.save(address);
        log.info("Added address {} for user {} [corr: {}]", address.getId(), userId, correlationId);

        return mapToResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesByUserId(UUID userId) {
        userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return userAddressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AddressResponse mapToResponse(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .addressType(address.getAddressType())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .primary(address.isPrimary())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
