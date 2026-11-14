-- Some widgets, such as the ranked list of members, are not about one member.
ALTER TABLE widget_messages ALTER COLUMN provider_id DROP NOT NULL;
