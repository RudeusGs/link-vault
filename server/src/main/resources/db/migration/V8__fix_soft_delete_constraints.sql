-- Drop the ordinary unique constraints that conflict with soft deletion
ALTER TABLE vaults DROP CONSTRAINT IF EXISTS uk_vaults_user_name;
ALTER TABLE tags DROP CONSTRAINT IF EXISTS uk_tags_user_name;

-- Workspace members: drop the full unique constraint and create a partial index
ALTER TABLE workspace_members DROP CONSTRAINT IF EXISTS uk_workspace_member;
CREATE UNIQUE INDEX IF NOT EXISTS uk_workspace_member_active 
ON workspace_members (workspace_id, user_id) 
WHERE deleted_at IS NULL;
