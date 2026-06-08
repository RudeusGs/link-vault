create table if not exists workspaces (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    owner_user_id uuid not null references users(id) on delete cascade,
    name varchar(150) not null,
    slug varchar(120) not null,
    constraint uk_workspaces_slug unique (slug)
);

create table if not exists workspace_members (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role varchar(30) not null,
    constraint ck_workspace_members_role check (role in ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER')),
    constraint uk_workspace_members_workspace_user unique (workspace_id, user_id)
);

create index if not exists idx_workspaces_owner on workspaces(owner_user_id);
create index if not exists idx_workspaces_created_at on workspaces(created_at);

create index if not exists idx_workspace_members_workspace on workspace_members(workspace_id);
create index if not exists idx_workspace_members_user on workspace_members(user_id);
create index if not exists idx_workspace_members_role on workspace_members(role);
