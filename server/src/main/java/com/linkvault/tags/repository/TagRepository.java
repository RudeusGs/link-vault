package com.linkvault.tags.repository;

import com.linkvault.tags.entity.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByWorkspace_IdOrderByNameAsc(UUID workspaceId);

    java.util.Optional<Tag> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    boolean existsByWorkspace_IdAndNameIgnoreCase(UUID workspaceId, String name);
}