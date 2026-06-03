package com.linkvault.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserContextService {

    private static final String DEMO_USERNAME = "demo";

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getDemoUser() {
        return userRepository.findFirstByOrderByCreatedAtAsc()
            .orElseGet(this::createDemoUser);
    }

    private User createDemoUser() {
        User user = new User();
        user.setUsername(DEMO_USERNAME);
        user.setEmail("demo@linkvault.local");
        user.setPasswordHash("demo-password-not-for-auth");
        user.setDisplayName("Demo User");
        return userRepository.save(user);
    }
}
