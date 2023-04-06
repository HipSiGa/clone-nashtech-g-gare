package com.yas.customer.service;

import com.yas.customer.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.utils.Constants;
import com.yas.customer.viewmodel.AddressGetVm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository userAddressRepository;

    public List<AddressGetVm> getUserAddressList() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<UserAddress> userAddressList = userAddressRepository.findAllByUserId(userId);
        return userAddressList.stream()
                .map(userAddress -> {
                    AddressGetVm addressGetListResponseVm = new AddressGetVm(
                            userAddress.getId(), userAddress.getIsActive());
                    return addressGetListResponseVm;
                })
                .toList();
    }

    public void createAddress(Long addressId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserAddress userAddress = UserAddress.builder()
                .userId(userId)
                .addressId(addressId)
                .isActive(false)
                .build();

        userAddressRepository.save(userAddress);
    }

    public void deleteAddress(Long id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        UserAddress userAddress = userAddressRepository.findOneByUserIdAndAddressId(userId, id);
        if (userAddress == null) {
            throw new NotFoundException(Constants.ERROR_CODE.USER_ADDRESS_NOT_FOUND);
        }
        userAddressRepository.delete(userAddress);
    }

    public void chooseDefaultAddress(Long id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<UserAddress> userAddressList = userAddressRepository.findAllByUserId(userId);
        List<UserAddress> newUserAddressList = new ArrayList<>();
        for (UserAddress userAddress : userAddressList) {
            if (userAddress.getAddressId() == id) {
                userAddress.setIsActive(true);
            } else {
                userAddress.setIsActive(false);
            }
            newUserAddressList.add(userAddress);
        }
        userAddressRepository.saveAll(newUserAddressList);
    }
}
