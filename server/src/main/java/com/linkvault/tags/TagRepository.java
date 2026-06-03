package com.linkvault.tags;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByUser_IdOrderByNameAsc(UUID userId);

    boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);
}
