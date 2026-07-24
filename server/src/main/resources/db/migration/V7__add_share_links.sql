create table if not exists share_links (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    token_hash varchar(255) not null,
    target_type varchar(50) not null,
    target_id uuid not null,
    access_level varchar(20) not null,
    password_hash varchar(255),
    expires_at timestamptz,
    is_revoked boolean not null default false,
    created_by_user_id uuid references users(id) on delete set null,
    access_count integer not null default 0,
    last_accessed_at timestamptz,
    constraint uk_share_links_token_hash unique (token_hash)
);

create index if not exists idx_share_links_target on share_links(target_type, target_id);
create index if not exists idx_share_links_expires on share_links(expires_at);
