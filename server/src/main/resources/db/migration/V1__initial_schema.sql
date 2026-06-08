create table if not exists users (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    username varchar(100) not null unique,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(150),
    avatar_url varchar(500),
    is_verified boolean,
    is_enabled boolean,
    auth_provider varchar(30),
    last_login_at timestamptz
);

create table if not exists vaults (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    user_id uuid not null references users(id) on delete cascade,
    name varchar(150) not null,
    description varchar(1000),
    icon varchar(80),
    color varchar(40),
    constraint uk_vaults_user_name unique (user_id, name)
);

create table if not exists folders (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    vault_id uuid not null references vaults(id) on delete cascade,
    parent_id uuid references folders(id) on delete cascade,
    name varchar(150) not null,
    description varchar(1000),
    icon varchar(80),
    sort_order integer not null default 0
);

create table if not exists resources (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    vault_id uuid not null references vaults(id) on delete cascade,
    folder_id uuid references folders(id) on delete cascade,
    title varchar(255) not null,
    description varchar(1000),
    resource_type varchar(20) not null,
    url varchar(2000),
    file_url varchar(2000),
    file_name varchar(255),
    file_size bigint,
    mime_type varchar(150),
    storage_provider varchar(80),
    storage_key varchar(500),
    content text,
    code_language varchar(80),
    source_name varchar(255),
    thumbnail_url varchar(2000),
    preview_title varchar(500),
    preview_description varchar(1000),
    favicon_url varchar(2000),
    site_name varchar(255),
    canonical_url varchar(2000),
    preview_fetched_at timestamptz,
    preview_status varchar(40),
    preview_error varchar(1000),
    is_favorite boolean not null default false,
    is_archived boolean not null default false
);

create table if not exists tags (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    user_id uuid not null references users(id) on delete cascade,
    name varchar(100) not null,
    color varchar(40),
    constraint uk_tags_user_name unique (user_id, name)
);

create table if not exists resource_tags (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    resource_id uuid not null references resources(id) on delete cascade,
    tag_id uuid not null references tags(id) on delete cascade,
    constraint uk_resource_tags_resource_tag unique (resource_id, tag_id)
);

create table if not exists resource_views (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    resource_id uuid not null references resources(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    viewed_at timestamptz not null
);

create index if not exists idx_users_username on users(username);
create index if not exists idx_users_email on users(email);

create index if not exists idx_vaults_user on vaults(user_id);
create index if not exists idx_vaults_created_at on vaults(created_at);
create unique index if not exists uk_vaults_user_name_ci on vaults(user_id, lower(name)) where deleted_at is null;

create index if not exists idx_folders_vault on folders(vault_id);
create index if not exists idx_folders_parent on folders(parent_id);
create index if not exists idx_folders_created_at on folders(created_at);
create unique index if not exists uk_folders_root_name_ci on folders(vault_id, lower(name)) where parent_id is null and deleted_at is null;
create unique index if not exists uk_folders_parent_name_ci on folders(vault_id, parent_id, lower(name)) where parent_id is not null and deleted_at is null;

create index if not exists idx_resources_vault on resources(vault_id);
create index if not exists idx_resources_folder on resources(folder_id);
create index if not exists idx_resources_type on resources(resource_type);
create index if not exists idx_resources_favorite on resources(is_favorite);
create index if not exists idx_resources_created_at on resources(created_at);
create index if not exists idx_resources_archive on resources(is_archived);

create index if not exists idx_tags_user on tags(user_id);
create index if not exists idx_tags_created_at on tags(created_at);
create unique index if not exists uk_tags_user_name_ci on tags(user_id, lower(name)) where deleted_at is null;

create index if not exists idx_resource_tags_resource on resource_tags(resource_id);
create index if not exists idx_resource_tags_tag on resource_tags(tag_id);

create index if not exists idx_resource_views_resource on resource_views(resource_id);
create index if not exists idx_resource_views_user on resource_views(user_id);
create index if not exists idx_resource_views_viewed_at on resource_views(viewed_at);
