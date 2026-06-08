create extension if not exists pgcrypto;

alter table vaults add column if not exists workspace_id uuid;
alter table tags add column if not exists workspace_id uuid;

insert into workspaces (id, created_at, updated_at, owner_user_id, name, slug)
select
    gen_random_uuid(),
    now(),
    now(),
    u.id,
    left(coalesce(nullif(trim(u.display_name), ''), u.username, 'Personal'), 140) || ' Workspace',
    left(
        trim(both '-' from regexp_replace(lower(coalesce(nullif(trim(u.username), ''), 'workspace')), '[^a-z0-9]+', '-', 'g')),
        80
    ) || '-' || left(u.id::text, 8)
from users u
where not exists (
    select 1
    from workspace_members wm
    where wm.user_id = u.id
      and wm.role = 'OWNER'
      and wm.deleted_at is null
);

insert into workspace_members (id, created_at, updated_at, workspace_id, user_id, role)
select gen_random_uuid(), now(), now(), w.id, w.owner_user_id, 'OWNER'
from workspaces w
where not exists (
    select 1
    from workspace_members wm
    where wm.workspace_id = w.id
      and wm.user_id = w.owner_user_id
      and wm.deleted_at is null
);

with default_workspaces as (
    select distinct on (wm.user_id)
        wm.user_id,
        wm.workspace_id
    from workspace_members wm
    join workspaces w on w.id = wm.workspace_id
    where wm.role = 'OWNER'
      and wm.deleted_at is null
      and w.deleted_at is null
    order by wm.user_id, w.created_at asc
)
update vaults v
set workspace_id = dw.workspace_id
from default_workspaces dw
where v.user_id = dw.user_id
  and v.workspace_id is null;

with default_workspaces as (
    select distinct on (wm.user_id)
        wm.user_id,
        wm.workspace_id
    from workspace_members wm
    join workspaces w on w.id = wm.workspace_id
    where wm.role = 'OWNER'
      and wm.deleted_at is null
      and w.deleted_at is null
    order by wm.user_id, w.created_at asc
)
update tags t
set workspace_id = dw.workspace_id
from default_workspaces dw
where t.user_id = dw.user_id
  and t.workspace_id is null;

alter table vaults alter column workspace_id set not null;
alter table tags alter column workspace_id set not null;

alter table vaults drop constraint if exists uk_vaults_user_name;
alter table tags drop constraint if exists uk_tags_user_name;

drop index if exists uk_vaults_user_name_ci;
drop index if exists uk_tags_user_name_ci;

alter table vaults
    add constraint fk_vaults_workspace
    foreign key (workspace_id) references workspaces(id) on delete cascade;

alter table tags
    add constraint fk_tags_workspace
    foreign key (workspace_id) references workspaces(id) on delete cascade;

alter table vaults
    add constraint uk_vaults_workspace_name unique (workspace_id, name);

alter table tags
    add constraint uk_tags_workspace_name unique (workspace_id, name);

create index if not exists idx_vaults_workspace on vaults(workspace_id);
create index if not exists idx_tags_workspace on tags(workspace_id);
create index if not exists idx_resources_vault on resources(vault_id);
create index if not exists idx_folders_vault on folders(vault_id);

create unique index if not exists uk_vaults_workspace_name_ci
    on vaults(workspace_id, lower(name))
    where deleted_at is null;

create unique index if not exists uk_tags_workspace_name_ci
    on tags(workspace_id, lower(name))
    where deleted_at is null;
