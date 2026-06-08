package com.linkvault.workspaces.repository;

import com.linkvault.users.entity.User;
import com.linkvault.workspaces.entity.Workspace;
import com.linkvault.workspaces.entity.WorkspaceMember;
import com.linkvault.workspaces.enums.WorkspaceRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @EntityGraph(attributePaths = {"workspace", "workspace.owner"})
    List<WorkspaceMember> findByUser_Id(UUID userId);

    @Query("""
        select wm from WorkspaceMember wm
        join fetch wm.workspace w
        join fetch w.owner
        where wm.user.id = :userId
          and wm.deletedAt is null
          and w.deletedAt is null
        """)
    List<WorkspaceMember> findActiveByUserId(UUID userId);

    @EntityGraph(attributePaths = {"workspace", "workspace.owner"})
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(UUID workspaceId, UUID userId);

    @Query("""
        select wm from WorkspaceMember wm
        join fetch wm.workspace w
        join fetch w.owner
        join fetch wm.user
        where w.id = :workspaceId
          and wm.user.id = :userId
          and wm.deletedAt is null
          and w.deletedAt is null
        """)
    Optional<WorkspaceMember> findActiveByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query("""
        select wm from WorkspaceMember wm
        join fetch wm.workspace w
        join fetch w.owner
        join fetch wm.user
        where wm.id = :memberId
          and w.id = :workspaceId
          and wm.deletedAt is null
          and w.deletedAt is null
        """)
    Optional<WorkspaceMember> findActiveByIdAndWorkspaceId(UUID memberId, UUID workspaceId);

    @EntityGraph(attributePaths = {"workspace", "workspace.owner"})
    Optional<WorkspaceMember> findFirstByUser_IdAndRoleOrderByWorkspace_CreatedAtAsc(UUID userId, WorkspaceRole role);

    @EntityGraph(attributePaths = {"workspace", "workspace.owner"})
    Optional<WorkspaceMember> findFirstByUser_IdAndRoleAndDeletedAtIsNullAndWorkspace_DeletedAtIsNullOrderByWorkspace_CreatedAtAsc(
        UUID userId,
        WorkspaceRole role
    );

    @Query("""
        select wm from WorkspaceMember wm
        join fetch wm.user
        join fetch wm.workspace w
        where w.id = :workspaceId
          and wm.deletedAt is null
          and w.deletedAt is null
        order by wm.createdAt asc
        """)
    List<WorkspaceMember> findActiveByWorkspaceId(UUID workspaceId);

    long countByWorkspace_IdAndDeletedAtIsNull(UUID workspaceId);

    long countByUser_IdAndDeletedAtIsNullAndWorkspace_DeletedAtIsNull(UUID userId);

    long countByWorkspace_IdAndRoleAndDeletedAtIsNull(UUID workspaceId, WorkspaceRole role);

    boolean existsByWorkspace_IdAndUser_IdAndDeletedAtIsNull(UUID workspaceId, UUID userId);
}