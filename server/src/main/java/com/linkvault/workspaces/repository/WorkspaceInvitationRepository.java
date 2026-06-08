package com.linkvault.workspaces.repository;

import com.linkvault.workspaces.entity.WorkspaceInvitation;
import com.linkvault.workspaces.enums.InvitationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {

    @EntityGraph(attributePaths = {"workspace", "invitedUser", "invitedBy"})
    List<WorkspaceInvitation> findByWorkspace_IdOrderByCreatedAtDesc(UUID workspaceId);

    @EntityGraph(attributePaths = {"workspace", "invitedUser", "invitedBy"})
    Optional<WorkspaceInvitation> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    @EntityGraph(attributePaths = {"workspace", "invitedUser", "invitedBy"})
    Optional<WorkspaceInvitation> findByToken(String token);

    boolean existsByWorkspace_IdAndInvitedIdentifierIgnoreCaseAndStatus(
        UUID workspaceId,
        String invitedIdentifier,
        InvitationStatus status
    );

    boolean existsByToken(String token);
}