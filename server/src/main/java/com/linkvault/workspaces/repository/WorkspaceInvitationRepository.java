package com.linkvault.workspaces.repository;

import com.linkvault.workspaces.entity.WorkspaceInvitation;
import com.linkvault.workspaces.enums.InvitationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {

    @EntityGraph(attributePaths = {"workspace", "invitedUser", "invitedBy"})
    List<WorkspaceInvitation> findByWorkspace_IdOrderByCreatedAtDesc(UUID workspaceId);

    @EntityGraph(attributePaths = {"workspace", "invitedUser", "invitedBy"})
    Optional<WorkspaceInvitation> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    @EntityGraph(attributePaths = {"workspace", "invitedUser", "invitedBy"})
    Optional<WorkspaceInvitation> findByToken(String token);

    @Query("""
        select invitation
        from WorkspaceInvitation invitation
        join fetch invitation.workspace workspace
        join fetch invitation.invitedBy invitedBy
        left join fetch invitation.invitedUser invitedUser
        where invitation.status = :status
          and invitation.expiresAt > :now
          and workspace.deletedAt is null
          and (
            invitedUser.id = :userId
            or lower(invitation.invitedIdentifier) in :identifiers
          )
        order by invitation.createdAt desc
        """)
    List<WorkspaceInvitation> findPendingForUser(
        @Param("userId") UUID userId,
        @Param("identifiers") List<String> identifiers,
        @Param("status") InvitationStatus status,
        @Param("now") Instant now
    );

    boolean existsByWorkspace_IdAndInvitedIdentifierIgnoreCaseAndStatus(
        UUID workspaceId,
        String invitedIdentifier,
        InvitationStatus status
    );

    boolean existsByToken(String token);
}
