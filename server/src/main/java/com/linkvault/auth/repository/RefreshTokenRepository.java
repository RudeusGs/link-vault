package com.linkvault.auth.repository;

import com.linkvault.auth.entity.RefreshToken;
import com.linkvault.users.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<RefreshToken> findByIdAndUser_Id(UUID id, UUID userId);
}