package com.shut.user.service;

import com.shut.user.dto.UpdateUserRequest;
import com.shut.user.dto.UserOnboardingRequest;
import com.shut.user.entity.User;
import com.shut.user.exception.UserNotFoundException;
import com.shut.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Creates a customer account on first contact, or refreshes Telegram public fields. */
    @Transactional
    public User onboard(UserOnboardingRequest request) {
        return userRepository.findByTelegramId(request.telegramId())
                .map(user -> {
                    user.setTelegramUsername(blankToNull(request.telegramUsername()));
                    user.setFirstName(blankToNull(request.firstName()));
                    user.setLastName(blankToNull(request.lastName()));
                    return user;
                })
                .orElseGet(() -> {
                    User user = new User(request.telegramId(), blankToNull(request.firstName()));
                    user.setTelegramUsername(blankToNull(request.telegramUsername()));
                    user.setLastName(blankToNull(request.lastName()));
                    return userRepository.save(user);
                });
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        if (request.firstName() != null) user.setFirstName(blankToNull(request.firstName()));
        if (request.lastName() != null) user.setLastName(blankToNull(request.lastName()));
        if (request.telegramUsername() != null) user.setTelegramUsername(blankToNull(request.telegramUsername()));
        if (request.phoneNumber() != null) user.setPhoneNumber(blankToNull(request.phoneNumber()));
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
