package com.finapse.service;

import com.finapse.entity.User;
import com.finapse.exception.ResourceNotFoundException;
import com.finapse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    // The single local user seeded in seed.sql
    private static final UUID DEFAULT_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;

    public User getDefaultUser() {
        return userRepository.findById(DEFAULT_USER_ID)
                .orElseGet(() -> {
                    User u = new User();
                    u.setId(DEFAULT_USER_ID);
                    u.setName("Local User");
                    return userRepository.save(u);
                });
    }
}
