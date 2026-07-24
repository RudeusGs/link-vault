package com.linkvault.sharing.repository;

import com.linkvault.sharing.entity.ShareLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    Optional<ShareLink> findByTokenHashAndDeletedAtIsNull(String tokenHash);
    List<ShareLink> findByTargetTypeAndTargetIdAndDeletedAtIsNull(String targetType, UUID targetId);
}
