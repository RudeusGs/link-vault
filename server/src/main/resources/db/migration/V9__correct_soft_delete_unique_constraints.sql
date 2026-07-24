-- Drop the correct constraints that V8 missed because it used the wrong names
ALTER TABLE vaults DROP CONSTRAINT IF EXISTS uk_vaults_workspace_name;
ALTER TABLE tags DROP CONSTRAINT IF EXISTS uk_tags_workspace_name;
ALTER TABLE workspace_members DROP CONSTRAINT IF EXISTS uk_workspace_members_workspace_user;

-- Create partial unique indexes to enforce uniqueness only for active records
CREATE UNIQUE INDEX IF NOT EXISTS uk_vaults_workspace_name_active 
    ON vaults(workspace_id, name) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tags_workspace_name_active 
    ON tags(workspace_id, name) WHERE deleted_at IS NULL;

-- Ensure workspace_members is correct
CREATE UNIQUE INDEX IF NOT EXISTS uk_workspace_member_active 
    ON workspace_members(workspace_id, user_id) WHERE deleted_at IS NULL;
