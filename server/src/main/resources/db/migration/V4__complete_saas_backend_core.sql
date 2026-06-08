alter table workspaces
    add column if not exists plan varchar(30) not null default 'FREE';

alter table workspaces
    drop constraint if exists ck_workspaces_plan;

alter table workspaces
    add constraint ck_workspaces_plan check (plan in ('FREE', 'PRO', 'TEAM'));

create table if not exists workspace_invitations (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    invited_identifier varchar(255) not null,
    invited_user_id uuid references users(id) on delete set null,
    invited_by_user_id uuid not null references users(id) on delete cascade,
    role varchar(30) not null,
    token varchar(120) not null unique,
    status varchar(30) not null,
    expires_at timestamptz not null,
    accepted_at timestamptz,
    constraint ck_workspace_invitations_role check (role in ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER')),
    constraint ck_workspace_invitations_status check (status in ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED'))
);

create table if not exists audit_logs (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    workspace_id uuid not null references workspaces(id) on delete cascade,
    actor_user_id uuid references users(id) on delete set null,
    action varchar(120) not null,
    target_type varchar(80) not null,
    target_id uuid,
    metadata text
);

create table if not exists refresh_tokens (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    user_id uuid not null references users(id) on delete cascade,
    token_hash varchar(120) not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    last_used_at timestamptz,
    user_agent varchar(500),
    ip_address varchar(80)
);

create index if not exists idx_workspace_invitations_workspace_status
    on workspace_invitations(workspace_id, status);
create index if not exists idx_workspace_invitations_identifier_status
    on workspace_invitations(workspace_id, lower(invited_identifier), status);
create index if not exists idx_workspace_invitations_token
    on workspace_invitations(token);
create index if not exists idx_workspace_invitations_invited_user
    on workspace_invitations(invited_user_id);

create unique index if not exists uk_workspace_invitations_pending_identifier
    on workspace_invitations(workspace_id, lower(invited_identifier))
    where status = 'PENDING' and deleted_at is null;

create index if not exists idx_audit_logs_workspace_created_at
    on audit_logs(workspace_id, created_at);
create index if not exists idx_audit_logs_actor
    on audit_logs(actor_user_id);

create index if not exists idx_refresh_tokens_user
    on refresh_tokens(user_id);
create index if not exists idx_refresh_tokens_hash
    on refresh_tokens(token_hash);
