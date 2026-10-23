-- Make legacy Hibernate-created group chat tables enforce the same relations as fresh installs.
-- NOT VALID protects existing rows from blocking deployment while still enforcing new writes.
DO $$
BEGIN
    IF to_regclass('public.group_memberships') IS NOT NULL
       AND to_regclass('public.community_groups') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.group_memberships'::regclass
             AND confrelid = 'public.community_groups'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE group_memberships
            ADD CONSTRAINT fk_group_memberships_group
            FOREIGN KEY (group_id) REFERENCES community_groups(id) ON DELETE CASCADE NOT VALID;
    END IF;

    IF to_regclass('public.group_memberships') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.group_memberships'::regclass
             AND confrelid = 'public.users'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE group_memberships
            ADD CONSTRAINT fk_group_memberships_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE NOT VALID;
    END IF;

    IF to_regclass('public.group_messages') IS NOT NULL
       AND to_regclass('public.community_groups') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.group_messages'::regclass
             AND confrelid = 'public.community_groups'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE group_messages
            ADD CONSTRAINT fk_group_messages_group
            FOREIGN KEY (group_id) REFERENCES community_groups(id) ON DELETE CASCADE NOT VALID;
    END IF;

    IF to_regclass('public.group_messages') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.group_messages'::regclass
             AND confrelid = 'public.users'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE group_messages
            ADD CONSTRAINT fk_group_messages_sender
            FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE NOT VALID;
    END IF;

    IF to_regclass('public.group_messages') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.group_messages'::regclass
             AND confrelid = 'public.group_messages'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE group_messages
            ADD CONSTRAINT fk_group_messages_parent
            FOREIGN KEY (parent_message_id) REFERENCES group_messages(id) ON DELETE SET NULL NOT VALID;
    END IF;

    IF to_regclass('public.message_reactions') IS NOT NULL
       AND to_regclass('public.group_messages') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.message_reactions'::regclass
             AND confrelid = 'public.group_messages'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE message_reactions
            ADD CONSTRAINT fk_message_reactions_message
            FOREIGN KEY (message_id) REFERENCES group_messages(id) ON DELETE CASCADE NOT VALID;
    END IF;

    IF to_regclass('public.message_reactions') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conrelid = 'public.message_reactions'::regclass
             AND confrelid = 'public.users'::regclass
             AND contype = 'f'
       ) THEN
        ALTER TABLE message_reactions
            ADD CONSTRAINT fk_message_reactions_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE NOT VALID;
    END IF;
END $$;

-- Support incremental polling, recent-message pagination, and grouped reaction lookups.
CREATE INDEX IF NOT EXISTS idx_group_memberships_user_status
    ON group_memberships(user_id, status, group_id);
CREATE INDEX IF NOT EXISTS idx_group_memberships_group_status
    ON group_memberships(group_id, status, user_id);
CREATE INDEX IF NOT EXISTS idx_group_messages_group_id
    ON group_messages(group_id, id);
CREATE INDEX IF NOT EXISTS idx_message_reactions_message_user
    ON message_reactions(message_id, user_id);
